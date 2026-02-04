# Documentation for package-registry

<a name="documentation-for-api-endpoints"></a>
## Documentation for API Endpoints

All URIs are relative to *http://localhost*

| Class | Method | HTTP request | Description |
|------------ | ------------- | ------------- | -------------|
| *FHIRPackagesApi* | [**getPackage**](Apis/FHIRPackagesApi.md#getPackage) | **GET** /packages/{packageName}/{packageVersion} | Get a specific FHIR package |
*FHIRPackagesApi* | [**getPackageOverview**](Apis/FHIRPackagesApi.md#getPackageOverview) | **GET** /packages/{packageName} | Get an overview over all versions of a FHIR package |


<a name="documentation-for-models"></a>
## Documentation for Models

 - [Dist](./Models/Dist.md)
 - [FhirPackageOverviewDto](./Models/FhirPackageOverviewDto.md)
 - [FhirPackageVersion](./Models/FhirPackageVersion.md)


<a name="documentation-for-authorization"></a>
## Documentation for Authorization

All endpoints do not require authorization.
