package de.gematik.demis.packageregistry.provider;

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

import static org.junit.jupiter.api.Assertions.assertTrue;

import de.gematik.demis.packageregistry.core.PackageStoragePort;
import de.gematik.demis.packageregistry.domain.PackageKey;
import de.gematik.demis.packageregistry.retriever.FakeTokenProviderConfig;
import lombok.SneakyThrows;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Integration test for preloading FHIR packages at application startup, with focus on dependency
 * handling
 */
@ActiveProfiles("test-preloading-with-dependencies")
@SpringBootTest
@AutoConfigureMockMvc
@Import(FakeTokenProviderConfig.class)
class PackagePreloadingWithDependenciesIT {

  private static final String DUMMY_PACKAGE = "src/test/resources/dummy-package/dummy_package.tgz";
  private static final String PACKAGE_WITHOUT_DEPENDENCIES =
      "src/test/resources/buggy-packages/no-dependencies.tgz";
  private static final EnhancedWireMock MOCK_SERVER = EnhancedWireMock.createWithDynamicPort(true);

  @MockitoSpyBean private PackageStoragePort storagePort;
  @Autowired private MockMvc mockMvc;

  @DynamicPropertySource
  static void registerWireMockProperties(DynamicPropertyRegistry registry) {
    MOCK_SERVER.startMockAndRegisterProperties(registry);
  }

  @BeforeAll
  @SneakyThrows
  static void init() {
    runWireMock();
  }

  // We use WireMock to mock the Google Package Registry, which is used to retrieve the FHIR
  // packages and the artefacts for supply chain verification. Since this process happens at
  // application startup, we need to start WireMock before loading the Spring ApplicationContext.
  private static void runWireMock() {
    // Stub get package request
    MOCK_SERVER.onCallReturnsBytes("/packageABC/-/packageABC-1.2.3.tgz", DUMMY_PACKAGE);
    // Stub dependencies of Dummy Package
    MOCK_SERVER.onCallReturnsBytes(
        "/hl7.fhir.r4.core/-/hl7.fhir.r4.core-4.0.1.tgz", PACKAGE_WITHOUT_DEPENDENCIES);
    MOCK_SERVER.onCallReturnsBytes(
        "/de.basisprofil.r4/-/de.basisprofil.r4-1.5.3.tgz", PACKAGE_WITHOUT_DEPENDENCIES);
  }

  @AfterAll
  static void stopWiremock() {
    MOCK_SERVER.stop();
  }

  @DisplayName(
      "Dependencies of packages marked for preload are retrieved on application start and available in the registry")
  @SneakyThrows
  @Test
  void getPackage_dependencyOfInitialLoadedPackageIsAvailable() {
    // Preparation

    // Package hl7.fhir.r4.core@4.0.1 is a dependency of packageABC@1.2.3, which has been
    // preloaded at application start

    // Execution
    var result = storagePort.getPackage(new PackageKey("hl7.fhir.r4.core", "4.0.1"));

    // Assertions
    assertTrue(result.isPresent());
  }
}
