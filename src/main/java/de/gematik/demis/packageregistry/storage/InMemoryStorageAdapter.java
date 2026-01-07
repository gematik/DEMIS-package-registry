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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
class InMemoryStorageAdapter implements PackageStoragePort {

  private final Map<PackageKey, FhirPackage> registry = new ConcurrentHashMap<>();

  @Override
  public Optional<FhirPackage> getPackage(PackageKey packageKey) {
    return Optional.ofNullable(registry.get(packageKey));
  }

  @Override
  public List<FhirPackage> getAllVersionsForPackage(String packageName) {
    return registry.values().stream()
        .filter(pkg -> pkg.getName().equals(packageName))
        .sorted(Comparator.comparing(p -> ModuleDescriptor.Version.parse(p.getVersion())))
        .toList();
  }

  @Override
  public void storePackage(FhirPackage fhirPackage) {
    registry.put(fhirPackage.getPackageKey(), fhirPackage);
  }

  @Override
  public boolean packageExists(PackageKey packageKey) {
    return registry.containsKey(packageKey);
  }
}
