package de.gematik.demis.packageregistry.core;

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

import de.gematik.demis.packageregistry.TestHelper;
import de.gematik.demis.packageregistry.core.FhirPackageFactory.DependencyReaderException;
import de.gematik.demis.packageregistry.domain.PackageKey;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class FhirPackageFactoryTest {

  FhirPackageFactory underTest = new FhirPackageFactory();

  private static final String PATH_DUMMY_PACKAGE =
      "src/test/resources/dummy-package/dummy_package.tgz";
  private static final String PATH_INVALID_TGZ = "src/test/resources/buggy-packages/invalid.ignore";
  private static final String PATH_PACKAGE_WITHOUT_DEPENDENCIES =
      "src/test/resources/buggy-packages/no-dependencies.tgz";
  private static final String PATH_NO_PACKAGE_JSON =
      "src/test/resources/buggy-packages/no-package-json.tgz";
  private static final String PATH_INVALID_PACKAGE_JSON =
      "src/test/resources/buggy-packages/invalid-package-json.tgz";

  @Test
  void readDependencies_happyPath() throws Exception {
    // Preparation
    PackageKey randomKey = TestHelper.generateRandomPackageKey();
    byte[] tgz = getPackageAsTgz(PATH_DUMMY_PACKAGE);

    // Execution
    var dependencies = underTest.readDependencies(randomKey, tgz);

    // Assertion
    assertEquals(2, dependencies.size());
    assertEquals("4.0.1", dependencies.get("hl7.fhir.r4.core"));
    assertEquals("1.5.3", dependencies.get("de.basisprofil.r4"));
  }

  @Test
  void readDependencies_PackageJsonNotFound() throws Exception {
    // Preparation
    PackageKey randomKey = TestHelper.generateRandomPackageKey();
    byte[] tgz = getPackageAsTgz(PATH_NO_PACKAGE_JSON);

    // Execution and Assertion
    var ex =
        assertThrows(
            DependencyReaderException.class, () -> underTest.readDependencies(randomKey, tgz));
    String expectedMessage =
        String.format(
            "package.json not found inside FHIR package %s@%s",
            randomKey.name(), randomKey.version());
    assertEquals(expectedMessage, ex.getMessage());
  }

  @Test
  void readDependencies_InvalidTgz() throws Exception {
    // Preparation
    PackageKey randomKey = TestHelper.generateRandomPackageKey();
    byte[] tgz = getPackageAsTgz(PATH_INVALID_TGZ);

    // Execution and Assertion
    var ex =
        assertThrows(
            DependencyReaderException.class, () -> underTest.readDependencies(randomKey, tgz));
    assertTrue(
        ex.getMessage()
            .contains(
                String.format(
                    "Failed to extract package.json from FHIR package %s@%s",
                    randomKey.name(), randomKey.version())));
  }

  @Test
  void readDependencies_NoDependencies() throws Exception {
    // Preparation
    PackageKey randomKey = TestHelper.generateRandomPackageKey();
    byte[] tgz = getPackageAsTgz(PATH_PACKAGE_WITHOUT_DEPENDENCIES);

    // Execution
    var dependencies = underTest.readDependencies(randomKey, tgz);

    // Assertion
    assertTrue(dependencies.isEmpty());
  }

  @Test
  void readDependencies_InvalidPackageJson() throws Exception {
    // Preparation
    PackageKey randomKey = TestHelper.generateRandomPackageKey();
    byte[] tgz = getPackageAsTgz(PATH_INVALID_PACKAGE_JSON);

    // Execution and Assertion
    var ex =
        assertThrows(
            DependencyReaderException.class, () -> underTest.readDependencies(randomKey, tgz));
    assertTrue(ex.getMessage().contains("Failed to parse dependencies from package.json"));
  }

  private byte[] getPackageAsTgz(String resourcePath) throws Exception {
    return Files.readAllBytes(Path.of(resourcePath));
  }
}
