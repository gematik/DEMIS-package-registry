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

import static de.gematik.demis.packageregistry.retriever.RetrieverProperties.RegistryType;
import static de.gematik.demis.packageregistry.retriever.RetrieverProperties.RegistryType.GCP_WITH_JSON_KEYFILE;

import de.gematik.demis.packageregistry.common.SpringRestClient;
import de.gematik.demis.packageregistry.core.PackageRetrieverPort;
import de.gematik.demis.packageregistry.retriever.verification.SupplyChainVerifierPort;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring factory for creating the appropriate {@link PackageRetrieverPort} implementation based on
 * the configured registry type.
 *
 * <p>Supports both public and private Google registries as well as generic public NPM registries.
 * Optionally wraps the retriever in {@link VerifyingPackageRetrieverAdapter} if supply chain
 * verification is enabled and supported for the registry type.
 */
@Configuration
@AllArgsConstructor
@Slf4j
class PackageRetrieverAdapterFactory {

  private final RetrieverProperties retrieverProperties;
  private final SpringRestClient restClient;
  private final SupplyChainVerifierPort supplyChainVerifier;
  private final ObjectProvider<GoogleAccessTokenProvider> googleAccessTokenProviderProvider;

  @Bean(name = "packageRetriever")
  public PackageRetrieverPort createPackageRetrieverAdapter() {
    var registryType = retrieverProperties.getSourceRegistry().type();
    log.info("Creating PackageRetrieverAdapter for registry type: {}", registryType.name());

    PackageRetrieverPort baseRetriever =
        switch (registryType) {
          case GCP_WITH_JSON_KEYFILE, GCP_PUBLIC -> createGooglePackageRetriever();
          case PUBLIC -> createPublicGenericPackageRetriever();
        };

    // Conditionally wrap with verifying decorator
    if (retrieverProperties.isSupplyChainVerificationEnabled()
        && registryTypeSupportsVerification(registryType)) {
      log.info("Supply chain verification is enabled.");
      return new VerifyingPackageRetrieverAdapter(
          baseRetriever, supplyChainVerifier, withCheckStageAttestation(registryType));
    }

    log.info("Supply chain verification is disabled or not applicable for this registry type.");
    return baseRetriever;
  }

  private boolean withCheckStageAttestation(RegistryType registryType) {
    return registryType == GCP_WITH_JSON_KEYFILE;
  }

  private boolean registryTypeSupportsVerification(RegistryType registryType) {
    return switch (registryType) {
      case GCP_WITH_JSON_KEYFILE, GCP_PUBLIC -> true;
      case PUBLIC -> false;
    };
  }

  private PackageRetrieverPort createGooglePackageRetriever() {
    GoogleAccessTokenProvider googleAccessTokenProvider =
        googleAccessTokenProviderProvider.getIfAvailable();
    return new GoogleRegistryPackageRetrieverAdapter(
        retrieverProperties, restClient, googleAccessTokenProvider);
  }

  private PackageRetrieverPort createPublicGenericPackageRetriever() {
    return new GenericPublicPackageRetrieverAdapter(retrieverProperties, restClient);
  }
}
