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
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import de.gematik.demis.packageregistry.TestHelper;
import de.gematik.demis.packageregistry.core.PackageRetrieverPort;
import de.gematik.demis.packageregistry.domain.PackageKey;
import de.gematik.demis.packageregistry.retriever.FakeTokenProviderConfig;
import de.gematik.demis.packageregistry.retriever.RetrieverProperties;
import de.gematik.demis.packageregistry.retriever.verification.SupplyChainVerifierPort;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;
import lombok.SneakyThrows;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Acceptance tests for the Package Registry application using a private Google Artifact Registry as
 * source registry.
 */
@ActiveProfiles("test-integration-private-gar")
@SpringBootTest
@AutoConfigureMockMvc
@Import(FakeTokenProviderConfig.class)
class AcceptanceWithPrivateGoogleRegistryIT {

  private static final EnhancedWireMock MOCK_SERVER = EnhancedWireMock.createWithDynamicPort(true);
  private static final String DUMMY_PACKAGE = "src/test/resources/dummy-package/dummy_package.tgz";
  private static final String DUMMY_PACKAGE_SIGNATURE =
      "src/test/resources/dummy-package/dummy_package_sig.bundle.json";
  private static final String DUMMY_PACKAGE_ATTESTATION =
      "src/test/resources/dummy-package/dummy_package_att.stage.dev.bundle.json";
  private static final String PACKAGE_WITHOUT_DEPENDENCIES =
      "src/test/resources/buggy-packages/no-dependencies.tgz";
  private static final String PACKAGE_WITH_DEPENDENCY_FOO_1_2_3 =
      "src/test/resources/packages-for-dependency-chain/with-dependency_foo@1_2_3.tgz";
  private static final String PACKAGE_WITH_DEPENDENCY_BAR_1_0_X =
      "src/test/resources/packages-for-dependency-chain/with-dependency_bar@1_0_x.tgz";
  private static final String PACKAGE_WITH_3_DEPENDENCIES =
      "src/test/resources/packages-for-dependency-chain/with-3-dependencies.tgz";
  private static final String ROOT_PACKAGE_JSON_BAR_WITH_MATCH_FOR_1_0_X =
      "src/test/resources/packages-for-dependency-chain/packageRoot_bar_withVersions_1_0.json";
  private static final String ROOT_PACKAGE_JSON_BAR_WITH_NO_MATCH_FOR_1_0_X =
      "src/test/resources/packages-for-dependency-chain/packageRoot_bar_withoutVersions_1_0.json";

  @Autowired private MockMvc mockMvc;
  @MockitoSpyBean private RetrieverProperties retrieverProperties;
  @MockitoSpyBean private PackageRetrieverPort retrieverPort;
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

  @Nested
  class getPackage {

    @DisplayName(
        "If package not available in cache, registry retrieves it on the fly from the source registry")
    @SneakyThrows
    @Test
    void getPackage_packageCanBeRetrievedOnDemand() {
      // Preparation
      // In this test we only want to test on-demand package retrieval and supply chain verification
      when(retrieverProperties.isDependencyLoadingEnabled()).thenReturn(false);

      var pkgName = TestHelper.getRandomPkgName();
      var pkgVersion = TestHelper.getRandomPkgVersion();

      String urlMainPackage = "/" + pkgName + "/-/" + pkgName + "-" + pkgVersion + ".tgz";
      MOCK_SERVER.onCallReturnsBytes(urlMainPackage, DUMMY_PACKAGE);
      String urlSignature =
          "/files/" + pkgName + ":" + pkgVersion + ":sig.bundle.json:download?alt=media";
      MOCK_SERVER.onCallReturnsBytes(urlSignature, DUMMY_PACKAGE_SIGNATURE);
      String urlAttestation =
          "/files/" + pkgName + ":" + pkgVersion + ":att.stage.dev.bundle.json:download?alt=media";
      MOCK_SERVER.onCallReturnsBytes(urlAttestation, DUMMY_PACKAGE_ATTESTATION);

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
    }

