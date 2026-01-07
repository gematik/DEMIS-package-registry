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

import jakarta.annotation.PostConstruct;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Value;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "retriever")
@Value
@Validated
@AllArgsConstructor
public class RetrieverProperties {

  @Valid @NotNull SourceRegistry sourceRegistry;
  @Valid @NotNull Gcp gcp;
  @Valid @NotNull SupplyChainVerification supplyChainVerification;
  boolean dependencyLoadingEnabled;

  /** Use for conditional or more complex checks not covered by simple annotations. */
  @PostConstruct
  public void validate() {
    validateSourceRegistry();
    validateSupplyChainSecurity();
  }

  private void validateSourceRegistry() {
    if (sourceRegistry.type == RegistryType.GCP_WITH_JSON_KEYFILE
        && StringUtils.isBlank(gcp.serviceAccountKeyfilePath)) {
      throw new SourceRegistryConfigurationException(
          "Incorrect configuration: path to Json-KeyFile is required for registry of type GCP_WITH_JSON_KEYFILE");
    }
  }

  private void validateSupplyChainSecurity() {
    if (supplyChainVerification.enabled()) {
      if (StringUtils.isBlank(supplyChainVerification.signatureSan)) {
        throw new SupplyChainVerificationException(
            "If Supply chain verification is enabled, signature signer name must be provided.");
      }
      if (sourceRegistry.type == RegistryType.GCP_WITH_JSON_KEYFILE
          && StringUtils.isBlank(supplyChainVerification.attestationSan)) {
        throw new SupplyChainVerificationException(
            "If Supply chain verification is enabled and source registry is of type GCP_WITH_JSON_KEYFILE, attestation signer name must be provided.");
      }
    }
  }

  public boolean isSupplyChainVerificationEnabled() {
    return supplyChainVerification != null && supplyChainVerification.enabled();
  }

  public record SourceRegistry(@NotNull RegistryType type, @NotBlank String url) {}

  public record Gcp(String serviceAccountKeyfilePath) {}

  public record SupplyChainVerification(
      boolean enabled,
      // Subject Alternative Name (SAN) of the signing certificate used for signature and
      // stage attestation
      String signatureSan,
      String attestationSan,
      String signatureAttestationBaseUrl) {

    public String extractStageFromAttestationSan() {
      String serviceAccountName = attestationSan.split("@")[0];
      int lastDash = serviceAccountName.lastIndexOf('-');
      return serviceAccountName.substring(lastDash + 1);
    }
  }

  public enum RegistryType {
    GCP_WITH_JSON_KEYFILE,
    GCP_PUBLIC,
    PUBLIC
  }

  static class SourceRegistryConfigurationException extends RuntimeException {
    public SourceRegistryConfigurationException(String message) {
      super(message);
    }
  }

  static class SupplyChainVerificationException extends RuntimeException {
    public SupplyChainVerificationException(String message) {
      super(message);
    }
  }
}
