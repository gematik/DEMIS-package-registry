package de.gematik.demis.packageregistry.retriever;

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
import static org.mockito.Mockito.*;

import de.gematik.demis.packageregistry.common.FhirPackageOverviewDto;
import de.gematik.demis.packageregistry.common.SpringRestClient;
import de.gematik.demis.packageregistry.domain.PackageKey;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GenericPublicPackageRetrieverAdapterTest {

  private SpringRestClient restClient;
  private GenericPublicPackageRetrieverAdapter adapter;

  @BeforeEach
  void setUp() {
    restClient = mock(SpringRestClient.class);
    final RetrieverProperties retrieverProperties = mock(RetrieverProperties.class);
    adapter = spy(new GenericPublicPackageRetrieverAdapter(retrieverProperties, restClient));
  }

  @Test
  void returnPackageBytesWhenTarballUrlExists() {
    PackageKey key = new PackageKey("foo", "1.0.0");
    String tarballUrl = "http://example.com/foo-1.0.0.tgz";
    byte[] expectedBytes = {1, 2, 3};

    doReturn(Optional.of(getFhirPackageOverviewDto(tarballUrl)))
        .when(adapter)
        .getPackageOverview("foo");
    when(restClient.getBytes(tarballUrl, null)).thenReturn(Optional.of(expectedBytes));

    final Optional<byte[]> result = adapter.retrievePackageAsTgzBinary(key);

    assertThat(result).contains(expectedBytes);
  }

  @Test
  void returnsEmptyWhenNoTarballUrlFound() {
    doReturn(Optional.empty()).when(adapter).getPackageOverview("foo");
    assertThat(adapter.retrievePackageAsTgzBinary(new PackageKey("foo", "1.0.0"))).isEmpty();
  }

  private static FhirPackageOverviewDto getFhirPackageOverviewDto(String tarballUrl) {
    final FhirPackageOverviewDto.FhirPackageVersion version =
        new FhirPackageOverviewDto.FhirPackageVersion(
            "foo", "1.0.0", new FhirPackageOverviewDto.Dist(tarballUrl), null);
    return FhirPackageOverviewDto.builder()
        .id("foo")
        .name("foo")
        .distTags(Map.of())
        .versions(Map.of("1.0.0", version))
        .build();
  }
}
