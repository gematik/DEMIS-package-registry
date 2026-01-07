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
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import de.gematik.demis.packageregistry.TestHelper;
import de.gematik.demis.packageregistry.domain.PackageKey;
import de.gematik.demis.packageregistry.retriever.RetrieverProperties;
import de.gematik.demis.packageregistry.retriever.verification.SupplyChainVerifierPort;
import java.nio.file.Files;
import java.nio.file.Path;
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
 * Acceptance tests for the Package Registry application using a public Google Artifact Registry as
 * source registry.
 */
@ActiveProfiles("test-integration-public-gar")
@SpringBootTest
@AutoConfigureMockMvc
class AcceptanceWithPublicGoogleRegistryIT {

  private static final EnhancedWireMock MOCK_SERVER = EnhancedWireMock.createWithDynamicPort(false);
  private static final String DUMMY_PACKAGE = "src/test/resources/dummy-package/dummy_package.tgz";
  private static final String DUMMY_PACKAGE_SIGNATURE =
      "src/test/resources/dummy-package/dummy_package_sig.bundle.json";

  @Autowired private MockMvc mockMvc;
  @MockitoSpyBean private RetrieverProperties retrieverProperties;
  @MockitoSpyBean private SupplyChainVerifierPort verifierPort;

  @DynamicPropertySource
  static void registerWireMockProperties(DynamicPropertyRegistry registry) {
    MOCK_SERVER.startMockAndRegisterProperties(registry);
  }

  @BeforeAll
  @SneakyThrows
  static void init() {
    disableSupplyChainValidationOnWindows();
  }

  @AfterAll
  static void stopWiremock() {
    MOCK_SERVER.stop();
  }

  @DisplayName(
      "Package registry delivers on demand a package retrieved from a public Google Artifact Registry")
  @SneakyThrows
  @Test
  void getPackage_packageCanBeRetrievedOnDemand() {
    // Preparation
    var pkgName = TestHelper.getRandomPkgName();
    var pkgVersion = TestHelper.getRandomPkgVersion();

    String urlMainPackage = "/" + pkgName + "/-/" + pkgName + "-" + pkgVersion + ".tgz";
    MOCK_SERVER.onCallReturnsBytes(urlMainPackage, DUMMY_PACKAGE);
    String urlSignature =
        "/files/" + pkgName + ":" + pkgVersion + ":sig.bundle.json:download?alt=media";
    MOCK_SERVER.onCallReturnsBytes(urlSignature, DUMMY_PACKAGE_SIGNATURE);

    // Execution
    final var result = mockMvc.perform(get("/packages/{name}/{version}", pkgName, pkgVersion));

    // Assertions
    byte[] expectedBytes = Files.readAllBytes(Path.of(DUMMY_PACKAGE));
    String expectedFilename = pkgName + "-" + pkgVersion + ".tgz";

    result
        .andExpect(status().is2xxSuccessful())
        .andExpect(content().bytes(expectedBytes))
        .andExpect(content().contentType("application/fhir+npmpackage"))
        .andExpect(
            header()
                .string(
                    "Content-Disposition", "attachment; filename=\"" + expectedFilename + "\""));

    PackageKey packageKey = new PackageKey(pkgName, pkgVersion);
    verify(verifierPort, never()).verifyStageAttestation(packageKey, expectedBytes);
  }
}
