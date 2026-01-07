<div style="text-align:right"><img src="https://raw.githubusercontent.com/gematik/gematik.github.io/master/Gematik_Logo_Flag_With_Background.png" width="250" height="47" alt="gematik GmbH Logo"/> <br/> </div> <br/> 

# Package-Registry

[![Quality Gate Status](https://sonar.prod.ccs.gematik.solutions/api/project_badges/measure?project=de.gematik.demis%3Apackage-registry&metric=alert_status&token=sqb_10ef3bcb01d693226638edb174f7143bbf27ad93)](https://sonar.prod.ccs.gematik.solutions/dashboard?id=de.gematik.demis%3Apackage-registry)[![Vulnerabilities](https://sonar.prod.ccs.gematik.solutions/api/project_badges/measure?project=de.gematik.demis%3Apackage-registry&metric=vulnerabilities&token=sqb_10ef3bcb01d693226638edb174f7143bbf27ad93)](https://sonar.prod.ccs.gematik.solutions/dashboard?id=de.gematik.demis%3Apackage-registry)[![Bugs](https://sonar.prod.ccs.gematik.solutions/api/project_badges/measure?project=de.gematik.demis%3Apackage-registry&metric=bugs&token=sqb_10ef3bcb01d693226638edb174f7143bbf27ad93)](https://sonar.prod.ccs.gematik.solutions/dashboard?id=de.gematik.demis%3Apackage-registry)[![Code Smells](https://sonar.prod.ccs.gematik.solutions/api/project_badges/measure?project=de.gematik.demis%3Apackage-registry&metric=code_smells&token=sqb_10ef3bcb01d693226638edb174f7143bbf27ad93)](https://sonar.prod.ccs.gematik.solutions/dashboard?id=de.gematik.demis%3Apackage-registry)[![Lines of Code](https://sonar.prod.ccs.gematik.solutions/api/project_badges/measure?project=de.gematik.demis%3Apackage-registry&metric=ncloc&token=sqb_10ef3bcb01d693226638edb174f7143bbf27ad93)](https://sonar.prod.ccs.gematik.solutions/dashboard?id=de.gematik.demis%3Apackage-registry)[![Coverage](https://sonar.prod.ccs.gematik.solutions/api/project_badges/measure?project=de.gematik.demis%3Apackage-registry&metric=coverage&token=sqb_10ef3bcb01d693226638edb174f7143bbf27ad93)](https://sonar.prod.ccs.gematik.solutions/dashboard?id=de.gematik.demis%3Apackage-registry)

<details>
  <summary>Table of Contents</summary>
  <ol>
    <li>
      <a href="#about-the-project">About The Project</a>
      <ul>
        <li><a href="#release-notes">Release Notes</a></li>
      </ul>
    </li>
    <li>
      <a href="#getting-started">Getting Started</a>
      <ul>
        <li><a href="#prerequisites">Prerequisites</a></li>
        <li><a href="#installation">Installation</a></li>
      </ul>
    </li>
    <li><a href="#usage">Usage</a></li>
    <li><a href="#security-policy">Security Policy</a></li>
    <li><a href="#contributing">Contributing</a></li>
    <li><a href="#license">License</a></li>
    <li><a href="#contact">Contact</a></li>
  </ol>
</details>

## About The Project

The package-registry is a service that provides a central FHIR packages repository for other services within the
cluster.

### Getting Started

The package registry retrieves the FHI packages from a source registry.
Currently following source registries are supported and can be configured:

- Private Google Artifact Registry (GAR)
    - authentication with service account key file
    - supports supply chain validation (Cosign signature and stage validation)
- Public Google Artifact Registry (GAR)
    - no registration/authentication
    - supports supply chain validation (Cosign signature only)
- Generic public npm registry
    - no registration/authentication
    - no supply chain validation

#### Configuration

The package-registry can be configured via environment variables.

| Env Var / Type of source registry | Private Google Artifact Registry (GAR)             | Public Google Artifact Registry (GAR)              | Generic public npm registry   |
|-----------------------------------|----------------------------------------------------|----------------------------------------------------|-------------------------------|
| SOURCE_REGISTRY_TYPE              | GCP_WITH_JSON_KEYFILE                              | GCP_PUBLIC                                         | PUBLIC                        |
| SOURCE_REGISTRY_URL               | required                                           | required                                           | required                      |
| SUPPLY_CHAIN_VERIFICATION_ENABLED | true or false (default true)                       | true or false (default true)                       | not relevant                  |
| SIGNATURE_SAN                     | required if SUPPLY_CHAIN_VERIFICATION_ENABLED true | required if SUPPLY_CHAIN_VERIFICATION_ENABLED true | not relevant                  |
| ATTESTATION_SAN                   | required if SUPPLY_CHAIN_VERIFICATION_ENABLED true | not relevant                                       | not relevant                  |
| SIGNATURE_ATTESTATION_URL         | required if SUPPLY_CHAIN_VERIFICATION_ENABLED true | required if SUPPLY_CHAIN_VERIFICATION_ENABLED true | not relevant                  |
| GCP_SA_KEYFILE_PATH               | required                                           | not relevant                                       | not relevant                  |
| CONFIG_DEPENDENCY_LOADING_ENABLED | true or false (default false)                      | true or false (default false)                      | true or false (default false) |

### Release Notes

See [ReleaseNotes.md](./ReleaseNotes.md) for all information regarding the (newest) releases.

### Prerequisites

The Project requires Java 21 and Maven 3.8+.

### Installation

The Project can be built with the following command:

```sh
mvn clean install
```

The Docker Image associated to the service can be built with the extra profile `docker`:

```sh
mvn clean install -Pdocker
```

## Architecture

This project follows the principles of Clean Architecture (also known as Hexagonal Architecture or Ports and
Adapters).  
The goal is to separate business logic from technical concerns, making the codebase easier to test, evolve, and
maintain.

### Key rules

- The domain layer knows nothing about any other layer.
- The service layer (core package) orchestrates application logic and defines ports for external interactions.
- The adapter layer implements those ports.
- Dependencies flow inward only: adapters → services → domain.

## Usage

The application can be executed from a JAR file or a Docker Image:

```sh
# As JAR Application
java -jar target/package-registry.jar
# As Docker Image
docker run --rm -it -p 8080:8080 package-registry:latest
```

It can also be deployed on Kubernetes by using the Helm Chart defined in the folder `deployment/helm/package-registry`:

```ssh
helm install package-registry ./deployment/helm/package-registry
```

### Continuous Integration and Delivery

The project contains Jenkins Pipelines to perform automatic build and scanning (`ci.jenkinsfile`) and release (based on
retagging of the given Git Tag, `release.jenkinsfile`).
Please adjust the variable values defined at the beginning of the pipelines!

For both the pipelines, you need to create a first initial Release Version in JIRA, so it can be retrieved from Jenkins
with the Jenkins Shared Library functions.

**BEWARE**: The Release Pipeline requires a manual configuration of the parameters over the Jenkins UI, defining a JIRA
Release Version plugin and naming it `JIRA_RELEASE_VERSION`.
The Information such as Project Key and Regular Expression depends on the project and must be correctly configured.

### Endpoints

Define here the available endpoints exposed by the service

## Security Policy

If you want to see the security policy, please check our [SECURITY.md](.github/SECURITY.md).

## Contributing

If you want to contribute, please check our [CONTRIBUTING.md](.github/CONTRIBUTING.md).

## License

Copyright 2023-2025 gematik GmbH

EUROPEAN UNION PUBLIC LICENCE v. 1.2

EUPL © the European Union 2007, 2016

See the [LICENSE](./LICENSE.md) for the specific language governing permissions and limitations under the License

## Additional Notes and Disclaimer from gematik GmbH

1. Copyright notice: Each published work result is accompanied by an explicit statement of the license conditions for
   use. These are regularly typical conditions in connection with open source or free software. Programs
   described/provided/linked here are free software, unless otherwise stated.
2. Permission notice: Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
   associated documentation files (the "Software"), to deal in the Software without restriction, including without
   limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the
   Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
    1. The copyright notice (Item 1) and the permission notice (Item 2) shall be included in all copies or substantial
       portions of the Software.
    2. The software is provided "as is" without warranty of any kind, either express or implied, including, but not
       limited to, the warranties of fitness for a particular purpose, merchantability, and/or non-infringement. The
       authors or copyright holders shall not be liable in any manner whatsoever for any damages or other claims arising
       from, out of or in connection with the software or the use or other dealings with the software, whether in an
       action of contract, tort, or otherwise.
    3. We take open source license compliance very seriously. We are always striving to achieve compliance at all times
       and to improve our processes. If you find any issues or have any suggestions or comments, or if you see any other
       ways in which we can improve, please reach out to: ospo@gematik.de
3. Please note: Parts of this code may have been generated using AI-supported technology. Please take this into account,
   especially when troubleshooting, for security analyses and possible adjustments.

## Contact

E-Mail to [DEMIS Entwicklung](mailto:demis-entwicklung@gematik.de?subject=[GitHub]%20Package-Registry)