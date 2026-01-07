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

import static de.gematik.demis.packageregistry.retriever.RetrieverProperties.RegistryType.GCP_WITH_JSON_KEYFILE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import de.gematik.demis.packageregistry.retriever.FakeTokenProviderConfig;
import de.gematik.demis.packageregistry.retriever.RetrieverProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@ActiveProfiles("test-configuration")
@SpringBootTest
@Import(FakeTokenProviderConfig.class)
@MockitoBean(types = {RegistryLoadManager.class})
class PackageRegistryConfigurationTest {

  @Autowired private RetrieverProperties retrieverProperties;
  @Autowired private InitialPackageProperties initialPackageProperties;

  @DisplayName("Check that the source registry properties are loaded correctly")
  @Test
  void sourceRegistryPropertiesLoaded() {
    assertAll(
        "Check registry properties",
        () ->
            assertThat(retrieverProperties.getSourceRegistry().type())
                .isEqualTo(GCP_WITH_JSON_KEYFILE),
        () ->
            assertThat(retrieverProperties.getSourceRegistry().url())
                .isEqualTo("https://europe-west3-npm.pkg.dev/gematik-demis-prod/fhir-packages"),
        () ->
            assertThat(retrieverProperties.getGcp().serviceAccountKeyfilePath())
                .isEqualTo("fake/path/to/key.json"));
  }

  @DisplayName("Check that the initial package properties are loaded correctly")
  @Test
  void targetPackagesLoaded() {
    var packages = initialPackageProperties.getPackages();
    var firstPackage = packages.getFirst();
    var secondPackage = packages.get(1);

    assertAll(
        "Check packages",
        () -> assertThat(packages).hasSize(2),
        () -> assertThat(firstPackage.name()).isEqualTo("rki.demis.notification-api"),
        () -> assertThat(firstPackage.versions()).containsExactly("5.3.1-beta.18", "5.3.1-beta.24"),
        () -> assertThat(secondPackage.versions()).containsExactly("1.2.0-beta.15"));
  }
}
