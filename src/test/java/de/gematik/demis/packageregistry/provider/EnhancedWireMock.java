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

import static com.github.tomakehurst.wiremock.client.WireMock.*;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import de.gematik.demis.packageregistry.retriever.FakeTokenProviderConfig;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.SneakyThrows;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;

/**
 * We use WireMock in our tests to mock the source registry that provides the FHIR packages (and in
 * some cases also the signature/attestation material) to the package registry.
 */
public class EnhancedWireMock {

  private final WireMockServer mockServer;
  private final boolean requireAuthorizationHeader;

  public static EnhancedWireMock createWithDynamicPort(boolean requireAuthorizationHeader) {
    return new EnhancedWireMock(
        new WireMockServer(WireMockConfiguration.options().dynamicPort()),
        requireAuthorizationHeader);
  }

  private EnhancedWireMock(WireMockServer mockServer, boolean requireAuthorizationHeader) {
    this.mockServer = mockServer;
    this.requireAuthorizationHeader = requireAuthorizationHeader;
  }

  public void onCallReturnsBytes(String expectedRelativeUrl, String pathToFileToServe) {
    onCallMockServerReturns(
        expectedRelativeUrl, pathToFileToServe, MediaType.APPLICATION_OCTET_STREAM);
  }

  public void onCallReturnsJson(String expectedRelativeUrl, String pathToFileToServe) {
    onCallMockServerReturns(expectedRelativeUrl, pathToFileToServe, MediaType.APPLICATION_JSON);
  }

  public void onCallReturnsBytesWithDelay(
      String expectedRelativeUrl, String pathToFileToServe, int delayMs) {
    onCallMockServerReturnsWithDelay(
        expectedRelativeUrl, pathToFileToServe, MediaType.APPLICATION_OCTET_STREAM, delayMs);
  }

  public void startMockAndRegisterProperties(DynamicPropertyRegistry registry) {
    mockServer.start(); // start WireMock before Spring loads context
    int port = mockServer.port();

    registry.add("retriever.source-registry.url", () -> "http://localhost:" + port);
    registry.add(
        "retriever.supply-chain-verification.signature-attestation-base-url",
        () -> "http://localhost:" + port);
  }

  public void stop() {
    mockServer.stop();
  }

  private void onCallMockServerReturns(
      String expectedRelativeUrl, String pathToFileToServe, MediaType contentType) {
    onCallMockServerReturnsWithDelay(expectedRelativeUrl, pathToFileToServe, contentType, 0);
  }

  @SneakyThrows
  private void onCallMockServerReturnsWithDelay(
      String expectedRelativeUrl, String pathToFileToServe, MediaType contentType, int delayMs) {

    var requestBuilder =
        com.github.tomakehurst.wiremock.client.WireMock.get(urlEqualTo(expectedRelativeUrl));

    if (requireAuthorizationHeader) {
      requestBuilder =
          requestBuilder.withHeader(
              "Authorization", equalTo("Bearer " + FakeTokenProviderConfig.getFakeToken()));
    }

    byte[] bytes = Files.readAllBytes(Path.of(pathToFileToServe));

    mockServer.stubFor(
        requestBuilder.willReturn(
            aResponse()
                .withFixedDelay(delayMs)
                .withStatus(200)
                .withHeader("Content-Type", contentType.toString())
                .withBody(bytes)));
  }
}