    @DisplayName(
        "When retrieving a package on demand, its dependencies are also retrieved and wildcard versions are resolved")
    @SneakyThrows
    @Test
    void getPackage_packageCanBeRetrievedOnDemandWithDependencies() {
      // Preparation
      // We disable supply chain verification for this test to avoid the need to stub, focus is on
      // dependency loading
      doNothing().when(verifierPort).verifySignature(any(), any());
      doNothing().when(verifierPort).verifyStageAttestation(any(), any());

      var pkgName = TestHelper.getRandomPkgName();
      var pkgVersion = TestHelper.getRandomPkgVersion();
      String urlMainPackage = "/" + pkgName + "/-/" + pkgName + "-" + pkgVersion + ".tgz";

      MOCK_SERVER.onCallReturnsBytes(urlMainPackage, PACKAGE_WITH_DEPENDENCY_FOO_1_2_3);
      MOCK_SERVER.onCallReturnsBytes("/foo/-/foo-1.2.3.tgz", PACKAGE_WITH_DEPENDENCY_BAR_1_0_X);
      MOCK_SERVER.onCallReturnsJson("/bar", ROOT_PACKAGE_JSON_BAR_WITH_MATCH_FOR_1_0_X);
      MOCK_SERVER.onCallReturnsBytes("/bar/-/bar-1.0.15.tgz", PACKAGE_WITHOUT_DEPENDENCIES);

      // Execution: retrieve main package
      final var getPackage =
          mockMvc.perform(get(String.format("/packages/%s/%s", pkgName, pkgVersion)));

      // Assertions
      getPackage.andExpect(status().is2xxSuccessful());

      // Dependencies should now be available in the registry
      final var getDependency = mockMvc.perform(get("/packages/{name}/{version}", "bar", "1.0.15"));
      getDependency.andExpect(status().is2xxSuccessful());

      // TThe dependency should be retrieved only once
      Mockito.verify(retrieverPort, Mockito.times(1)) // called only once
          .retrievePackageAsTgzBinary(
              Mockito.argThat(pk -> "bar".equals(pk.name()) && "1.0.15".equals(pk.version())));
    }

