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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GoogleAccessTokenProviderTest {

  @Mock RetrieverProperties properties;

  @Mock GoogleCredentials mockCredentials;

  @Mock AccessToken mockAccessToken;

  private GoogleAccessTokenProvider underTest;

  @BeforeEach
  void setUp(@TempDir Path tempDir) throws Exception {
    Path dummyFile = Files.writeString(tempDir.resolve("dummy.json"), "irrelevant");
    RetrieverProperties.Gcp mockGcp = new RetrieverProperties.Gcp(dummyFile.toString());
    when(properties.getGcp()).thenReturn(mockGcp);
  }

  @DisplayName("init and getToken should work correctly when no IOExceptions occur")
  @Test
  @SneakyThrows
  void init_and_getToken_shouldWork() {

    try (MockedStatic<GoogleCredentials> mocked = Mockito.mockStatic(GoogleCredentials.class)) {
      // Preparation
      mocked
          .when(() -> GoogleCredentials.fromStream(any(InputStream.class)))
          .thenReturn(mockCredentials);

      when(mockCredentials.createScoped(anyList())).thenReturn(mockCredentials);
      when(mockCredentials.getAccessToken()).thenReturn(mockAccessToken);
      when(mockAccessToken.getTokenValue()).thenReturn("fake-token");

      underTest = new GoogleAccessTokenProvider(properties);

      // Execution
      underTest.init();
      String token = underTest.getToken();

      // Assertion
      verify(mockCredentials).refreshIfExpired();
      assertEquals("fake-token", token);
    }
  }

  @DisplayName("init should throw PackageRetrieverException when IOException occurs")
  @Test
  void init_throws() {

    try (MockedStatic<GoogleCredentials> mocked = Mockito.mockStatic(GoogleCredentials.class)) {
      // Preparation
      mocked
          .when(() -> GoogleCredentials.fromStream(any(InputStream.class)))
          .thenThrow(IOException.class);
      underTest = new GoogleAccessTokenProvider(properties);

      // Execution and assertion
      var ex = assertThrows(PackageRetrieverException.class, () -> underTest.init());
      assertEquals("Failed to load Google credentials from keyfile", ex.getMessage());
    }
  }

  @DisplayName("getToken should throw PackageRetrieverException when IOException occurs")
  @Test
  @SneakyThrows
  void getToken_throws() {

    try (MockedStatic<GoogleCredentials> mocked = Mockito.mockStatic(GoogleCredentials.class)) {
      // Preparation
      mocked
          .when(() -> GoogleCredentials.fromStream(any(InputStream.class)))
          .thenReturn(mockCredentials);
      when(mockCredentials.createScoped(anyList())).thenReturn(mockCredentials);
      doThrow(new IOException("Network error")).when(mockCredentials).refreshIfExpired();

      underTest = new GoogleAccessTokenProvider(properties);
      underTest.init();

      // Execution and assertion
      var ex = assertThrows(PackageRetrieverException.class, () -> underTest.getToken());
      assertEquals("Failed to refresh Google credentials", ex.getMessage());
    }
  }
}
