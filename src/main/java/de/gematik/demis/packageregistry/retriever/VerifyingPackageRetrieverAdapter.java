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

import de.gematik.demis.packageregistry.core.PackageRetrieverPort;
import de.gematik.demis.packageregistry.domain.PackageKey;
import de.gematik.demis.packageregistry.retriever.verification.SupplyChainVerifierPort;
import java.util.Optional;
import java.util.Set;
import lombok.AllArgsConstructor;

/**
 * Decorator for {@link PackageRetrieverPort} that applies supply chain validation to retrieved
 * packages.
 */
@AllArgsConstructor
class VerifyingPackageRetrieverAdapter implements PackageRetrieverPort {

  private final PackageRetrieverPort delegate;
  private final SupplyChainVerifierPort verifier;
  private boolean checkStageAttestation;

  @Override
  public Optional<byte[]> retrievePackageAsTgzBinary(PackageKey packageKey) {
    var packageOpt = delegate.retrievePackageAsTgzBinary(packageKey);
    packageOpt.ifPresent(
        tgz -> {
          verifier.verifySignature(packageKey, tgz);
          if (checkStageAttestation) {
            verifier.verifyStageAttestation(packageKey, tgz);
          }
        });
    return packageOpt;
  }

  @Override
  public Set<String> listAvailableVersions(String packageName) {
    return delegate.listAvailableVersions(packageName);
  }
}
