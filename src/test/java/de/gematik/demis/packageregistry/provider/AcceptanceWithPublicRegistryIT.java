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

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import de.gematik.demis.packageregistry.TestHelper;
import de.gematik.demis.packageregistry.common.SpringRestClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import lombok.SneakyThrows;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Acceptance tests for the Package Registry application using a public registry as source registry.
 */
@ActiveProfiles("test-integration-public-registry")
@SpringBootTest
@AutoConfigureMockMvc
class AcceptanceWithPublicRegistryIT {

  private static final EnhancedWireMock MOCK_SERVER = EnhancedWireMock.createWithDynamicPort(false);
  private static final String DUMMY_PACKAGE = "src/test/resources/dummy-package/dummy_package.tgz";
  private static final String HL7_FHIR_US_CORE_OVERVIEW_JSON =
      "src/test/resources/package-overview.json";

  @Autowired private MockMvc mockMvc;
  @MockitoSpyBean private SpringRestClient restClient;

  @DynamicPropertySource
  static void registerWireMockProperties(DynamicPropertyRegistry registry) {
    MOCK_SERVER.startMockAndRegisterProperties(registry);
  }

  @AfterAll
  static void stopWiremock() {
    MOCK_SERVER.stop();
  }

  @DisplayName(
      "Package registry delivers on demand a package retrieved from a public source registry")
  @SneakyThrows
  @Test
  void getPackage_packageCanBeRetrievedOnDemand() {
    // Preparation
    var pkgName = "hl7.fhir.us.core";
    var pkgVersion = "2.1.0";

    String urlPackageOverview = "/" + pkgName;
    MOCK_SERVER.onCallReturnsJson(urlPackageOverview, HL7_FHIR_US_CORE_OVERVIEW_JSON);

    Path pathToDummyPackage = Path.of(DUMMY_PACKAGE);
    byte[] tgzBytes = Files.readAllBytes(pathToDummyPackage);

    when(restClient.getBytes("https://packages.simplifier.net/hl7.fhir.us.core/2.1.0", null))
        .thenReturn(Optional.of(tgzBytes));

    // Execution
    final var result = mockMvc.perform(get("/packages/{name}/{version}", pkgName, pkgVersion));

    // Assertions
    String expectedFilename = pkgName + "-" + pkgVersion + ".tgz";

    result
        .andExpect(status().is2xxSuccessful())
        .andExpect(content().bytes(tgzBytes))
        .andExpect(content().contentType("application/fhir+npmpackage"))
        .andExpect(
            header()
                .string(
                    "Content-Disposition", "attachment; filename=\"" + expectedFilename + "\""));
  }

  @DisplayName("Not found is returned when package is unknown in public source registry")
  @SneakyThrows
  @Test
  void getPackage_packageUnknown() {
    // Preparation
    var pkgName = TestHelper.getRandomPkgName();
    var pkgVersion = TestHelper.getRandomPkgVersion();
    //  We don't instruct WireMock to return anything for this package, so the overview request will
    // return 404

    // Execution
    final var result = mockMvc.perform(get("/packages/{name}/{version}", pkgName, pkgVersion));

    // Assertions
    result.andExpect(status().isNotFound());
  }

  @DisplayName("Not found is returned when package version is unknown in public source registry")
  @SneakyThrows
  @Test
  void getPackage_versionUnknown() {
    // Preparation
    var pkgName = "hl7.fhir.us.core";
    var pkgVersion = "x.y.z"; // non-existing version

    String urlPackageOverview = "/" + pkgName;
    MOCK_SERVER.onCallReturnsJson(urlPackageOverview, HL7_FHIR_US_CORE_OVERVIEW_JSON);

    // Execution
    final var result = mockMvc.perform(get("/packages/{name}/{version}", pkgName, pkgVersion));

    // Assertions
    result.andExpect(status().isNotFound());
  }
}
