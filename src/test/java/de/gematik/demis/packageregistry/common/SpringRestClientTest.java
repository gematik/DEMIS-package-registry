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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClientResponseException;

class SpringRestClientTest {

  private static final String TOKEN = "dummy-token";
  private static final String PATH = "/foo/bar";
  private WireMockServer wireMockServer;
  private SpringRestClient springRestClient;
  private String url;

  @BeforeEach
  void setup() {
    wireMockServer = new WireMockServer(0); // dynamic port
    wireMockServer.start();
    configureFor("localhost", wireMockServer.port());

    url = "http://localhost:" + wireMockServer.port() + PATH;

    springRestClient = new SpringRestClient();
  }

  @AfterEach
  void tearDown() {
    wireMockServer.stop();
  }

  @Test
  void getReturnsEmptyOptionalWhen404() {
    stubFor(
        get(urlEqualTo(PATH))
            .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer " + TOKEN))
            .willReturn(aResponse().withStatus(HttpStatus.NOT_FOUND.value())));

    final Optional<byte[]> result = springRestClient.getBytes(url, TOKEN);

    assertTrue(result.isEmpty(), "Should return empty Optional when resource not found (404)");
  }

  @Test
  void getThrowsExceptionWhenServerError() {
    stubFor(
        get(urlEqualTo(PATH))
            .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer " + TOKEN))
            .willReturn(aResponse().withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())));
    assertThrows(RestClientResponseException.class, () -> springRestClient.getBytes(url, TOKEN));
  }
}
