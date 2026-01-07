# FHIRPackagesApi

All URIs are relative to *http://localhost:37103*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**getPackage**](FHIRPackagesApi.md#getPackage) | **GET** /packages/{packageName}/{packageVersion} | Get a specific FHIR package |


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
- **Accept**: */*

