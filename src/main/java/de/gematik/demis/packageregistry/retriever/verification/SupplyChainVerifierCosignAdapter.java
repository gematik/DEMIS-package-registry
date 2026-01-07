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

import com.google.common.hash.Hashing;
import de.gematik.demis.packageregistry.domain.PackageKey;
import de.gematik.demis.packageregistry.retriever.RetrieverProperties;
import dev.sigstore.KeylessVerificationException;
import dev.sigstore.KeylessVerifier;
import dev.sigstore.VerificationOptions;
import dev.sigstore.bundle.Bundle;
import dev.sigstore.bundle.BundleParseException;
import dev.sigstore.strings.StringMatcher;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
class SupplyChainVerifierCosignAdapter implements SupplyChainVerifierPort {

  private final RetrieverProperties retrieverProperties;
  private final CosignBundleLoader cosignBundleLoader;
  private final KeylessVerifier verifier;

  @Override
  public void verifySignature(PackageKey packageKey, byte[] tgz) {
    log.info(
        "Verifying supply chain signature for package {}@{}",
        packageKey.name(),
        packageKey.version());

    verifyCosignBundle(
        tgz,
        cosignBundleLoader.loadSignature(packageKey.name(), packageKey.version()),
        retrieverProperties.getSupplyChainVerification().signatureSan(),
        "signature",
        packageKey);

    log.info(
        "Supply chain signature for package {}@{} is valid",
        packageKey.name(),
        packageKey.version());
  }

  @Override
  public void verifyStageAttestation(PackageKey packageKey, byte[] tgz) {
    log.info(
        "Verifying stage attestation for package {}@{} on stage {}",
        packageKey.name(),
        packageKey.version(),
        retrieverProperties.getSupplyChainVerification().extractStageFromAttestationSan());

    verifyCosignBundle(
        tgz,
        cosignBundleLoader.loadAttestation(packageKey.name(), packageKey.version()),
        retrieverProperties.getSupplyChainVerification().attestationSan(),
        "stage attestation",
        packageKey);

    log.info(
        "Stage attestation for package {}@{} is valid", packageKey.name(), packageKey.version());
  }

  private void verifyCosignBundle(
      byte[] payload, byte[] bundleBytes, String signerName, String type, PackageKey packageKey) {
    try {
      verifier.verify(
          getHash(payload), parseBundle(bundleBytes), getVerificationOptionsWithSigner(signerName));
    } catch (KeylessVerificationException e) {
      throw new SupplyChainVerificationException(
          String.format(
              "Package %s@%s is not authorized: invalid %s",
              packageKey.name(), packageKey.version(), type),
          e);
    } catch (Exception e) {
      throw new SupplyChainVerificationException(
          String.format(
              "Failed to verify %s for package %s@%s",
              type, packageKey.name(), packageKey.version()),
          e);
    }
  }

  private VerificationOptions getVerificationOptionsWithSigner(String signerName) {
    return VerificationOptions.builder()
        .addCertificateMatchers(
            VerificationOptions.CertificateMatcher.fulcio()
                .issuer(StringMatcher.string("https://accounts.google.com"))
                .subjectAlternativeName(StringMatcher.string(signerName))
                .build())
        .build();
  }

  private byte[] getHash(byte[] payload) {
    return Hashing.sha256().hashBytes(payload).asBytes();
  }

  private Bundle parseBundle(byte[] bundleBytes) throws BundleParseException {
    return Bundle.from(
        new InputStreamReader(new ByteArrayInputStream(bundleBytes), StandardCharsets.UTF_8));
  }
}