    @DisplayName(
        "Concurrent requests for a non available package: the registry retrieves it from the source registry only the first time and keep the others threads waiting")
    @SneakyThrows
    @Test
    void getPackage_happyHandlingOfThreadConcurrency() {
      // Preparation
      // We disable supply chain verification for this test to avoid the need to stub
      // signature/attestation
      doNothing().when(verifierPort).verifySignature(any(), any());
      doNothing().when(verifierPort).verifyStageAttestation(any(), any());
      when(retrieverProperties.isDependencyLoadingEnabled()).thenReturn(false);

      var pkgName = TestHelper.getRandomPkgName();
      var pkgVersion = TestHelper.getRandomPkgVersion();

      MOCK_SERVER.onCallReturnsBytesWithDelay(
          "/" + pkgName + "/-/" + pkgName + "-" + pkgVersion + ".tgz", DUMMY_PACKAGE, 2000);

      // Create multiple CompletableFutures for concurrent calls
      int numThreads = 20;
      List<CompletableFuture<ResultActions>> futures =
          IntStream.range(0, numThreads)
              .mapToObj(
                  i ->
                      CompletableFuture.supplyAsync(
                          () -> {
                            try {
                              return mockMvc.perform(
                                  get("/packages/{name}/{version}", pkgName, pkgVersion));
                            } catch (Exception e) {
                              throw new RuntimeException(e);
                            }
                          }))
              .toList();

      // Wait for all tasks to complete
      CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

      // Assertions
      futures.forEach(
          future -> {
            try {
              future.get().andExpect(status().is2xxSuccessful());
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
          });

      Mockito.verify(retrieverPort, times(1))
          .retrievePackageAsTgzBinary(new PackageKey(pkgName, pkgVersion));
    }

    @DisplayName(
        "If the package requested by client is available neither in the cache nor in the source registry a 404 is returned")
    @SneakyThrows
    @Test
    void getPackage_packageNotFound() {
      var pkgName = TestHelper.getRandomPkgName();
      var pkgVersion = TestHelper.getRandomPkgVersion();
      var result = mockMvc.perform(get("/packages/{name}/{version}", pkgName, pkgVersion));

      result.andExpect(status().isNotFound());
    }

    @DisplayName(
        "If one dependency somewhere in the chain is not found, then the whole retrieval fails immediately and a 404 is returned")
    @SneakyThrows
    @Test
    void getPackage_dependencyNotFound() {
      // Preparation

      // We disable supply chain verification for this test to avoid the need to stub, focus is on
      // dependency loading
      doNothing().when(verifierPort).verifySignature(any(), any());
      doNothing().when(verifierPort).verifyStageAttestation(any(), any());

      var pkgName = TestHelper.getRandomPkgName();
      var pkgVersion = TestHelper.getRandomPkgVersion();
      String urlMainPackage = "/" + pkgName + "/-/" + pkgName + "-" + pkgVersion + ".tgz";
      MOCK_SERVER.onCallReturnsBytes(urlMainPackage, PACKAGE_WITH_3_DEPENDENCIES);

      // PACKAGE_WITH_3_DEPENDENCIES has 3 dependencies: tof@7.7.7, taf@8.8.8 and tif@9.9.9
      MOCK_SERVER.onCallReturnsBytesWithDelay(
          "/tof/-/tof-7.7.7.tgz", PACKAGE_WITHOUT_DEPENDENCIES, 2000);
      MOCK_SERVER.onCallReturnsBytesWithDelay(
          "/taf/-/taf-8.8.8.tgz", PACKAGE_WITHOUT_DEPENDENCIES, 2000);
      // --> No stub for tif@9.9.9, so it should produce a 404

      // Execution and assertion with timing
      long start = System.currentTimeMillis();
      final var getPackage =
          mockMvc.perform(get("/packages/{name}/{version}", pkgName, pkgVersion));

      getPackage.andExpect(status().isNotFound());

      long duration = System.currentTimeMillis() - start;

      assertTrue(
          duration < 1000,
          "Request fails as soon as one dependency is not found and doesnt wait for all dependencies to be processed.");

      // Wait for the delayed dependencies to complete loading
      Thread.sleep(3000);

      // Verify that successfully downloaded dependencies are available in the registry even if the
      // main package-retrieval failed
      var getTofDependency = mockMvc.perform(get("/packages/tof/7.7.7"));
      getTofDependency.andExpect(status().is2xxSuccessful());
      verify(retrieverPort, times(1)).retrievePackageAsTgzBinary(new PackageKey("tof", "7.7.7"));

      var getTafDependency = mockMvc.perform(get("/packages/taf/8.8.8"));
      getTafDependency.andExpect(status().is2xxSuccessful());
      verify(retrieverPort, times(1)).retrievePackageAsTgzBinary(new PackageKey("taf", "8.8.8"));
    }

    @DisplayName(
        "If one dependency can not be resolved, the whole retrieval fails and a 404 is returned")
    @SneakyThrows
    @Test
    void getPackage_dependencyCannotBeResolved() {
      // Preparation

      // We disable supply chain verification for this test to avoid the need to stub, focus is on
      // dependency loading
      doNothing().when(verifierPort).verifySignature(any(), any());
      doNothing().when(verifierPort).verifyStageAttestation(any(), any());

      var pkgName = TestHelper.getRandomPkgName();
      var pkgVersion = TestHelper.getRandomPkgVersion();
      String urlMainPackage = "/" + pkgName + "/-/" + pkgName + "-" + pkgVersion + ".tgz";

      MOCK_SERVER.onCallReturnsBytes(urlMainPackage, PACKAGE_WITH_DEPENDENCY_BAR_1_0_X);
      MOCK_SERVER.onCallReturnsJson("/bar", ROOT_PACKAGE_JSON_BAR_WITH_NO_MATCH_FOR_1_0_X);

      // Execution
      final var getPackage =
          mockMvc.perform(get("/packages/{name}/{version}", pkgName, pkgVersion));

      // Assertions
      getPackage.andExpect(status().isNotFound());
    }
  }

  @DirtiesContext
  @Nested
  class getPackageOverview {

