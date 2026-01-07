package de.gematik.demis.packageregistry.provider;

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

import de.gematik.demis.packageregistry.common.FhirPackageOverviewDto;
import de.gematik.demis.packageregistry.core.PackageVersionAggregate;
import de.gematik.demis.packageregistry.domain.FhirPackage;
import de.gematik.demis.packageregistry.domain.PackageKey;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
class FhirPackageMapper {

  public FhirPackageOverviewDto toDto(PackageVersionAggregate aggregate, String baseUrl) {
    return FhirPackageOverviewDto.builder()
        .id(aggregate.getPackageName())
        .name(aggregate.getPackageName())
        .distTags(Map.of("latest", aggregate.latestVersion()))
        .versions(
            aggregate.getAvailableVersions().stream()
                .collect(
                    Collectors.toMap(
                        FhirPackage::getVersion,
                        pkg -> toDtoVersion(pkg, baseUrl, aggregate.getResolvedDependencies()))))
        .build();
  }

  private FhirPackageOverviewDto.FhirPackageVersion toDtoVersion(
      FhirPackage pkg, String baseUrl, Map<String, Set<PackageKey>> resolvedDependencies) {

    var dependencies =
        resolvedDependencies.containsKey(pkg.getVersion())
            ? resolvedDependencies.get(pkg.getVersion()).stream()
                .collect(Collectors.toMap(PackageKey::name, PackageKey::version))
            : null;

    String tarballUrl = baseUrl + pkg.getVersion();

    return new FhirPackageOverviewDto.FhirPackageVersion(
        pkg.getName(), pkg.getVersion(), new FhirPackageOverviewDto.Dist(tarballUrl), dependencies);
  }
}
