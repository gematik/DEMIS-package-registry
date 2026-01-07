package de.gematik.demis.packageregistry.common;

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

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

class SpringRestClientTest {

  private WireMockServer wireMockServer;
  private SpringRestClient springRestClient;

  @BeforeEach
  void setup() {
    wireMockServer = new WireMockServer(0); // dynamic port
    wireMockServer.start();
    configureFor("localhost", wireMockServer.port());

    springRestClient = new SpringRestClient();
  }

  @AfterEach
  void tearDown() {
    wireMockServer.stop();
  }

  @Test
  void get_returnsEmptyOptional_when404() {
    // Arrange
    String token = "dummy-token";
    String url = "http://localhost:" + wireMockServer.port() + "/foo/bar";

    stubFor(
        get(urlEqualTo("/foo/bar"))
            .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer " + token))
            .willReturn(aResponse().withStatus(404)));

    // Act
    Optional<byte[]> result = springRestClient.getBytes(url, token);

    // Assert
    assertTrue(result.isEmpty(), "Should return empty Optional when resource not found (404)");
  }
}
