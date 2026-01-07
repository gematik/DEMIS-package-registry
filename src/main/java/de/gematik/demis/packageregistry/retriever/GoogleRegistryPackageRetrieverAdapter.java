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

import de.gematik.demis.packageregistry.common.SpringRestClient;
import de.gematik.demis.packageregistry.domain.PackageKey;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.util.UriUtils;

/** Adapter for retrieving packages from a Google Cloud Platform (GCP) hosted NPM registry. */
@Slf4j
final class GoogleRegistryPackageRetrieverAdapter extends AbstractNpmPackageRetriever {

  /** Null if the registry of type GCP_PUBLIC */
  private final GoogleAccessTokenProvider googleAccessTokenProvider;

  public GoogleRegistryPackageRetrieverAdapter(
      RetrieverProperties retrieverProperties,
      SpringRestClient restClient,
      GoogleAccessTokenProvider googleAccessTokenProvider) {
    super(restClient, retrieverProperties);
    this.googleAccessTokenProvider = googleAccessTokenProvider;
  }

  @Override
  public Optional<byte[]> retrievePackageAsTgzBinary(PackageKey packageKey) {
    log.info(
        "Requesting package {}@{} from remote source registry",
        packageKey.name(),
        packageKey.version());
    try {
      return restClient.getBytes(
          buildUrlForPackageFetch(packageKey.name(), packageKey.version()), getAuthToken());

    } catch (Exception e) {
      throw new PackageRetrieverException(
          String.format("Failed to load package %s@%s", packageKey.name(), packageKey.version()),
          e);
    }
  }

  @Override
  protected String getAuthToken() {
    return googleAccessTokenProvider != null ? googleAccessTokenProvider.getToken() : null;
  }

  private String buildUrlForPackageFetch(String packageName, String version) {
    var registryUrl = retrieverProperties.getSourceRegistry().url();
    String encodedPackageName = getEncodedPackageName(packageName);
    return String.format(
        "%s/%s/-/%s-%s.tgz", registryUrl, encodedPackageName, encodedPackageName, version);
  }

  private String getEncodedPackageName(String packageName) {
    return UriUtils.encodePathSegment(packageName, StandardCharsets.UTF_8);
  }
}
