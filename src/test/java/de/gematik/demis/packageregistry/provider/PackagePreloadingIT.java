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

import static de.gematik.demis.packageregistry.TestHelper.disableSupplyChainValidationOnWindows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.gematik.demis.packageregistry.core.InitialPackageProperties;
import de.gematik.demis.packageregistry.core.PackageRetrieverPort;
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
 * Integration test for preloading FHIR packages at application startup.
 *
 * <p>Normally, FHIR packages are retrieved on demand. However, the application can be configured to
 * preload certain packages at startup via the application properties. This test verifies that the
 * preloading mechanism works correctly.
 */
@ActiveProfiles("test-preloading")
@SpringBootTest
@AutoConfigureMockMvc
@Import(FakeTokenProviderConfig.class)
class PackagePreloadingIT {

  private static final String PATH_DUMMY_PACKAGE =
      "src/test/resources/dummy-package/dummy_package.tgz";
  private static final String PATH_DUMMY_PACKAGE_SIGNATURE =
      "src/test/resources/dummy-package/dummy_package_sig.bundle.json";
  private static final String PATH_DUMMY_PACKAGE_ATTESTATION =
      "src/test/resources/dummy-package/dummy_package_att.stage.dev.bundle.json";
  private static final EnhancedWireMock MOCK_SERVER = EnhancedWireMock.createWithDynamicPort(true);

  @Autowired private MockMvc mockMvc;
  @MockitoSpyBean private InitialPackageProperties initialPackageProperties;
  @MockitoSpyBean private PackageRetrieverPort retrieverPort;
  @MockitoSpyBean private PackageStoragePort storagePort;

  @DynamicPropertySource
  static void registerWireMockProperties(DynamicPropertyRegistry registry) {
    MOCK_SERVER.startMockAndRegisterProperties(registry);
  }

  @BeforeAll
  @SneakyThrows
  static void init() {
    disableSupplyChainValidationOnWindows();
    runWireMock();
  }

  // We use WireMock to mock the Google Package Registry, which is used to retrieve the FHIR
  // packages and the artefacts for supply chain verification. Since this process happens at
  // application startup, we need to start WireMock before loading the Spring ApplicationContext.
  private static void runWireMock() {
    // Assume packageABC@1.2.3 is declared as initial package in properties
    // Stub get package request
    MOCK_SERVER.onCallReturnsBytes("/packageABC/-/packageABC-1.2.3.tgz", PATH_DUMMY_PACKAGE);
    // Stub get signature request
    MOCK_SERVER.onCallReturnsBytes(
        "/files/packageABC:1.2.3:sig.bundle.json:download?alt=media", PATH_DUMMY_PACKAGE_SIGNATURE);
    // Stub get attestation request
    MOCK_SERVER.onCallReturnsBytes(
        "/files/packageABC:1.2.3:att.stage.dev.bundle.json:download?alt=media",
        PATH_DUMMY_PACKAGE_ATTESTATION);
  }

  @DisplayName(
      "FHIR packages marked for pre-load in the properties are retrieved on application start and available in registry")
  @SneakyThrows
  @Test
  void getPackage_deliveryOfPreloadedPackages() {

    // Preparation
    var preloadedPkgName = initialPackageProperties.getPackages().getFirst().name();
    var preloadedPkgVersion =
        initialPackageProperties.getPackages().getFirst().versions().getFirst();

    // Execution
    var result = storagePort.getPackage(new PackageKey(preloadedPkgName, preloadedPkgVersion));

    // Assertions
    assertTrue(result.isPresent());
  }
}
