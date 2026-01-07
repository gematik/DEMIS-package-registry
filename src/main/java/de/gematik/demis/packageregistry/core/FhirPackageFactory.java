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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.gematik.demis.packageregistry.domain.FhirPackage;
import de.gematik.demis.packageregistry.domain.PackageKey;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class FhirPackageFactory {

  public FhirPackage createFhirPackage(PackageKey packageKey, byte[] tgzContent) {
    return FhirPackage.of(
        packageKey,
        java.time.Instant.now(),
        java.nio.ByteBuffer.wrap(tgzContent).asReadOnlyBuffer(),
        readDependencies(packageKey, tgzContent));
  }

  Map<String, String> readDependencies(PackageKey packageKey, byte[] tgzContent) {
    String packageJson = extractPackageJsonFromTgz(packageKey, tgzContent);
    return parseDependenciesFromPackageJson(packageJson);
  }

  private String extractPackageJsonFromTgz(PackageKey packageKey, byte[] tgzContent) {

    try (ByteArrayInputStream bais = new ByteArrayInputStream(tgzContent);
        GZIPInputStream gzipIn = new GZIPInputStream(bais);
        TarArchiveInputStream tarIn = new TarArchiveInputStream(gzipIn)) {

      TarArchiveEntry entry;
      while ((entry = tarIn.getNextEntry()) != null) {
        if (entry.getName().endsWith("package.json")) {
          ByteArrayOutputStream baos = new ByteArrayOutputStream();
          tarIn.transferTo(baos);
          return baos.toString(StandardCharsets.UTF_8);
        }
      }

    } catch (Exception e) {
      throw new DependencyReaderException(
          String.format(
              "Failed to extract package.json from FHIR package %s@%s",
              packageKey.name(), packageKey.version()),
          e);
    }

    throw new DependencyReaderException(
        String.format(
            "package.json not found inside FHIR package %s@%s",
            packageKey.name(), packageKey.version()));
  }

  private Map<String, String> parseDependenciesFromPackageJson(String packageJson) {
    try {
      ObjectMapper mapper = new ObjectMapper();
      JsonNode deps = mapper.readTree(packageJson).path("dependencies");
      if (!deps.isObject()) {
        return Collections.emptyMap();
      }
      return mapper.convertValue(deps, new TypeReference<>() {});
    } catch (Exception e) {
      throw new DependencyReaderException("Failed to parse dependencies from package.json", e);
    }
  }

  public static class DependencyReaderException extends RuntimeException {
    public DependencyReaderException(String message) {
      super(message);
    }

    public DependencyReaderException(String message, Exception e) {
      super(message, e);
    }
  }
}
