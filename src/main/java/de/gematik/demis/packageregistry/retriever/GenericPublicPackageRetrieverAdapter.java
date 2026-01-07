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
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

/**
 * Adapter to retrieve FHIR packages from any generic public NPM registry, including Simplifier.
 *
 * <p>This adapter does not make assumptions about the URL structure for fetching the TGZ binary,
 * unlike {@link GoogleRegistryPackageRetrieverAdapter}, which relies on a specific URL pattern.
 * Instead, the download URL is extracted dynamically from the package overview metadata.
 *
 * <p><strong>Note:</strong> This adapter does not perform any supply chain validation or integrity
 * checks on the downloaded packages.
 *
 * <p>Use this adapter when you need a generic, registry-agnostic package retriever and do not
 * require supply chain validation.
 */
@Slf4j
final class GenericPublicPackageRetrieverAdapter extends AbstractNpmPackageRetriever {

  public GenericPublicPackageRetrieverAdapter(
      RetrieverProperties retrieverProperties, SpringRestClient restClient) {
    super(restClient, retrieverProperties);
  }

  @Override
  public Optional<byte[]> retrievePackageAsTgzBinary(PackageKey packageKey) {
    return getPackageOverview(packageKey.name())
        .map(overview -> overview.getVersions().get(packageKey.version()))
        .map(versionInfo -> versionInfo.dist().tarball())
        .flatMap(url -> restClient.getBytes(url, null));
  }
}
