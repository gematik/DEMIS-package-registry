# FHIRPackagesApi

All URIs are relative to *http://localhost*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**getPackage**](FHIRPackagesApi.md#getPackage) | **GET** /packages/{packageName}/{packageVersion} | Get a specific FHIR package |
| [**getPackageOverview**](FHIRPackagesApi.md#getPackageOverview) | **GET** /packages/{packageName} | Get an overview over all versions of a FHIR package |


<a name="getPackage"></a>
# **getPackage**
> byte[] getPackage(packageName, packageVersion)

Get a specific FHIR package

    Returns the binary content of a specific FHIR package by name and version

### Parameters

|Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **packageName** | **String**|  | [default to null] |
| **packageVersion** | **String**|  | [default to null] |

### Return type

**byte[]**

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/fhir+npmpackage

<a name="getPackageOverview"></a>
# **getPackageOverview**
> FhirPackageOverviewDto getPackageOverview(packageName)

Get an overview over all versions of a FHIR package

    Returns metadata about all available versions of a specific FHIR package by name

### Parameters

|Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **packageName** | **String**|  | [default to null] |

### Return type

[**FhirPackageOverviewDto**](../Models/FhirPackageOverviewDto.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

