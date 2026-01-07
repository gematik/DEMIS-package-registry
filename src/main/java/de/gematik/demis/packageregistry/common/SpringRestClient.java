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

import java.net.URI;
import java.net.http.HttpClient;
import java.util.Optional;
import org.springframework.http.*;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class SpringRestClient {

  private final RestClient restClient = initializeRestClient();

  /**
   * Explicitly use JDK HttpClient because, unlike RestTemplate, the new RestClient does not follow
   * redirects by default.
   *
   * @return a RestClient configured with redirect support
   */
  private RestClient initializeRestClient() {
    HttpClient httpClient =
        HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
    return RestClient.builder().requestFactory(new JdkClientHttpRequestFactory(httpClient)).build();
  }

  /**
   * Fetches the raw bytes from the specified URL with optional Bearer token authentication.
   *
   * @param url the URL to fetch bytes from
   * @param bearerToken null if no auth is needed
   */
  public Optional<byte[]> getBytes(String url, String bearerToken) {
    return executeRequest(url, bearerToken, MediaType.APPLICATION_OCTET_STREAM, byte[].class);
  }

  /**
   * Fetches JSON from the specified URL with optional Bearer token authentication.
   *
   * @param url the URL to fetch JSON from
   * @param responseType the class type to deserialize the JSON into
   * @param bearerToken null if no auth is needed
   */
  public <T> Optional<T> getJson(String url, Class<T> responseType, String bearerToken) {
    return executeRequest(url, bearerToken, MediaType.APPLICATION_JSON, responseType);
  }

  private <T> Optional<T> executeRequest(
      String url, String bearerToken, MediaType mediaType, Class<T> responseType) {
    try {
      var requestSpec = restClient.get().uri(URI.create(url)).accept(mediaType);
      if (bearerToken != null) {
        requestSpec = requestSpec.header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken);
      }
      var result = requestSpec.retrieve().body(responseType);
      return Optional.ofNullable(result);
    } catch (RestClientResponseException e) {
      if (e.getStatusCode().value() == 404) {
        return Optional.empty();
      }
      throw e;
    }
  }
}
