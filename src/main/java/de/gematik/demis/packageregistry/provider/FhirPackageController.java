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

import de.gematik.demis.packageregistry.common.FhirPackageOverviewDto;
import de.gematik.demis.packageregistry.core.PackageNotFoundException;
import de.gematik.demis.packageregistry.core.RegistryQueryService;
import de.gematik.demis.packageregistry.domain.FhirPackage;
import de.gematik.demis.packageregistry.domain.PackageKey;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletionException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriUtils;

@RestController
@AllArgsConstructor
@Slf4j
@Tag(name = "FHIR Packages", description = "API for retrieving FHIR packages from the registry")
@RequestMapping("/packages")
class FhirPackageController {

  private static final String FHIR_NPM_PACKAGE = "application/fhir+npmpackage";
  private static final MediaType FHIR_NPM_MEDIA_TYPE = MediaType.parseMediaType(FHIR_NPM_PACKAGE);

  private final RegistryQueryService registryQueryService;
  private final FhirPackageMapper fhirPackageMapper;

  @Operation(
      summary = "Get a specific FHIR package",
      description = "Returns the binary content of a specific FHIR package by name and version")
  @GetMapping(value = "/{packageName}/{packageVersion}", produces = FHIR_NPM_PACKAGE)
  public ResponseEntity<byte[]> getPackage(
      @PathVariable String packageName, @PathVariable String packageVersion) {

    log.info("Processing request for package {}@{}", packageName, packageVersion);
    var requestUri = ServletUriComponentsBuilder.fromCurrentRequestUri().build().getPath();

    try {
      var pkg =
          registryQueryService
              .getPackageAsync(new PackageKey(packageName, packageVersion))
              .join(); // blocking is fine here bec we use virtual threads
      return ResponseEntity.ok()
          .header(
              HttpHeaders.CONTENT_DISPOSITION,
              "attachment; filename=\"" + buildPackageFilename(pkg) + "\"")
          .contentType(FHIR_NPM_MEDIA_TYPE)
          .body(pkg.toBytes());
    } catch (Exception ex) {
      Throwable cause = ex instanceof CompletionException ? ex.getCause() : ex;
      log.warn("Request GET {} failed", requestUri, cause);
      if (cause instanceof PackageNotFoundException) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
      }
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  @Operation(
      summary = "Get an overview over all versions of a FHIR package",
      description =
          "Returns metadata about all available versions of a specific FHIR package by name")
  @GetMapping(value = "/{packageName}", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<FhirPackageOverviewDto> getPackageOverview(
      @PathVariable String packageName) {

    return registryQueryService
        .getPackageVersionAggregate(packageName)
        .map(aggregate -> fhirPackageMapper.toDto(aggregate, getBaseUrl()))
        .map(
            dto ->
                ResponseEntity.ok()
                    .header(
                        "Content-Disposition",
                        "inline; filename=\"" + packageName + "-overview.json\"")
                    .body(dto))
        .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
  }

  private String buildPackageFilename(FhirPackage pkg) {
    String filename = String.format("%s-%s.tgz", pkg.getName(), pkg.getVersion());
    return UriUtils.encode(filename, StandardCharsets.UTF_8);
  }

  private String getBaseUrl() {
    return ServletUriComponentsBuilder.fromCurrentRequestUri().path("/").toUriString();
  }
}
