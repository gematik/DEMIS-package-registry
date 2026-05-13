package de.gematik.demis.packageregistry.storage;

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

import static org.assertj.core.api.Assertions.assertThat;

import de.gematik.demis.packageregistry.domain.FhirPackage;
import de.gematik.demis.packageregistry.domain.PackageKey;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InMemoryStorageAdapterWithTtlTest {
  private InMemoryStorageWithTtlAdapter adapter;
  private final String cron = System.getProperty("in-memory-cache.cleaner-cron", "* * 23 * * *");

  @BeforeEach
  void setUp() {
    final var properties = new InMemoryStorageWithTtlAdapterProperties(Duration.ofSeconds(2), cron);
    adapter = new InMemoryStorageWithTtlAdapter(properties);
  }

  @Test
  void packageNotFoundAfterTtlTest() {
    PackageKey key = new PackageKey("expired-package", "1.0.0");
    FhirPackage pkg =
        FhirPackage.of(
            key, Instant.now(), ByteBuffer.wrap(new byte[] {}).asReadOnlyBuffer(), Map.of());
    adapter.storePackage(pkg);

    assertThat(adapter.packageExists(key)).isTrue();

    Awaitility.await()
        .atMost(6, TimeUnit.SECONDS)
        .untilAsserted(() -> assertThat(adapter.packageExists(key)).isFalse());
  }

  @Test
  void packageIsRemovedFromCacheAfterItsTtlTest() {
    PackageKey key = new PackageKey("removable-package", "1.0.0");
    FhirPackage pkg =
        FhirPackage.of(
            key, Instant.now(), ByteBuffer.wrap(new byte[] {}).asReadOnlyBuffer(), Map.of());
    adapter.storePackage(pkg);

    assertThat(adapter.packageExists(key)).isTrue();

    Awaitility.await()
        .atMost(6, TimeUnit.SECONDS)
        .untilAsserted(() -> assertThat(adapter.packageExists(key)).isFalse());
    adapter.removeExpired();

    assertThat(adapter.getExpiryFor(key)).isEmpty();
  }

  @Test
  void emptyIsReturnedWhenPackageIsAbsentTest() {

    PackageKey key = new PackageKey("nonexistent-package", "1.0.0");
    assertThat(adapter.getPackage(key)).isEmpty();
  }

  @Test
  void packageExistsReturnsFalseIfAbsentTest() {
    PackageKey key = new PackageKey("test-package", "1.0.0");
    assertThat(adapter.packageExists(key)).isFalse();
  }

  @Test
  void packageExistsReturnsTrueIfPresentTest() {
    PackageKey key = new PackageKey("test-package", "1.0.0");
    FhirPackage pkg =
        FhirPackage.of(
            key, Instant.now(), ByteBuffer.wrap(new byte[] {}).asReadOnlyBuffer(), Map.of());
    adapter.storePackage(pkg);

    assertThat(adapter.packageExists(key)).isTrue();
  }

  @Test
  void getAllVersionsForPackageReturnsSortedVersionsTest() {
    PackageKey key1 = new PackageKey("test-package", "1.0.0");
    PackageKey key2 = new PackageKey("test-package", "2.0.0");
    FhirPackage pkg1 =
        FhirPackage.of(key1, Instant.now(), ByteBuffer.wrap(new byte[] {}), Map.of());
    FhirPackage pkg2 =
        FhirPackage.of(key2, Instant.now(), ByteBuffer.wrap(new byte[] {}), Map.of());
    adapter.storePackage(pkg2);
    adapter.storePackage(pkg1);

    var versions = adapter.getAllVersionsForPackage("test-package");
    assertThat(versions).hasSize(2);
    assertThat(versions.get(0).getVersion()).isEqualTo("1.0.0");
    assertThat(versions.get(1).getVersion()).isEqualTo("2.0.0");
  }

  @Test
  void getPackageRefreshesExpiryTest() {
    PackageKey key = new PackageKey("test-package", "1.0.0");
    FhirPackage pkg =
        FhirPackage.of(
            key, Instant.now(), ByteBuffer.wrap(new byte[] {}).asReadOnlyBuffer(), Map.of());
    adapter.storePackage(pkg);

    Instant initialExpiry = adapter.getExpiryFor(key).orElseThrow();

    Awaitility.await().pollDelay(Duration.ofMillis(200)).until(() -> true);

    adapter.getPackage(key);

    Instant refreshedExpiry = adapter.getExpiryFor(key).orElseThrow();

    assertThat(refreshedExpiry).isAfter(initialExpiry);
  }
}
