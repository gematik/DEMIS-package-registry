<div style="text-align:right"><img src="https://raw.githubusercontent.com/gematik/gematik.github.io/master/Gematik_Logo_Flag_With_Background.png" width="250" height="47" alt="gematik GmbH Logo"/> <br/> </div> <br/> 

# Release notes

## Release 1.0.3

- activated openapi documentation generation
- improved documentation
- updated spring-parent to 2.14.20

## Release 1.0.2

- internal release adjustments

## Release 1.0.1

- updated dependencies

## Release 1.0.0

- The Package Registry provides centralized access to FHIR packages for internal services
  within the Kubernetes cluster, simplifying package distribution and reducing redundant downloads from source
  registry.
- 3 types of source registries are supported: Google Artifact Registry (GAR) with or without authentication, as well as
  any generic public npm registry.
- Supply chain security: supports signature and stage attestation verification (scope depending on source registry
  type).
- The registry can be configured to resolve and download package dependencies recursively (incl. versions with wildcard
  like foo@1.0.x).
- The registry can be configured to pre-load packages at startup. Regardless of startup configuration, any requested
  package not found in the registry will be retrieved on demand from the source.