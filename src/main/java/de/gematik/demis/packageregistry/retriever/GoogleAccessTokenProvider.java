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

import com.google.auth.oauth2.GoogleCredentials;
import jakarta.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
    prefix = "retriever.source-registry",
    name = "type",
    havingValue = "GCP_WITH_JSON_KEYFILE")
@RequiredArgsConstructor
@Slf4j
public class GoogleAccessTokenProvider {

  private GoogleCredentials credentials;

  private final RetrieverProperties retrieverProperties;

  private static final String GCP_SCOPE_CLOUD_PLATFORM =
      "https://www.googleapis.com/auth/cloud-platform";

  @PostConstruct
  public void init() {
    try (InputStream in =
        new FileInputStream(retrieverProperties.getGcp().serviceAccountKeyfilePath())) {
      this.credentials =
          GoogleCredentials.fromStream(in).createScoped(List.of(GCP_SCOPE_CLOUD_PLATFORM));
      log.info("Google credentials loaded from keyfile");
    } catch (IOException e) {
      throw new PackageRetrieverException("Failed to load Google credentials from keyfile", e);
    }
  }

  public String getToken() {
    try {
      credentials.refreshIfExpired();
    } catch (IOException e) {
      throw new PackageRetrieverException("Failed to refresh Google credentials", e);
    }
    return credentials.getAccessToken().getTokenValue();
  }
}
