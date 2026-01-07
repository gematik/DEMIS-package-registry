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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import de.gematik.demis.packageregistry.TestHelper;
import de.gematik.demis.packageregistry.retriever.RetrieverProperties;
import dev.sigstore.KeylessVerificationException;
import dev.sigstore.KeylessVerifier;
import dev.sigstore.VerificationOptions;
import dev.sigstore.bundle.Bundle;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SupplyChainVerifierCosignAdapterTest {

  @Mock private RetrieverProperties retrieverPropertiesConfig;
  @Mock private RetrieverProperties.SupplyChainVerification supplyChainVerificationConfig;
  @Mock private CosignBundleLoader cosignBundleLoader;
  @Mock private KeylessVerifier verifier;

  @InjectMocks private SupplyChainVerifierCosignAdapter underTest;

  private static final String PATH_DUMMY_PACKAGE_SIGNATURE =
      "src/test/resources/dummy-package/dummy_package_sig.bundle.json";
  private static final String PATH_DUMMY_PACKAGE_ATTESTATION =
      "src/test/resources/dummy-package/dummy_package_att.stage.dev.bundle.json";

  @BeforeEach
  @SneakyThrows
  void setup() {
    when(retrieverPropertiesConfig.getSupplyChainVerification())
        .thenReturn(supplyChainVerificationConfig);
  }

  @SneakyThrows
  private void setupSignatureMocks() {
    when(supplyChainVerificationConfig.signatureSan()).thenReturn("sig-signer");
    when(cosignBundleLoader.loadSignature(anyString(), anyString()))
        .thenReturn(Files.readAllBytes(Path.of(PATH_DUMMY_PACKAGE_SIGNATURE)));
  }

  @SneakyThrows
  private void setupAttestationMocks() {
    when(supplyChainVerificationConfig.attestationSan()).thenReturn("att-signer");
    when(cosignBundleLoader.loadAttestation(anyString(), anyString()))
        .thenReturn(Files.readAllBytes(Path.of(PATH_DUMMY_PACKAGE_ATTESTATION)));
  }

  @Test
  void verifySignature_signatureInvalid_throws() throws Exception {
    // Preparation
    setupSignatureMocks();
    doThrow(new KeylessVerificationException("invalid signature"))
        .when(verifier)
        .verify(any(byte[].class), any(Bundle.class), any(VerificationOptions.class));
    var randomPackageKey = TestHelper.generateRandomPackageKey();

    // Execution & Assertion
    SupplyChainVerificationException exception =
        assertThrows(
            SupplyChainVerificationException.class,
            () -> underTest.verifySignature(randomPackageKey, new byte[] {1, 2, 3}));

    var expectedMessage =
        String.format(
            "Package %s@%s is not authorized: invalid signature",
            randomPackageKey.name(), randomPackageKey.version());
    assertEquals(expectedMessage, exception.getMessage());
  }

  @Test
  void verifyStageAttestation_attestationInvalid_throws() throws Exception {
    // Setup: configure the verifier to throw on stage attestation
    setupAttestationMocks();

    doThrow(new KeylessVerificationException("invalid attestation"))
        .when(verifier)
        .verify(any(byte[].class), any(Bundle.class), any(VerificationOptions.class));

    var randomPackageKey = TestHelper.generateRandomPackageKey();

    // Execution & Assertion
    SupplyChainVerificationException exception =
        assertThrows(
            SupplyChainVerificationException.class,
            () -> underTest.verifyStageAttestation(randomPackageKey, new byte[] {1, 2, 3}));

    var expectedMessage =
        String.format(
            "Package %s@%s is not authorized: invalid stage attestation",
            randomPackageKey.name(), randomPackageKey.version());
    assertEquals(expectedMessage, exception.getMessage());
  }

  @Test
  void verifySignature_genericException_throws() throws Exception {
    // Preparation
    setupSignatureMocks();

    doThrow(new RuntimeException("Unexpected error"))
        .when(verifier)
        .verify(any(byte[].class), any(Bundle.class), any(VerificationOptions.class));
    var randomPackageKey = TestHelper.generateRandomPackageKey();

    // Execution & Assertion
    SupplyChainVerificationException exception =
        assertThrows(
            SupplyChainVerificationException.class,
            () -> underTest.verifySignature(randomPackageKey, new byte[] {1, 2, 3}));

    var expectedMessage =
        String.format(
            "Failed to verify signature for package %s@%s",
            randomPackageKey.name(), randomPackageKey.version());
    assertEquals(expectedMessage, exception.getMessage());
    assertThat(exception.getCause()).isInstanceOf(RuntimeException.class);
  }

  @Test
  void verifyStageAttestation_attestationBundleParseException_throws() throws Exception {

    // Preparation
    setupAttestationMocks();
    doThrow(new RuntimeException("Unexpected error while checking attestation"))
        .when(verifier)
        .verify(any(byte[].class), any(Bundle.class), any(VerificationOptions.class));

    var randomPackageKey = TestHelper.generateRandomPackageKey();

    // Execution & Assertion
    SupplyChainVerificationException exception =
        assertThrows(
            SupplyChainVerificationException.class,
            () -> underTest.verifyStageAttestation(randomPackageKey, new byte[] {1, 2, 3}));

    var expectedMessage =
        String.format(
            "Failed to verify stage attestation for package %s@%s",
            randomPackageKey.name(), randomPackageKey.version());
    assertEquals(expectedMessage, exception.getMessage());
    assertThat(exception.getCause()).isInstanceOf(RuntimeException.class);
  }
}
