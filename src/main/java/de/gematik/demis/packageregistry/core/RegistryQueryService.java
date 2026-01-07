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
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** This service offers an interface to query the package registry */
@Service
@Slf4j
@RequiredArgsConstructor
public class RegistryQueryService {

  private final RetrieverProperties retrieverProperties;

  private final PackageStoragePort storagePort;

  private final RegistryLoadManager registryLoadManager;
  private final DependencyResolver dependencyResolver;

  /**
   * Retrieves a FHIR package by its key.
   *
   * <p>The method first checks the local storage. If the package is not found, it attempts to
   * retrieve it from the remote source.
   *
   * @param packageKey the identifier of the package to retrieve
   * @return a {@link CompletableFuture} that completes with the requested {@link FhirPackage}, or
   *     exceptionally if retrieval fails
   */
  public CompletableFuture<FhirPackage> getPackageAsync(PackageKey packageKey) {
    log.info(
        "Searching for package {}@{} in local storage", packageKey.name(), packageKey.version());

    return storagePort
        .getPackage(packageKey)
        .map(CompletableFuture::completedFuture)
        .orElseGet(() -> registryLoadManager.retrievePackageAsync(packageKey));
  }

  /**
   * Returns an aggregate containing all versions of a package along with their resolved
   * dependencies if the dependency-loading-mode has been enabled.
   */
  public Optional<PackageVersionAggregate> getPackageVersionAggregate(String packageName) {
    log.info("Preparing version aggregate for package {}", packageName);

    List<FhirPackage> allVersions = storagePort.getAllVersionsForPackage(packageName);
    if (allVersions.isEmpty()) {
      return Optional.empty();
    }

    Map<String, Set<PackageKey>> resolvedDependenciesMap;
    if (!retrieverProperties.isDependencyLoadingEnabled()) {
      resolvedDependenciesMap = Collections.emptyMap();
    } else {
      resolvedDependenciesMap =
          allVersions.stream()
              .collect(
                  Collectors.toMap(
                      FhirPackage::getVersion, dependencyResolver::getLocallyResolvedDependencies));
    }

    return Optional.of(
        PackageVersionAggregate.of(packageName, Set.copyOf(allVersions), resolvedDependenciesMap));
  }
}
