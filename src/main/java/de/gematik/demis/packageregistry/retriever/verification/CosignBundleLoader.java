package de.gematik.demis.packageregistry.retriever.verification;

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
import de.gematik.demis.packageregistry.retriever.GoogleAccessTokenProvider;
import de.gematik.demis.packageregistry.retriever.RetrieverProperties;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@Slf4j
class CosignBundleLoader {

  private static final String SIGNATURE_SUFFIX = "sig.bundle.json";
  private static final String ATTESTATION_SUFFIX_TEMPLATE = "att.stage.%s.bundle.json";

  private final ObjectProvider<GoogleAccessTokenProvider> googleAccessTokenProviderProvider;
  private final SpringRestClient restClient;
  private final RetrieverProperties retrieverProperties;

  public byte[] loadSignature(String packageName, String packageVersion) {
    log.info("Loading signature for package {}@{}", packageName, packageVersion);
    return loadBundle(packageName, packageVersion, SIGNATURE_SUFFIX, "signature");
  }

  public byte[] loadAttestation(String packageName, String packageVersion) {
    var stage = retrieverProperties.getSupplyChainVerification().extractStageFromAttestationSan();
    log.info(
        "Loading attestation for package {}@{} on stage {}", packageName, packageVersion, stage);
    return loadBundle(packageName, packageVersion, getAttestationSuffix(stage), "attestation");
  }

  private byte[] loadBundle(String packageName, String packageVersion, String suffix, String type) {
    String url = buildUrl(packageName, packageVersion, suffix);
    return restClient
        .getBytes(url, getGoogleAccessToken())
        .orElseThrow(
            () ->
                new SupplyChainVerificationException(
                    String.format(
                        "Failed to load %s for package %s@%s", type, packageName, packageVersion)));
  }

  private String getGoogleAccessToken() {
    final var provider = googleAccessTokenProviderProvider.getIfAvailable();
    return provider == null ? null : provider.getToken();
  }

  private String getAttestationSuffix(String stage) {
    return String.format(ATTESTATION_SUFFIX_TEMPLATE, stage);
  }

  private String buildUrl(String packageName, String packageVersion, String suffix) {
    String encodedName = URLEncoder.encode(packageName, StandardCharsets.UTF_8);
    String encodedVersion = URLEncoder.encode(packageVersion, StandardCharsets.UTF_8);
    String encodedSuffix = URLEncoder.encode(suffix, StandardCharsets.UTF_8);

    return String.format(
        "%s/files/%s:%s:%s:download?alt=media",
        retrieverProperties.getSupplyChainVerification().signatureAttestationBaseUrl(),
        encodedName,
        encodedVersion,
        encodedSuffix);
  }
}
