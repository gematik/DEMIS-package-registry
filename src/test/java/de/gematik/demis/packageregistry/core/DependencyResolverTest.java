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
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DependencyResolverTest {

  @Mock private PackageStoragePort storagePort;
  @Mock private PackageRetrieverPort retrieverPort;
  @InjectMocks private DependencyResolver underTest;

  @DisplayName(
      "Given a dependency with exact depRawVersion, then the exact depRawVersion is returned if available")
  @Test
  void resolvesExactVersion_available() {
    // Preparation
    var depRawName = TestHelper.getRandomPkgName();
    var depRawVersion = "1.2.3";
    var availableVersions = Set.of("1.2.3", "1.2.5");

    // Execution
    var resolved =
        underTest.resolveWithBestAvailableDependency(depRawName, depRawVersion, availableVersions);

    // Assertion
    assertTrue(resolved.isPresent());
    assertEquals(depRawVersion, resolved.get().version());
  }

  @DisplayName(
      "Given a dependency with exact depRawVersion, an empty optional is returned if depRawVersion not available")
  @Test
  void resolvesExactVersion_notAvailable() {
    // Preparation
    var depRawName = TestHelper.getRandomPkgName();
    var depRawVersion = "1.2.3";
    var availableVersions = Set.<String>of();

    // Execution
    var resolved =
        underTest.resolveWithBestAvailableDependency(depRawName, depRawVersion, availableVersions);

    // Assertion
    assertTrue(resolved.isEmpty());
  }

  @DisplayName(
      "Given a dependency with patch wildcard, when resolving, then the highest matching patch is returned")
  @Test
  void resolvesPatchWildcard() {
    // Preparation
    var depRawName = TestHelper.getRandomPkgName();
    var depRawVersion = "1.2.x";
    var availableVersions = Set.of("1.2.0", "1.2.7", "1.3.1");

    // Execution
    var resolved =
        underTest.resolveWithBestAvailableDependency(depRawName, depRawVersion, availableVersions);

    // Assertion
    assertTrue(resolved.isPresent());
    assertEquals("1.2.7", resolved.get().version());
  }

  @DisplayName(
      "Given a dependency with no matching depRawVersion, when resolving, then an empty result is returned")
  @Test
  void returnsEmptyIfNoMatchingVersion() {
    // Preparation
    var depRawName = TestHelper.getRandomPkgName();
    var depRawVersion = "2.0.x";
    var availableVersions = Set.of("1.2.0", "1.2.7", "1.3.1");

    // Execution
    var resolved =
        underTest.resolveWithBestAvailableDependency(depRawName, depRawVersion, availableVersions);

    // Assertion
    assertTrue(resolved.isEmpty());
  }
}
