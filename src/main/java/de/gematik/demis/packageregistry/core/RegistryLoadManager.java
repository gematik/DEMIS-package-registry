package de.gematik.demis.packageregistry.core;

/*-
 * #%L
 * package-registry
 * %%
 * Copyright (C) 2025 - 2026 gematik GmbH
 * %%
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the
 * European Commission – subsequent versions of the EUPL (the "Licence").
 * You may not use this work except in compliance with the Licence.
 *
 * You find a copy of the Licence in the "Licence" file or at
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either expressed or implied.
 * In case of changes by gematik find details in the "Readme" file.
 *
 * See the Licence for the specific language governing permissions and limitations under the Licence.
 *
 * *******
 *
 * For additional notes and disclaimer from gematik and in case of changes by gematik,
 * find details in the "Readme" file.
 * #L%
 */

import de.gematik.demis.packageregistry.domain.FhirPackage;
import de.gematik.demis.packageregistry.domain.PackageKey;
import de.gematik.demis.packageregistry.retriever.RetrieverProperties;
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * This class orchestrates all write operations into the package registry, including initial loading
 * of packages at startup and handling concurrent package retrievals.
 */
@Service
@Slf4j
@RequiredArgsConstructor
class RegistryLoadManager {

  private final InitialPackageProperties initialPackageProperties;
  private final RetrieverProperties retrieverProperties;

  private final PackageStoragePort storagePort;
  private final PackageRetrieverPort retrieverPort;

  private final DependencyResolver dependencyResolver;
  private final FhirPackageFactory fhirPackageFactory;

  private final Map<PackageKey, CompletableFuture<FhirPackage>> loadingPackages =
      new ConcurrentHashMap<>();
  private final Executor packageRetrievalExecutor = Executors.newVirtualThreadPerTaskExecutor();

  @PostConstruct
  void initializeRegistry() {
    if (!initialPackageProperties.getPackages().isEmpty()) {
      log.info("Preloading packages in the registry");

      List<CompletableFuture<FhirPackage>> preloadFutures =
          initialPackageProperties.getPackages().stream()
              .flatMap(p -> p.packageKeys().stream())
              .filter(key -> !storagePort.packageExists(key))
              .map(this::retrievePackageAsync)
              .toList();

      waitForAllOrFailFast(preloadFutures);
    }
  }

  /**
   * Retrieves a FHIR package from the source registry by its key and registers it in the local
   * storage.
   *
   * <p>If the property {@code dependency-loading-enabled} is set to true, all dependencies of the
   * package are retrieved recursively in parallel. The returned {@link CompletableFuture} completes
   * only when the package and all of its dependencies have been successfully loaded. If any
   * dependency cannot be resolved (no matching version for a wildcard specification like {@code
   * foo@1.0.x}) or found, the future completes exceptionally without waiting for all other
   * dependency retrievals to complete. This approach minimizes unnecessary waiting for the client
   * and ensures that a package becomes available in the registry only if all of its dependencies
   * are available as well.
   *
   * @param packageKey the identifier of the package to retrieve
   * @return a {@link CompletableFuture} that completes with the requested {@link FhirPackage}, or
   *     exceptionally if retrieval fails
   */
  public CompletableFuture<FhirPackage> retrievePackageAsync(PackageKey packageKey) {
    log.info(
        "Searching for package {}@{} in remote source registry",
        packageKey.name(),
        packageKey.version());

    return loadingPackages.computeIfAbsent(
        packageKey,
        key -> {
          log.info("Setting loading flag for package {}@{}", key.name(), key.version());
          return CompletableFuture.supplyAsync(
                  () -> retrieveAndRegisterPackage(packageKey), packageRetrievalExecutor)
              .whenComplete((pkg, ex) -> removeLoadingFlag(packageKey));
        });
  }

  private FhirPackage retrieveAndRegisterPackage(PackageKey packageKey) {
    byte[] tgz =
        retrieverPort
            .retrievePackageAsTgzBinary(packageKey)
            .orElseThrow(
                () ->
                    new PackageNotFoundException(
                        String.format(
                            "Package %s@%s not found", packageKey.name(), packageKey.version())));

    FhirPackage pkg = fhirPackageFactory.createFhirPackage(packageKey, tgz);

    if (retrieverProperties.isDependencyLoadingEnabled()) {
      retrieveDependenciesInParallel(pkg);
    }

    storagePort.storePackage(pkg);
    log.info("Package {}@{} added in the registry", pkg.getName(), pkg.getVersion());
    return pkg;
  }

  private void retrieveDependenciesInParallel(FhirPackage pkg) {
    log.info("Resolving dependencies for package {}@{}", pkg.getName(), pkg.getVersion());
    List<CompletableFuture<FhirPackage>> futures =
        pkg.getRawDependencies().entrySet().stream()
            .map(entry -> new PackageKey(entry.getKey(), entry.getValue()))
            .map(
                rawDep -> {
                  if (dependencyResolver.isVersionWithLatestPatch(rawDep.version())) {
                    return dependencyResolver
                        .resolveAgainstSourceRegistry(rawDep)
                        .or(() -> dependencyResolver.resolveAgainstLocalStorage(rawDep))
                        .orElseThrow(
                            () ->
                                new PackageNotFoundException(
                                    String.format(
                                        "Could not resolve dependency %s@%s for package %s@%s, no matching version found remotely or locally.",
                                        rawDep.name(),
                                        rawDep.version(),
                                        pkg.getName(),
                                        pkg.getVersion())));
                  }
                  return new PackageKey(rawDep.name(), rawDep.version());
                })
            .filter(key -> !storagePort.packageExists(key))
            .map(this::retrievePackageAsync)
            .toList();

    waitForAllOrFailFast(futures);
  }

  private void waitForAllOrFailFast(List<CompletableFuture<FhirPackage>> futures) {
    CompletableFuture<Void> allFutures =
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    futures.forEach(
        f ->
            f.whenComplete(
                (result, ex) -> {
                  if (ex != null) {
                    // Fail fast on first error
                    allFutures.completeExceptionally(ex);
                  }
                }));
    // Wait for all futures to complete.
    // Blocking with "join" is acceptable here because we use cheap virtual threads.
    // If a different (non-virtual) thread pool is used in the future,
    // this should be refactored to a fully asynchronous approach.
    allFutures.join();
  }

  private void removeLoadingFlag(PackageKey key) {
    var removedFuture = loadingPackages.remove(key);
    if (removedFuture != null) {
      log.info("Loading flag removed for package {}@{}", key.name(), key.version());
    } else {
      log.warn("Failed to remove loading flag for {}@{}", key.name(), key.version());
    }
  }
}
