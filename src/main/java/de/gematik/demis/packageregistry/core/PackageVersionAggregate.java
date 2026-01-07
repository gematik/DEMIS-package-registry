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
import java.lang.module.ModuleDescriptor;
import java.util.*;
import lombok.Value;

/** Represents a FHIR package with all its versions available in the Registry (at least one) */
@Value
public class PackageVersionAggregate {
  String packageName;
  Set<FhirPackage> availableVersions;

  /**
   * The resolved dependencies for each version. Key is the version string, value the set of
   * resolved PackageKeys.
   */
  Map<String, Set<PackageKey>> resolvedDependencies;

  private PackageVersionAggregate(
      String packageName,
      Set<FhirPackage> availableVersions,
      Map<String, Set<PackageKey>> resolvedDependencies) {
    this.packageName = packageName;
    this.availableVersions = Collections.unmodifiableSet(availableVersions);
    this.resolvedDependencies = Collections.unmodifiableMap(resolvedDependencies);
  }

  public static PackageVersionAggregate of(
      String packageName,
      Set<FhirPackage> availableVersions,
      Map<String, Set<PackageKey>> resolvedDependencies) {
    checkIntegrity(packageName, availableVersions);
    return new PackageVersionAggregate(packageName, availableVersions, resolvedDependencies);
  }

  public String latestVersion() {
    // Latest stable version
    Optional<FhirPackage> latestStable =
        availableVersions.stream()
            .filter(FhirPackage::isStableVersion)
            .max(Comparator.comparing(pkg -> ModuleDescriptor.Version.parse(pkg.getVersion())));

    if (latestStable.isPresent()) {
      return latestStable.get().getVersion();
    }

    // Prerelease: highest major.minor.patch-label, then latest downloadedAt
    return availableVersions.stream()
        .max(
            Comparator.comparing(this::getComparableVersion)
                .thenComparing(FhirPackage::getDownloadedAt))
        .orElseThrow()
        .getVersion();
  }

  private ModuleDescriptor.Version getComparableVersion(FhirPackage pkg) {
    return ModuleDescriptor.Version.parse(pkg.getMajorMinorPatchVersion());
  }

  private static void checkIntegrity(String packageName, Set<FhirPackage> availableVersions) {
    if (availableVersions == null || availableVersions.isEmpty()) {
      throw new IllegalArgumentException("A Package Aggregate must have at least one Fhir Package");
    }

    boolean hasMismatch =
        availableVersions.stream().anyMatch(pkg -> !pkg.getName().equals(packageName));
    if (hasMismatch) {
      throw new IllegalArgumentException(
          "All Fhir Packages must match the given packageName: " + packageName);
    }
  }
}
