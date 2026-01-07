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
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.semver4j.Semver;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
class DependencyResolver {

  private static final String DEPENDENCY_WITH_HIGHEST_PATCH_VERSION_REGEX =
      "^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.x$";

  private final PackageStoragePort storagePort;
  private final PackageRetrieverPort retrieverPort;

  /**
   * For a given dependency identified by name and raw version (e.g."bar@1.2.x") returns the Package
   * Key of the best matching package among the available versions.
   */
  public Optional<PackageKey> resolveWithBestAvailableDependency(
      String dependencyName, String dependencyRawVersion, Set<String> availableVersions) {
    return availableVersions.stream()
        .map(Semver::new)
        .filter(semver -> semver.satisfies(dependencyRawVersion))
        .max(Semver::compareTo)
        .map(semver -> new PackageKey(dependencyName, semver.getVersion()));
  }

  public boolean isVersionWithLatestPatch(String version) {
    return version.matches(DEPENDENCY_WITH_HIGHEST_PATCH_VERSION_REGEX);
  }

  public Optional<PackageKey> resolveAgainstSourceRegistry(PackageKey rawDependency) {
    log.info(
        "Resolving dependency {}@{} from remote", rawDependency.name(), rawDependency.version());

    return resolveWithBestAvailableDependency(
        rawDependency.name(),
        rawDependency.version(),
        retrieverPort.listAvailableVersions(rawDependency.name()));
  }

  public Optional<PackageKey> resolveAgainstLocalStorage(PackageKey rawDependency) {
    log.info("Resolving dependency {}@{} locally", rawDependency.name(), rawDependency.version());

    var availableVersions =
        storagePort.getAllVersionsForPackage(rawDependency.name()).stream()
            .map(FhirPackage::getVersion)
            .collect(Collectors.toSet());

    return resolveWithBestAvailableDependency(
        rawDependency.name(), rawDependency.version(), availableVersions);
  }

  public Set<PackageKey> getLocallyResolvedDependencies(FhirPackage fhirPackage) {
    return fhirPackage.getRawDependencies().entrySet().stream()
        .map(rawDep -> new PackageKey(rawDep.getKey(), rawDep.getValue()))
        .map(
            rawDep -> {
              if (isVersionWithLatestPatch(rawDep.version())) {
                return resolveAgainstLocalStorage(rawDep);
              }
              PackageKey packageKey = new PackageKey(rawDep.name(), rawDep.version());
              return storagePort.packageExists(packageKey)
                  ? Optional.of(packageKey)
                  : Optional.<PackageKey>empty();
            })
        .flatMap(Optional::stream)
        .collect(Collectors.toSet());
  }
}