    @DisplayName(
        "Package overview is successfully delivered to the client on request, dependencies included")
    @Test
    void getPackageOverview_returnsCorrectDto_withDependencies() throws Exception {
      // Preparation
      doNothing().when(verifierPort).verifySignature(any(), any());
      doNothing().when(verifierPort).verifyStageAttestation(any(), any());

      var pkg = new PackageKey(TestHelper.getRandomPkgName(), TestHelper.getRandomPkgVersion());

      String pkgUrl = String.format("/%s/-/%s-%s.tgz", pkg.name(), pkg.name(), pkg.version());
      MOCK_SERVER.onCallReturnsBytes(pkgUrl, DUMMY_PACKAGE);

      // Stub dependencies of Dummy Package
      MOCK_SERVER.onCallReturnsBytes(
          "/hl7.fhir.r4.core/-/hl7.fhir.r4.core-4.0.1.tgz", PACKAGE_WITHOUT_DEPENDENCIES);
      MOCK_SERVER.onCallReturnsBytes(
          "/de.basisprofil.r4/-/de.basisprofil.r4-1.5.3.tgz", PACKAGE_WITHOUT_DEPENDENCIES);

      // Trigger retrieval of the package
      loadPackage(pkg);

      // Execution
      var result =
          mockMvc.perform(
              get("/packages/{packageName}", pkg.name()).accept(MediaType.APPLICATION_JSON));

      // Assertions
      result
          .andExpect(status().is2xxSuccessful())
          .andExpect(jsonPath("$._id").value(pkg.name()))
          .andExpect(jsonPath("$.name").value(pkg.name()))
          .andExpect(
              jsonPath(String.format("$.versions.['%s'].name", pkg.version())).value(pkg.name()))
          .andExpect(
              jsonPath(String.format("$.versions.['%s'].version", pkg.version()))
                  .value(pkg.version()))
          .andExpect(
              jsonPath(
                      String.format(
                          "$.versions.['%s'].dependencies.['hl7.fhir.r4.core']", pkg.version()))
                  .value("4.0.1"))
          .andExpect(
              jsonPath(
                      String.format(
                          "$.versions.['%s'].dependencies.['de.basisprofil.r4']", pkg.version()))
                  .value("1.5.3"))
          .andExpect(jsonPath("$.['dist-tags'].latest").value(pkg.version()))
          .andExpect(
              jsonPath(String.format("$.versions.['%s'].dist.tarball", pkg.version()))
                  .value(
                      String.format("http://localhost/packages/%s/%s", pkg.name(), pkg.version())));
    }

    @DisplayName("Package overview has no dependency block if dependency loading is off")
    @Test
    void getPackageOverview_withoutDependencies() throws Exception {
      // Preparation
      doNothing().when(verifierPort).verifySignature(any(), any());
      doNothing().when(verifierPort).verifyStageAttestation(any(), any());

      when(retrieverProperties.isSupplyChainVerificationEnabled()).thenReturn(false);
      when(retrieverProperties.isDependencyLoadingEnabled()).thenReturn(false);

      var pkg = new PackageKey(TestHelper.getRandomPkgName(), TestHelper.getRandomPkgVersion());

      String pkgUrl = String.format("/%s/-/%s-%s.tgz", pkg.name(), pkg.name(), pkg.version());
      MOCK_SERVER.onCallReturnsBytes(pkgUrl, PACKAGE_WITH_DEPENDENCY_FOO_1_2_3);

      // Trigger retrieval of the package
      loadPackage(pkg);

      // Execution
      var result =
          mockMvc.perform(
              get("/packages/{packageName}", pkg.name()).accept(MediaType.APPLICATION_JSON));

      // Assertions
      result.andExpect(
          jsonPath(String.format("$.versions.['%s'].dependencies", pkg.version())).doesNotExist());
    }

    @DisplayName("If the package overview requested by client is not available a 404 is returned")
    @Test
    void getPackageOverview_returns404() throws Exception {
      mockMvc
          .perform(get("/packages/{packageName}", "non-existent"))
          .andExpect(status().isNotFound());
    }

    private void loadPackage(PackageKey pkg) throws Exception {
      mockMvc.perform(get("/packages/{name}/{versions}", pkg.name(), pkg.version()));
    }
  }
}
