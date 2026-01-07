package de.gematik.demis.packageregistry.domain;

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

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class FhirPackageTest {

  @ParameterizedTest
  @ValueSource(
      strings = {
        "1.0.0",
        "2.3.4-alpha",
        "5.10.7-latest-demis-3855-pkg-cosign",
        "1.2.3-beta.1",
        "0.0.1-foo.bar"
      })
  void of_acceptsValidVersions(String version) {
    FhirPackage pkg = createPackageWithKey(new PackageKey("foo", version));
    assertEquals(version, pkg.getVersion());
  }

  @ParameterizedTest
  @ValueSource(strings = {"1.0", "1.0.x", "foo", "1.0.0-", "1..0.0", "1.0.0..beta"})
  void of_rejectsInvalidVersions(String version) {
    var packageKey = new PackageKey("foo", version);
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> FhirPackage.of(packageKey, Instant.now(), ByteBuffer.allocate(0), Map.of()));
    assertEquals(ex.getMessage(), String.format("Invalid version:  %s@%s", "foo", version));
  }

  @ParameterizedTest
  @ValueSource(strings = {"1.0.0-beta", "1.0.0-beta.1", "5.10.7-latest-demis-3855-pkg-cosign"})
  void isPrerelease_returnsTrueIfLabelIsPresent(String version) {
    FhirPackage pkg = createPackageWithKey(new PackageKey("foo", version));
    assertTrue(pkg.isPrereleaseVersion());
  }

  @Test
  void getMajorMinorPatchVersion_workAsExpected() {
    FhirPackage pkg = createPackageWithKey(new PackageKey("foo", "1.2.3-beta.1"));
    assertEquals("1.2.3", pkg.getMajorMinorPatchVersion());
  }

  @Test
  void isPrerelease_returnsFalseForStableVersion() {
    FhirPackage pkg = createPackageWithKey(new PackageKey("foo", "1.2.3"));
    assertFalse(pkg.isPrereleaseVersion());
  }

  @Test
  void isStableVersion_returnsTrueForNoLabel() {
    FhirPackage pkg = createPackageWithKey(new PackageKey("foo", "1.2.3"));
    assertTrue(pkg.isStableVersion());
  }

  @Test
  void isStableVersion_returnsFalseForLabel() {
    FhirPackage pkg = createPackageWithKey(new PackageKey("foo", "1.2.3-beta"));
    assertFalse(pkg.isStableVersion());
  }

  private FhirPackage createPackageWithKey(PackageKey foo) {
    return FhirPackage.of(foo, Instant.now(), ByteBuffer.allocate(0), Map.of());
  }
}
