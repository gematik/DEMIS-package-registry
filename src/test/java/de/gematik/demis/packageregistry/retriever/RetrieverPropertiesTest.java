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

import static org.junit.jupiter.api.Assertions.*;

import de.gematik.demis.packageregistry.retriever.RetrieverProperties.SourceRegistryConfigurationException;
import org.junit.jupiter.api.Test;

class RetrieverPropertiesTest {

  @Test
  void validate_shouldThrowIfJsonKeyFileIsMissingForGcpRegistry() {
    // Preparation
    var sourceRegistryProperties =
        new RetrieverProperties.SourceRegistry(
            RetrieverProperties.RegistryType.GCP_WITH_JSON_KEYFILE, "http://example.com");
    var gcpProperties = new RetrieverProperties.Gcp(null);
    var retrieverProperties =
        new RetrieverProperties(sourceRegistryProperties, gcpProperties, null, false);

    // Execution and assertion
    var ex =
        assertThrows(SourceRegistryConfigurationException.class, retrieverProperties::validate);
    assertEquals(
        "Incorrect configuration: path to Json-KeyFile is required for registry of type GCP_WITH_JSON_KEYFILE",
        ex.getMessage());
  }

  @Test
  void validate_shouldThrowIfSupplyChainVerificationEnabledButMissingSignatureSigner() {
    var supplyChain =
        new RetrieverProperties.SupplyChainVerification(
            true, "", "trusted-attestation", "http://url");
    var retrieverProperties =
        new RetrieverProperties(
            new RetrieverProperties.SourceRegistry(
                RetrieverProperties.RegistryType.GCP_WITH_JSON_KEYFILE, "http://example.com"),
            new RetrieverProperties.Gcp("/path/to/key.json"),
            supplyChain,
            false);

    var ex =
        assertThrows(
            RetrieverProperties.SupplyChainVerificationException.class,
            retrieverProperties::validate);
    assertEquals(
        "If Supply chain verification is enabled, signature signer name must be provided.",
        ex.getMessage());
  }

  @Test
  void validate_shouldThrowIfSupplyChainVerificationEnabledButMissingAttestationSigner() {
    var supplyChain =
        new RetrieverProperties.SupplyChainVerification(true, "trusted-signer", "", "http://url");
    var retrieverProperties =
        new RetrieverProperties(
            new RetrieverProperties.SourceRegistry(
                RetrieverProperties.RegistryType.GCP_WITH_JSON_KEYFILE, "http://example.com"),
            new RetrieverProperties.Gcp("/path/to/key.json"),
            supplyChain,
            false);

    var ex =
        assertThrows(
            RetrieverProperties.SupplyChainVerificationException.class,
            retrieverProperties::validate);
    assertEquals(
        "If Supply chain verification is enabled and source registry is of type GCP_WITH_JSON_KEYFILE, attestation signer name must be provided.",
        ex.getMessage());
  }

  @Test
  void validate_shouldPassForGcpRegistryWithJsonKeyFile() {
    // Preparation
    var sourceRegistryProperties =
        new RetrieverProperties.SourceRegistry(
            RetrieverProperties.RegistryType.GCP_WITH_JSON_KEYFILE, "http://example.com");
    var gcpProperties = new RetrieverProperties.Gcp("/path/to/key.json");
    var supplyChain = new RetrieverProperties.SupplyChainVerification(false, null, null, null);
    var retrieverProperties =
        new RetrieverProperties(sourceRegistryProperties, gcpProperties, supplyChain, false);

    // Execution and assertion
    assertDoesNotThrow(retrieverProperties::validate);
  }

  @Test
  void isSupplyChainVerificationEnabled_shouldReturnFalseIfSupplyChainIsNull() {
    // Preparation
    var retrieverProperties =
        new RetrieverProperties(
            new RetrieverProperties.SourceRegistry(
                RetrieverProperties.RegistryType.GCP_WITH_JSON_KEYFILE, "http://example.com"),
            new RetrieverProperties.Gcp("/path/to/key.json"),
            null,
            false);

    assertFalse(retrieverProperties.isSupplyChainVerificationEnabled());
  }
}
