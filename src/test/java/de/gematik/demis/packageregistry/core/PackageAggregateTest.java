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

import de.gematik.demis.packageregistry.domain.FhirPackage;
import de.gematik.demis.packageregistry.domain.PackageKey;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PackageAggregateTest {

  @Nested
  class checkInitialization {
    @Test
    void of_throwsWhenAvailableVersionsEmpty() {
      Set<FhirPackage> availableVersions = Set.of();
      Map<String, Set<PackageKey>> dependencies = Map.of();
      IllegalArgumentException ex =
          assertThrows(
              IllegalArgumentException.class,
              () -> PackageVersionAggregate.of("foo", availableVersions, dependencies));
      assertTrue(ex.getMessage().contains("at least one Fhir Package"));
    }

    @Test
    void of_throwsWhenNameMismatch() {
      Set<FhirPackage> availableVersions = Set.of(createFhirPackage("bar", "1.0.0", Instant.now()));
      Map<String, Set<PackageKey>> dependencies = Map.of();
      IllegalArgumentException ex =
          assertThrows(
              IllegalArgumentException.class,
              () -> PackageVersionAggregate.of("foo", availableVersions, dependencies));
      assertTrue(ex.getMessage().contains("must match the given packageName"));
    }
  }

  @Nested
  class checkLatestVersion {
    @Test
    void latestVersion_returnsHighestStableVersion() {
      // Preparation
      Instant instant = Instant.parse("2025-09-01T10:00:00Z");
      FhirPackage v1 = createFhirPackage("demis.disease", "1.0.0", instant);
      FhirPackage v2 = createFhirPackage("demis.disease", "1.2.0", instant);
      FhirPackage v3 = createFhirPackage("demis.disease", "1.3.0-beta", instant);
      PackageVersionAggregate aggregate =
          PackageVersionAggregate.of("demis.disease", Set.of(v1, v2, v3), Map.of());

      // Assertion
      assertEquals("1.2.0", aggregate.latestVersion());
    }

    @Test
    void latestVersion_returnsHighestStableVersion_timestampDoesntMatter() {
      // Preparation
      Instant instant = Instant.parse("2025-09-01T10:00:00Z");
      Map<String, Set<PackageKey>> dependencies = Map.of();
      FhirPackage v1 = createFhirPackage("demis.disease", "1.0.0", instant);
      FhirPackage v2 = createFhirPackage("demis.disease", "1.2.0", instant.minusSeconds(3600));
      FhirPackage v3 = createFhirPackage("demis.disease", "1.3.0-beta", instant);
      PackageVersionAggregate aggregate =
          PackageVersionAggregate.of("demis.disease", Set.of(v1, v2, v3), dependencies);

      // Assertion
      assertEquals("1.2.0", aggregate.latestVersion());
    }

    @Test
    void latestVersion_returnsHighestPrerelease_whenNoStable() {
      // Preparation
      Instant instant = Instant.parse("2025-09-01T10:00:00Z");
      Map<String, Set<PackageKey>> dependencies = Map.of();
      FhirPackage v1 = createFhirPackage("demis.disease", "1.0.0-foo", instant);
      FhirPackage v2 = createFhirPackage("demis.disease", "1.2.0-bar", instant);
      FhirPackage v3 = createFhirPackage("demis.disease", "1.4.0-baz", instant);
      PackageVersionAggregate aggregate =
          PackageVersionAggregate.of("demis.disease", Set.of(v1, v2, v3), dependencies);

      // Assertion
      assertEquals("1.4.0-baz", aggregate.latestVersion());
    }

    @Test
    void latestVersion_returnsHighestPrerelease_whenNoStable_timestampDoesntMatter() {
      // Preparation
      Instant instant = Instant.parse("2025-09-01T10:00:00Z");
      Map<String, Set<PackageKey>> dependencies = Map.of();
      FhirPackage v1 = createFhirPackage("demis.disease", "1.0.0-foo", instant);
      FhirPackage v2 = createFhirPackage("demis.disease", "1.2.0-bar", instant);
      FhirPackage v3 = createFhirPackage("demis.disease", "1.4.0-baz", instant.minusSeconds(3600));
      PackageVersionAggregate aggregate =
          PackageVersionAggregate.of("demis.disease", Set.of(v1, v2, v3), dependencies);

      // Assertion
      assertEquals("1.4.0-baz", aggregate.latestVersion());
    }

    @Test
    void latestVersion_fallbackToLatestDownloaded_whenMultipleSameMajorMinorPatch() {
      // Preparation
      Instant instant = Instant.parse("2025-09-01T10:00:00Z");
      Map<String, Set<PackageKey>> dependencies = Map.of();
      FhirPackage v1 = createFhirPackage("demis.disease", "1.0.0-foo", instant);
      FhirPackage v2 = createFhirPackage("demis.disease", "1.0.0-bar", instant.plusSeconds(120));
      PackageVersionAggregate aggregate =
          PackageVersionAggregate.of("demis.disease", Set.of(v1, v2), dependencies);

      // Assertion
      assertEquals("1.0.0-bar", aggregate.latestVersion());
    }
  }

  private FhirPackage createFhirPackage(String name, String version, Instant downloadedAt) {
    return FhirPackage.of(
        new PackageKey(name, version), downloadedAt, ByteBuffer.allocate(0), Map.of());
  }
}
