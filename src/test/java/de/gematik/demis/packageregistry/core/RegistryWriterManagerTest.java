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
import static org.mockito.Mockito.when;

import de.gematik.demis.packageregistry.domain.PackageKey;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RegistryWriterManagerTest {

  @Mock private InitialPackageProperties initialPackageProperties;
  @Mock private PackageStoragePort storagePort;
  @Mock private PackageRetrieverPort retrieverPort;
  @InjectMocks private RegistryLoadManager underTest;

  @DisplayName("Initialization of registry will fail if a package marked for preloading is missing")
  @Test
  void initializeRegistryFailsIfPackageIsMissing() {
    // Preparation
    var packageToBeLoaded = new InitialPackageProperties.PackageTarget("foo", List.of("1.2.3"));
    when(initialPackageProperties.getPackages()).thenReturn(List.of(packageToBeLoaded));
    when(retrieverPort.retrievePackageAsTgzBinary(new PackageKey("foo", "1.2.3")))
        .thenReturn(Optional.empty());

    // Execution and Assertion
    CompletionException thrown =
        assertThrows(CompletionException.class, underTest::initializeRegistry);
    assertInstanceOf(PackageNotFoundException.class, thrown.getCause());
  }
}
