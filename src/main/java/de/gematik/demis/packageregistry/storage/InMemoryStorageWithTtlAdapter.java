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

import de.gematik.demis.packageregistry.core.PackageStoragePort;
import de.gematik.demis.packageregistry.domain.FhirPackage;
import de.gematik.demis.packageregistry.domain.PackageKey;
import java.lang.module.ModuleDescriptor;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "feature.flag.pr-pkg-ttl", havingValue = "true")
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class InMemoryStorageWithTtlAdapter implements PackageStoragePort {

  private final Map<PackageKey, CacheEntry> cache = new ConcurrentHashMap<>();
  private final InMemoryStorageWithTtlAdapterProperties properties;

  @Override
  public Optional<FhirPackage> getPackage(PackageKey packageKey) {
    CacheEntry entry = cache.get(packageKey);
    if (entry == null || entry.isExpired()) {
      log.info("Package {} not found or expired, returning empty", packageKey);
      return Optional.empty();
    }
    entry.refreshExpiry(properties);
    log.info("Package {} accessed, expiry refreshed to {}", packageKey, entry.expiry);
    return Optional.of(entry.pkg);
  }

  @Override
  public List<FhirPackage> getAllVersionsForPackage(String packageName) {
    log.info("Retrieving all versions for package '{}'", packageName);
    return cache.entrySet().stream()
        .filter(e -> !e.getValue().isExpired())
        .filter(e -> e.getKey().name().equals(packageName))
        .sorted(Comparator.comparing(e -> ModuleDescriptor.Version.parse(e.getKey().version())))
        .map(e -> e.getValue().pkg)
        .toList();
  }

  @Override
  public void storePackage(FhirPackage fhirPackage) {
    final long seconds = properties.ttlAfterUse().toSeconds();
    Instant expiry = Instant.now().plusSeconds(seconds);
    cache.put(fhirPackage.getPackageKey(), new CacheEntry(fhirPackage, expiry));
    log.info("Package {} stored with expiry at {}", fhirPackage.getPackageKey(), expiry);
  }

  @Override
  public boolean packageExists(PackageKey packageKey) {
    CacheEntry entry = cache.get(packageKey);
    boolean exists = entry != null && !entry.isExpired();
    log.info("Package {} existence check: {}", packageKey, exists);
    return exists;
  }

  @Scheduled(cron = "${in-memory-cache.cleaner-cron}")
  void removeExpired() {
    cache.entrySet().removeIf(e -> e.getValue().isExpired());
    log.info("Expired packages removed, current cache size: {}", cache.size());
  }

  /*
   * This method is for testing purposes only to verify the expiry time of a package.
   */
  final Optional<Instant> getExpiryFor(PackageKey key) {
    CacheEntry entry = cache.get(key);
    return entry != null ? Optional.of(entry.getExpiry()) : Optional.empty();
  }

  @Data
  @AllArgsConstructor
  private static final class CacheEntry {
    private final FhirPackage pkg;
    private volatile Instant expiry;

    boolean isExpired() {
      var timestamp = Instant.now();
      return timestamp.isAfter(expiry);
    }

    void refreshExpiry(InMemoryStorageWithTtlAdapterProperties properties) {
      this.expiry = Instant.now().plusSeconds(properties.ttlAfterUse().toSeconds());
    }
  }
}
