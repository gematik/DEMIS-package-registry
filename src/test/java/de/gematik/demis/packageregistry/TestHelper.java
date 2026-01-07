package de.gematik.demis.packageregistry;

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

import de.gematik.demis.packageregistry.domain.PackageKey;
import java.util.Random;
import java.util.UUID;

public class TestHelper {

  public static String getRandomPkgName() {
    return "package_" + UUID.randomUUID().toString().substring(0, 8);
  }

  public static String getRandomPkgVersion() {
    Random random = new Random();
    int major = random.nextInt(10);
    int minor = random.nextInt(10);
    int patch = random.nextInt(10);
    return major + "." + minor + "." + patch;
  }

  public static PackageKey generateRandomPackageKey() {
    return new PackageKey(getRandomPkgName(), getRandomPkgVersion());
  }

  // Disable supply chain verification on Windows because PEM line endings are CRLF by default,
  // which breaks Cosign signature verification in tests.
  public static void disableSupplyChainValidationOnWindows() {
    if (System.getProperty("os.name").toLowerCase().contains("win")) {
      System.setProperty("retriever.supply-chain-verification.enabled", "false");
    }
  }
}
