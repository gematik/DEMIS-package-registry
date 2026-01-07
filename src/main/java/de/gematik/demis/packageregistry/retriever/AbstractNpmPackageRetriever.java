package de.gematik.demis.packageregistry.retriever;

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
import de.gematik.demis.packageregistry.common.SpringRestClient;
import de.gematik.demis.packageregistry.core.PackageRetrieverPort;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Set;
import lombok.AllArgsConstructor;
import org.springframework.web.util.UriUtils;

/** Abstract base class for any NPM package retriever */
@AllArgsConstructor
abstract class AbstractNpmPackageRetriever implements PackageRetrieverPort {

  final SpringRestClient restClient;
  final RetrieverProperties retrieverProperties;

  /** Subclasses can override to provide a bearer token if needed. Default is null. */
  String getAuthToken() {
    return null;
  }

  @Override
  public Set<String> listAvailableVersions(String packageName) {
    return getPackageOverview(packageName)
        .map(FhirPackageOverviewDto::getVersions)
        .map(java.util.Map::keySet)
        .orElseGet(Set::of);
  }

  Optional<FhirPackageOverviewDto> getPackageOverview(String packageName) {
    return restClient.getJson(
        buildUrlForPackageOverview(packageName), FhirPackageOverviewDto.class, getAuthToken());
  }

  String buildUrlForPackageOverview(String packageName) {
    var registryUrl = retrieverProperties.getSourceRegistry().url();
    return String.format(
        "%s/%s", registryUrl, UriUtils.encodePathSegment(packageName, StandardCharsets.UTF_8));
  }
}
