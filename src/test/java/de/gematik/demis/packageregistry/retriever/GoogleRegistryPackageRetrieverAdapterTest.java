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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import de.gematik.demis.packageregistry.common.SpringRestClient;
import de.gematik.demis.packageregistry.domain.PackageKey;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GoogleRegistryPackageRetrieverAdapterTest {

  private SpringRestClient restClient;
  private GoogleRegistryPackageRetrieverAdapter adapter;

  @BeforeEach
  void setUp() {
    restClient = mock(SpringRestClient.class);
    final RetrieverProperties retrieverProperties = mock(RetrieverProperties.class);
    final RetrieverProperties.SourceRegistry sourceRegistry =
        mock(RetrieverProperties.SourceRegistry.class);
    when(retrieverProperties.getSourceRegistry()).thenReturn(sourceRegistry);
    when(sourceRegistry.url()).thenReturn("https://example.com/npm");
    final GoogleAccessTokenProvider tokenProvider = mock(GoogleAccessTokenProvider.class);
    when(tokenProvider.getToken()).thenReturn("token");
    adapter =
        new GoogleRegistryPackageRetrieverAdapter(retrieverProperties, restClient, tokenProvider);
  }

  @Test
  void returnsPackageBytesIfPackageExists() {
    final byte[] expected = new byte[] {1, 2, 3};
    when(restClient.getBytes(anyString(), any())).thenReturn(Optional.of(expected));
    final PackageKey key = new PackageKey("test", "1.0.0");

    final Optional<byte[]> result = adapter.retrievePackageAsTgzBinary(key);

    assertThat(result).isPresent();
    assertThat(result.get()).containsExactly(1, 2, 3);
    verify(restClient).getBytes(contains("test-1.0.0.tgz"), eq("token"));
  }

  @Test
  void returnsEmptyIfRestClientReturnsEmpty() {
    when(restClient.getBytes(anyString(), any())).thenReturn(Optional.empty());
    final PackageKey key = new PackageKey("notfound", "1.0.0");

    Optional<byte[]> result = adapter.retrievePackageAsTgzBinary(key);

    assertThat(result).isEmpty();
  }
}
