package de.gematik.demis.packageregistry.domain;

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

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import lombok.*;

@Value
public class FhirPackage {

  private static final String STABLE_VERSION_REGEX =
      "^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)$";
  private static final String PRERELEASE_VERSION_REGEX = "^\\d+\\.\\d+\\.\\d+-[A-Za-z0-9._-]+$";

  PackageKey packageKey;
  Instant downloadedAt;

  /** The TGZ binary of the package. Stored as a read-only ByteBuffer to ensure immutability */
  @ToString.Exclude ByteBuffer binaryTgz;

  /** The raw dependencies as read from the package.json file. */
  @ToString.Exclude Map<String, String> rawDependencies;

  private FhirPackage(
      PackageKey packageKey,
      Instant downloadedAt,
      ByteBuffer binaryTgz,
      Map<String, String> rawDependencies) {

    this.packageKey = packageKey;
    this.downloadedAt = downloadedAt;
    this.binaryTgz = binaryTgz;
    this.rawDependencies =
        rawDependencies == null ? Map.of() : Collections.unmodifiableMap(rawDependencies);
  }

  public static FhirPackage of(
      PackageKey packageKey,
      Instant downloadedAt,
      ByteBuffer binaryTgz,
      Map<String, String> rawDependencies) {
    if (!isValidPackageVersion(packageKey.version())) {
      throw new IllegalArgumentException(
          String.format("Invalid version:  %s@%s", packageKey.name(), packageKey.version()));
    }
    return new FhirPackage(packageKey, downloadedAt, binaryTgz, rawDependencies);
  }

  public byte[] toBytes() {
    // Create a read-only view to safely extract bytes without modifying the original buffer
    ByteBuffer buffer = this.getBinaryTgz().asReadOnlyBuffer();
    byte[] bytes = new byte[buffer.remaining()];
    buffer.get(bytes);
    return bytes;
  }

  public String getVersion() {
    return this.packageKey.version();
  }

  public String getMajorMinorPatchVersion() {
    return this.packageKey.version().split("-", 2)[0];
  }

  public String getName() {
    return this.packageKey.name();
  }

  public boolean isStableVersion() {
    return getVersion().matches(STABLE_VERSION_REGEX);
  }

  public boolean isPrereleaseVersion() {
    return getVersion().matches(PRERELEASE_VERSION_REGEX);
  }

  private static boolean isValidPackageVersion(String version) {
    return version.matches(STABLE_VERSION_REGEX) || version.matches(PRERELEASE_VERSION_REGEX);
  }
}
