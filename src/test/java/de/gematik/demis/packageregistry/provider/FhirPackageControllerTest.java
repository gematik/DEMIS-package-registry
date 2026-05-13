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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.gematik.demis.packageregistry.core.PackageNotFoundException;
import de.gematik.demis.packageregistry.core.RegistryQueryService;
import de.gematik.demis.packageregistry.domain.FhirPackage;
import de.gematik.demis.packageregistry.domain.PackageKey;
import de.gematik.demis.packageregistry.retriever.verification.SupplyChainVerificationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@ExtendWith(MockitoExtension.class)
class FhirPackageControllerTest {

  private static final String TEST_NAME = "test";
  private static final String TEST_VERSION = "1.0.0";
  private static final byte[] TEST_BYTES = {1, 2, 3};

  private RegistryQueryService registry;
  private FhirPackageController controller;
  ResponseEntity<byte[]> response;

  @BeforeEach
  void setUp() {
    registry = mock(RegistryQueryService.class);
    controller = new FhirPackageController(registry, null);
    RequestContextHolder.setRequestAttributes(
        new ServletRequestAttributes(new MockHttpServletRequest()));
  }

  @Test
  void returnsPackageBytesAndAttachmentHeaterIfPackageExists() {
    FhirPackage pkg = mockFhirPackage();
    when(registry.getPackageAsync(any())).thenReturn(CompletableFuture.completedFuture(pkg));

    response = controller.getPackage(TEST_NAME, TEST_VERSION);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).containsExactly(TEST_BYTES);
    assertThat(response.getHeaders().getFirst("Content-Disposition"))
        .contains(TEST_NAME + "-" + TEST_VERSION + ".tgz");
  }

  @Test
  void returns404ifPackageNotFound() {
    when(registry.getPackageAsync(any(PackageKey.class)))
        .thenReturn(CompletableFuture.completedFuture(null));

    response = controller.getPackage(TEST_NAME, TEST_VERSION);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void returns500IfSerializationFails() throws RuntimeException {
    FhirPackage pkg = mock(FhirPackage.class);
    when(pkg.toBytes()).thenThrow(new RuntimeException("fail"));
    when(registry.getPackageAsync(any(PackageKey.class)))
        .thenReturn(CompletableFuture.completedFuture(pkg));

    response = controller.getPackage(TEST_NAME, TEST_VERSION);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @Test
  void returns400IfIllegalArgumentException() {
    when(registry.getPackageAsync(any(PackageKey.class)))
        .thenReturn(CompletableFuture.failedFuture(new IllegalArgumentException()));

    response = controller.getPackage(TEST_NAME, TEST_VERSION);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @Test
  void returns404IfPackageNotFoundException() {
    when(registry.getPackageAsync(any(PackageKey.class)))
        .thenReturn(CompletableFuture.failedFuture(new PackageNotFoundException("not found")));

    response = controller.getPackage(TEST_NAME, TEST_VERSION);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void returns400IfSupplyChainVerificationException() {
    when(registry.getPackageAsync(any(PackageKey.class)))
        .thenReturn(CompletableFuture.failedFuture(new SupplyChainVerificationException("fail")));

    response = controller.getPackage(TEST_NAME, TEST_VERSION);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
  }

  @Test
  void returns504IfTimeoutException() {
    when(registry.getPackageAsync(any(PackageKey.class)))
        .thenReturn(CompletableFuture.failedFuture(new TimeoutException()));

    response = controller.getPackage(TEST_NAME, TEST_VERSION);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT);
  }

  @Test
  void returns500IfUnexpectedException() {
    when(registry.getPackageAsync(any(PackageKey.class)))
        .thenReturn(CompletableFuture.failedFuture(new RuntimeException("unexpected")));

    response = controller.getPackage(TEST_NAME, TEST_VERSION);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
  }

  private static FhirPackage mockFhirPackage() {
    FhirPackage pkg = mock(FhirPackage.class);
    when(pkg.getName()).thenReturn(FhirPackageControllerTest.TEST_NAME);
    when(pkg.getVersion()).thenReturn(FhirPackageControllerTest.TEST_VERSION);
    when(pkg.toBytes()).thenReturn(FhirPackageControllerTest.TEST_BYTES);
    return pkg;
  }
}
