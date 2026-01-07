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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import de.gematik.demis.packageregistry.common.SpringRestClient;
import de.gematik.demis.packageregistry.retriever.GoogleAccessTokenProvider;
import de.gematik.demis.packageregistry.retriever.RetrieverProperties;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

@ExtendWith(MockitoExtension.class)
class CosignBundleLoaderTest {

  @Mock private ObjectProvider<GoogleAccessTokenProvider> tokenProvider;
  @Mock private GoogleAccessTokenProvider googleAccessTokenProvider;
  @Mock private SpringRestClient restClient;
  @Mock private RetrieverProperties retrieverProperties;

  @InjectMocks private CosignBundleLoader underTest;

  @BeforeEach
  void init() {
    when(retrieverProperties.getSupplyChainVerification())
        .thenReturn(getDummySupplyChainVerification());
    when(tokenProvider.getIfAvailable()).thenReturn(googleAccessTokenProvider);
    when(googleAccessTokenProvider.getToken()).thenReturn("token");
  }

  @Test
  void loadSignature_shouldThrowIfSignatureNotFound() {
    // Preparation
    var expectedUrl = "http://base.url/files/foo:1.0.0:sig.bundle.json:download?alt=media";
    when(restClient.getBytes(expectedUrl, "token")).thenReturn(Optional.empty());

    // Execution and Assertion
    SupplyChainVerificationException ex =
        assertThrows(
            SupplyChainVerificationException.class, () -> underTest.loadSignature("foo", "1.0.0"));
    assertEquals("Failed to load signature for package foo@1.0.0", ex.getMessage());
  }

  @Test
  void loadAttestation_shouldThrowIfAttestationNotFound() {
    // Preparation
    var expectedUrl =
        "http://base.url/files/foo:1.0.0:att.stage.dev.bundle.json:download?alt=media";
    when(restClient.getBytes(expectedUrl, "token")).thenReturn(Optional.empty());

    SupplyChainVerificationException ex =
        assertThrows(
            SupplyChainVerificationException.class,
            () -> underTest.loadAttestation("foo", "1.0.0"));
    assertEquals("Failed to load attestation for package foo@1.0.0", ex.getMessage());
  }

  private RetrieverProperties.SupplyChainVerification getDummySupplyChainVerification() {
    return new RetrieverProperties.SupplyChainVerification(
        true,
        "demis-gar-fhir-packages-trust-dev",
        "demis-gar-fhir-packages-trust-dev",
        "http://base.url");
  }
}
