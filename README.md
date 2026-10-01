# trade-reporting-extracts

[![License: Apache 2.0](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)

Trade Reporting Extracts (TRE) is a backend service that receives report requests from the frontend, persists them in the TRE data store, and manages notifications about report availability and status. It enables users to request, track, and download trade reports efficiently.

## Running the Service Locally

You can run the service locally in two ways:

### 1. Using sbt

After cloning the repository, run the following command in the root directory:

```bash
sbt run
```

### 2. Using Service Manager CLI

To use the service manager CLI (sm2), please refer to the [official setup guide](https://docs.tax.service.gov.uk/mdtp-handbook/documentation/developer-set-up/set-up-service-manager.html) for instructions on how to install and configure it locally.

Once set up:

* To start the service:

  ```sh
  sm2 --start TRE_ALL
  ```
* To stop the service:

  ```sh
  sm2 --stop TRE_ALL
  ```

## Login enrolments

The service's endpoints (that need Enrolment to access) can be accessed by using the enrolments below:

| Enrolment Key | Identifier Name | Identifier Value |
| ------------- | --------------- | ---------------- |
| HMRC-CUS-ORG  | EORINumber      | GB123456789012   |
| HMRC-CUS-ORG  | EORINumber      | GB123456789014   |

## Testing

The minimum requirement for test coverage is **90%**. Builds will fail when the project drops below this threshold.

| Command                                | Description                  |
| -------------------------------------- | ---------------------------- |
| `sbt test`                             | Runs unit tests locally      |
| `sbt "test/testOnly *TEST_FILE_NAME*"` | Runs tests for a single file |

## Coverage

| Command                                  | Description                                                                                                        |
| ---------------------------------------- | ------------------------------------------------------------------------------------------------------------------ |
| `sbt clean coverage test coverageReport` | Generates a unit test coverage report. The report can be found at `target/scala-3.3.4/scoverage-report/index.html` |

## API Endpoints Overview

| Name                                                                                                    | Method | Endpoint                                                                       | Description                                                                         |
|---------------------------------------------------------------------------------------------------------|--------|--------------------------------------------------------------------------------|-------------------------------------------------------------------------------------|
| [Report Request API](#reportrequest-api)                                                                | POST   | `/trade-reporting-extracts/create-report-request`                              | Submit one or more requests for trade reports.                                      |
| [Available Reports API](#available-reports-api)                                                         | GET    | `/trade-reporting-extracts/api/available-reports`                              | Retrieve a list of available trade reports for the authenticated user or EORI.      |
| [Available Reports Count API](#available-reports-count-api)                                             | GET    | `/trade-reporting-extracts/api/available-reports-count`                        | Get the total number of available trade reports for the authenticated user or EORI. |
| [Requested Reports API](#requested-reports-api)                                                         | GET    | `/trade-reporting-extracts/requested-reports`                                  | Retrieve a list of trade reports that have been requested by the user or EORI.      |
| [Setup User API](#setup-user-api)                                                                       | GET    | `/trade-reporting-extracts/eori/setup-user`                                    | Initialize or set up a user in the system by EORI.                                  |
| [Authorised EORIs API](#authorised-eoris-api)                                                           | POST   | `/trade-reporting-extracts/user/authorised-eoris`                              | Retrieve a list of EORI numbers the user is authorized to access.                   |
| [Notification Email API]()                                                                              | POST   | `/trade-reporting-extracts/user/notification-email`                            | Retrieve notification email details.                                                |
| [Notify Report Available API](#notify-report-available-api)                                             | POST   | `/trade-reporting-extracts/notify-report-available`                            | Used by SDES to notify TRE that a report file is available for download.            |
| [Report Status Notification API](#report-status-notification-api)                                       | PUT    | `/trade-reporting-extracts/reportstatusnotification/v1`                        | EIS notifies this system about the status of a report request.                      |
| [EORI Update API](#eori-update-api)                                                                     | PUT    | `/trade-reporting-extracts/updateeori/v1`                                      | Update EORI references.                                                             |
| [Report Submission Limit API](#report-submission-limit-api)                                             | GET    | `/trade-reporting-extracts/report-submission-limit/:eori`                      | Determine whether an EORI has reached the daily report submission limit.            |
| [Report Request Limit Number API](#report-request-limit-number-api)                                     | GET    | `/trade-reporting-extracts/report-request-limit-number`                        | Retrieve the configured daily report request limit as a JSON string.                |
| [User Detail API](#user-detail-api)                                                                     | GET    | `/trade-reporting-extracts/eori/get-user-detail`                               | Retrieve user and notification details.                                             |
| [Download Audit API](#download-audit-api)                                                               | GET    | `/trade-reporting-extracts/downloaded-audit`                                   | Audit report download activity.                                                     |
| [Company Information API](#company-information-api)                                                     | POST   | `/trade-reporting-extracts/company-information`                                | Retrieve company information for an EORI.                                           |
| [Add Third Party Request API](#add-third-party-request-api)                                             | POST   | `/trade-reporting-extracts/add-third-party-request`                            | Add third-party access request.                                                     |
| [Edit Third Party Request API](#edit-third-party-request-api)                                           | PUT    | `/trade-reporting-extracts/edit-third-party-request`                           | Update third-party access request.                                                  |
| [Remove Third Party API](#remove-third-party-api)                                                       | DELETE | `/trade-reporting-extracts/remove-third-party`                                 | Remove third-party access.                                                          |
| [Third Party Self Removal API](#third-party-self-removal-api)                                           | DELETE | `/trade-reporting-extracts/third-party-access-self-removal`                    | Self-remove third-party access.                                                     |
| [Third Party Details API](#third-party-details-api)                                                     | GET    | `/trade-reporting-extracts/third-party-details`                                | Retrieve third-party access details.                                                |
| [Authorised Business Details API](#authorised-business-details-api)                                     | GET    | `/trade-reporting-extracts/authorised-business-details`                        | Retrieve authorised business details.                                               |
| [Users By Authorised EORI API](#users-by-authorised-eori-api)                                           | GET    | `/trade-reporting-extracts/get-users-by-authorised-eori`                       | Retrieve users with access to an authorised EORI.                                   |
| [Users By Authorised EORI Date Filtered API](#users-by-authorised-eori-date-filtered-api)               | GET    | `/trade-reporting-extracts/get-users-by-authorised-eori-date-filtered`         | Retrieve users with date filtering applied.                                         |
| [Get Additional Emails API](#get-additional-emails-api)                                                 | GET    | `/trade-reporting-extracts/get-additional-emails`                              | Retrieve additional email addresses.                                                |
| [Add Additional Email API](#add-additional-email-api)                                                   | POST   | `/trade-reporting-extracts/add-additional-email`                               | Add an additional email address.                                                    |
| [Remove Additional Email API](#remove-additional-email-api)                                             | DELETE | `/trade-reporting-extracts/remove-additional-email`                            | Remove an additional email address.                                                 |
| [Update Personal Email Notification Preference API](#update-personal-email-notification-preference-api) | POST   | `/trade-reporting-extracts/user/update-personal-email-notification-preference` | Update personal email notification preferences.                                     |

---

## ReportRequest API

**Endpoint**

```http
POST /trade-reporting-extracts/create-report-request
```

### Request Body

```json
{
  "eori": "GB123456789014",
  "reportStartDate": "2025-04-16",
  "reportEndDate": "2025-05-16",
  "whichEori": "GB123456789014",
  "reportName": "MyReport",
  "eoriRole": ["declarant"],
  "reportType": ["importHeader"],
  "dataType": "import",
  "additionalEmail": ["email1@gmail.com"]
}
```

### Response

Returns a JSON object containing

```json
[
  {
    "reportName": "MyReport",
    "reportType": "importHeader",
    "reportReference": "REF-00000001"
  }
]
```

For multiple report types:

```json
[
  {
    "reportName": "MyReport",
    "reportType": "importHeader",
    "reportReference": "REF-00000001"
  },
  {
    "reportName": "MyReport",
    "reportType": "importItem",
    "reportReference": "REF-00000002"
  }
]
```

### Response Codes

| Status | Description                            |
|--------|----------------------------------------|
| 200    | Report request(s) created successfully |
| 400    | Invalid request format                 |
| 403    | Authentication/authorisation failure   |
| 500    | Failed to create report request(s)     |


---

## Available Reports API

**Endpoint**

```http
GET /trade-reporting-extracts/api/available-reports
```

### Description

Returns available reports for a user.

### Response Codes

| Status | Description      |
| ------ | ---------------- |
| 200    | Reports returned |
| 400    | Invalid request  |

---

## Available Reports Count API

**Endpoint**

```http
GET /trade-reporting-extracts/api/available-reports-count
```

### Description

Returns a count of available reports.

### Example Response

```json
{
  "count": 2
}
```

---

## Requested Reports API

**Endpoint**

```http
GET /trade-reporting-extracts/requested-reports
```

### Description

Returns reports requested by the user.

### Response Codes

| Status | Description      |
| ------ | ---------------- |
| 200    | Reports returned |
| 204    | No reports found |
| 400    | Invalid request  |
| 500    | Server error     |

---

## Setup User API

**Endpoint**

```http
GET /trade-reporting-extracts/eori/setup-user
```

### Description

Retrieves an existing TRE user or creates a new user when one does not exist.

### Response Codes

| Status | Description                            |
| ------ | -------------------------------------- |
| 201    | User retrieved or created successfully |
| 400    | Invalid request                        |
| 403    | Forbidden                              |

> The supplied controller uses `Created(...)` for the successful response, so the controller implementation returns HTTP 201 for both an existing user and a newly created user.

---

## Authorised EORIs API

**Endpoint**

```http
POST /trade-reporting-extracts/user/authorised-eoris
```

### Description

Returns EORIs the user is authorised to access.

### Request Body

The EORI is supplied in the request body.

```json
{
  "eori": "GB123456789014"
}
```

### Response

Returns a JSON array containing the EORIs authorised for the supplied EORI.

```json
[
  "GB123456789014",
  "GB123456789012"
]
```

### Response Codes

| Status | Description                       |
| ------ | --------------------------------- |
| 200    | Authorised EORIs returned         |
| 400    | Invalid request                   |
| 403    | Forbidden                         |
| 500    | Error retrieving authorised EORIs |

---

## Notification Email API

**Endpoint**

```http
POST /trade-reporting-extracts/user/notification-email
```

### Description

Returns the user's configured notification email.

### Request Body

The EORI is supplied in the request body.

```json
{
  "eori": "GB123456789014"
}
```

### Response

Returns the user's notification email details.

The controller serialises the `NotificationEmail` model returned by the service. The model contains the notification email and its associated date/time information.

### Response Codes

| Status | Description                         |
| ------ | ----------------------------------- |
| 200    | Notification email returned         |
| 400    | Invalid request                     |
| 403    | Forbidden                           |
| 500    | Error retrieving notification email |

---

## User Detail API

**Endpoint**

```http
GET /trade-reporting-extracts/eori/get-user-detail
```

### Description

Retrieves user details and email information.

### Response

Returns the `UserDetails` object for the supplied EORI.

The response contains the following fields:

```json
{
  "eori": "GB123456789014",
  "additionalEmails": [],
  "authorisedUsers": [],
  "companyInformation": {},
  "notificationEmail": {}
}
```

The `UserDetails` model contains:

| Field                | Description                                         |
| -------------------- | --------------------------------------------------- |
| `eori`               | The user's EORI                                     |
| `additionalEmails`   | Additional email addresses associated with the user |
| `authorisedUsers`    | Users authorised for the EORI                       |
| `companyInformation` | Company information associated with the user        |
| `notificationEmail`  | Notification email details                          |

### Response Codes

| Status | Description           |
| ------ | --------------------- |
| 201    | User details returned |
| 400    | Invalid request       |
| 403    | Forbidden             |

---

## Company Information API

**Endpoint**

```http
POST /trade-reporting-extracts/company-information
```

### Description

Retrieves company information associated with an authorised EORI.

### Request Body

```json
{
  "eori": "GB123456789000"
}
```
### Response

```json
{
  "name": "Acme Ltd",
  "consent": "1",
  "address": {
    "addressLine1": "123 Street",
    "city": "City",
    "postcode": "AB12 3CD",
    "country": "UK"
  }
}
```

### Response Codes

| Status | Description           |
| ------ | --------------------- |
| 201    | Company information returned |
| 400    | Missing or invalid EORI in request body       |
| 403    | Authentication/authorisation failure             |

---

## Report Submission Limit API

**Endpoint**

```http
GET /trade-reporting-extracts/report-submission-limit/:eori
```

### Description

Checks whether an EORI has reached the configured report request submission limit.

### Response Codes

| Status | Description                                  |
|--------|----------------------------------------------|
| 204    | Report submission limit has not been reached |
| 429    | Report submission limit has been reached     |

---

## Report Request Limit Number API

**Endpoint**

```http
GET /trade-reporting-extracts/report-request-limit-number
```

### Description

Retrieve the configured daily report request limit as a JSON string.

### Example Response

```json
"25"
```

---

## Download Audit API

**Endpoint**

```http
GET /trade-reporting-extracts/downloaded-audit
```

### Description

Records report download audits.

### Request Body

The request body is parsed as an AuditDownloadRequest.

The exact fields depend on the AuditDownloadRequest model.
```json
{
"TBC": "AuditDownloadRequest fields"
}
```
### Response

A successful audit returns no response body.

204 No Content

If the audit processing fails, the error response returned by the service is passed directly to the caller.

### Response Codes
| Status | Description            |
| ------ | ---------------------- |
| 200    | Download audit processed successfully |
| 400    | Invalid request or audit processing failed        |

---

## Notify Report Available API

**Endpoint**

```http
POST /trade-reporting-extracts/notify-report-available
```

### Description

Used by SDES to notify TRE that a report file is available for download.

### Request Body

The request body must contain a valid FileNotificationResponse.

```json
{
  "eori": "GB123456789012",
  "fileName": "testFileName",
  "fileSize": 12345,
  "metadata": [
    {
      "retentionDays": "30"
    },
    {
      "fileType": "CSV"
    },
    {
      "eori": "GB123456789012"
    },
    {
      "mdtpReportXCorrelationID": "asfd-asdf-asdf"
    },
    {
      "mdtpReportRequestID": "TRE-19"
    },
    {
      "mdtpReportTypeName": "IMPORT-HEADER"
    },
    {
      "reportFilesParts": "1of2"
    }
  ]
}
```
### Response
A valid notification returns: Accepted
with HTTP 201 Created.

### Response Codes

| Status | Description                     |
|--------|---------------------------------|
| 200    | Notification processed          |
| 400    | Missing or invalid request body |

---

## Report Status Notification API

**Endpoints**

```http
PUT /tre/reportstatusnotification/v1
PUT /trade-reporting-extracts/reportstatusnotification/v1
```

### Description

Used by EIS to notify TRE of report status changes.

### Request Body

The request body must contain a valid EisReportStatusRequest.
```json
{
"applicationComponent": "CDAP",
"statusCode": "200",
"statusMessage": "Report processed successfully",
"statusTimestamp": "2023-10-02T14:30:00Z",
"statusType": "INFORMATION"
}
```

### Request Headers

The following headers are required:

| Header                  | Description              |
| ----------------------- | ------------------------ |
| `content-type`          | Request content type     |
| `authorization`         | EIS authentication token |
| `date`                  | HTTP request date        |
| `x-correlation-id`      | Correlation ID           |
| `x-transmitting-system` | Transmitting system      |
| `source-system`         | Source system            |


Example:

Content-Type: application/json
Authorization: Bearer EisAuthToken
Date: Mon, 02 Oct 2023 14:30:00 GMT
X-Correlation-Id: asfd-asdf-asdf
X-Transmitting-System: CDAP
Source-System: CDAP

### Response

A valid report status notification returns HTTP 201 Created.

### Response Codes

| Status | Description                                                             |
|--------|-------------------------------------------------------------------------|
| 201    | Notification processed                                                  |
| 400    | Missing required headers, missing request body, or invalid request body |
| 403    | Forbidden                                                               |

---

## EORI Update API

**Endpoints**

```http
PUT /tre/updateeori/v1
PUT /trade-reporting-extracts/updateeori/v1
```

### Description

Updates stored EORI information within TRE.

### Request Body

The request body must contain a valid EoriUpdate.

Example:
```json
{
    "newEori": "GB987654321098",
    "oldEori": "GB123456789012"
}
```

### Request Headers

The following headers are required:

| Header                  | Description               |
| ----------------------- | ------------------------- |
| `content-type`          | Request content type      |
| `authorization`         | ETMP authentication token |
| `date`                  | HTTP request date         |
| `x-correlation-id`      | Correlation ID            |
| `x-transmitting-system` | Transmitting system       |
| `source-system`         | Source system             |

### Response

A successful EORI update returns HTTP 201 Created.

### Response Codes

| Status | Description                                                             |
|--------|-------------------------------------------------------------------------|
| 201    | EORI update processed                                                   |
| 400    | Missing required headers, missing request body, or invalid request body |
| 403    | Forbidden                                                               |

---

## Third Party APIs

### Add Third Party Request

```http
POST /trade-reporting-extracts/add-third-party-request
```

### Description

Creates a third-party access request.

### Request Body

**TBC**

### Response

**TBC**

---

## Edit Third Party Request

```http
PUT /trade-reporting-extracts/edit-third-party-request
```

### Description

Updates a third-party access request.

### Request Body

**TBC**

### Response

**TBC**

---

## Remove Third Party

```http
DELETE /trade-reporting-extracts/remove-third-party
```

### Description

Removes third-party access.

### Request Body

**TBC**

---

## Third Party Self Removal

```http
DELETE /trade-reporting-extracts/third-party-access-self-removal
```

### Description

Allows a user to remove their own third-party access.

### Request Body

The request requires both the trader EORI and the third-party EORI.

```json
{
  "traderEori": "GB123456789014",
  "thirdPartyEori": "GB123456789012"
}
```

### Behaviour

The endpoint:

1. Removes the third-party authorisation for the trader EORI.
2. Removes reports associated with the third-party access removal.
3. Retrieves the trader's notification email.
4. Sends a third-party-access-self-removed email when a notification email is configured.
5. Returns HTTP 200 when the removal completes successfully.

If no notification email is configured, the removal still completes successfully and no email is sent.

### Response Codes

| Status | Description                                                                   |
| ------ | ----------------------------------------------------------------------------- |
| 200    | Third-party access successfully removed                                       |
| 400    | Missing or invalid request fields                                             |
| 403    | Forbidden                                                                     |
| 500    | Failed to remove third-party access, reports, or complete the removal request |

### Error Responses

If removal of the authorised user fails:

```text
Failed to remove third party access
```

If removal of associated reports fails:

```text
Failed to remove reports for third party access removal
```

If an exception occurs during the removal process:

```text
Failed to remove third party access removal request
```

---

## Third Party Details

```http
GET /trade-reporting-extracts/third-party-details
```

### Description

Returns third-party access information associated with the user.

### Request Body

The request requires both the user's EORI and the third-party EORI.

```json
{
  "eori": "GB123456789014",
  "thirdPartyEori": "GB123456789012"
}
```

### Response

When an authorised user is found, the endpoint returns `ThirdPartyDetails`.

The response contains:

| Field             | Description                                     |
| ----------------- | ----------------------------------------------- |
| `referenceName`   | Optional reference name for the third party     |
| `accessStartDate` | Date third-party access starts                  |
| `accessEndDate`   | Optional date third-party access ends           |
| `dataTypes`       | Set of data types accessible to the third party |
| `dataStartDate`   | Optional start date for the accessible data     |
| `dataEndDate`     | Optional end date for the accessible data       |

Example:

```json
{
  "referenceName": "Example Third Party",
  "accessStartDate": "2025-04-16",
  "accessEndDate": "2025-05-16",
  "dataTypes": [
    "import",
    "export"
  ],
  "dataStartDate": "2025-04-16",
  "dataEndDate": "2025-05-16"
}
```

### Response Codes

| Status | Description                                   |
| ------ | --------------------------------------------- |
| 200    | Third-party details returned                  |
| 400    | Missing or invalid request fields             |
| 403    | Forbidden                                     |
| 404    | No authorised user found for third party EORI |

### 404 Response

```text
No authorised user found for third party EORI
```

---

## Authorised Business Details API

**Endpoint**

```http
GET /trade-reporting-extracts/authorised-business-details
```

### Description

Returns authorised business details.

### Request Body

The request requires both the third-party EORI and trader EORI.

```json
{
  "thirdPartyEori": "GB123456789012",
  "traderEori": "GB123456789014"
}
```

### Response

When an authorised business is found, the endpoint returns the third-party details associated with the authorised business.

The response contains:

| Field             | Description                             |
| ----------------- | --------------------------------------- |
| `referenceName`   | Optional reference name                 |
| `accessStartDate` | Date access starts                      |
| `accessEndDate`   | Optional date access ends               |
| `dataTypes`       | Set of accessible data types            |
| `dataStartDate`   | Optional start date for accessible data |
| `dataEndDate`     | Optional end date for accessible data   |

Example:

```json
{
  "referenceName": "Example Third Party",
  "accessStartDate": "2025-04-16",
  "accessEndDate": "2025-05-16",
  "dataTypes": [
    "import",
    "export"
  ],
  "dataStartDate": "2025-04-16",
  "dataEndDate": "2025-05-16"
}
```

### Response Codes

| Status | Description                                  |
| ------ | -------------------------------------------- |
| 200    | Authorised business details returned         |
| 400    | Missing or invalid request fields            |
| 403    | Forbidden                                    |
| 404    | No authorised user found for the trader EORI |

### 404 Response

```text
No authorised user found for the trader EORI
```

---

## Users By Authorised EORI API

### Get Users By Authorised EORI

```http
GET /trade-reporting-extracts/get-users-by-authorised-eori
```

### Description

Returns users associated with an authorised EORI.

### Request Body

The authorised EORI is supplied in the request body.

```json
{
  "thirdPartyEori": "GB123456789012"
}
```

### Response

Returns a JSON array of `EoriBusinessAccessInfo` objects.

Each object contains:

| Field             | Description                          |
| ----------------- | ------------------------------------ |
| `eori`            | User EORI                            |
| `businessInfo`    | Optional business information        |
| `accessStart`     | Access start timestamp               |
| `reportDataStart` | Optional report data start timestamp |

Example:

```json
[
  {
    "eori": "GB123456789014",
    "businessInfo": "ABC Ltd",
    "accessStart": "2025-04-16T00:00:00Z",
    "reportDataStart": "2025-04-16T00:00:00Z"
  }
]
```

`businessInfo` may also be `null` when no business information is available.

### Response Codes

| Status | Description                                                |
| ------ | ---------------------------------------------------------- |
| 200    | Users returned                                             |
| 400    | Missing or invalid third-party EORI                        |
| 403    | Forbidden                                                  |
| 500    | Failed to fetch users by authorised EORI with access dates |

### Error Response

```text
Failed to fetch users by authorised EORI with access dates
```

---

## Get Users By Authorised EORI Date Filtered

```http
GET /trade-reporting-extracts/get-users-by-authorised-eori-date-filtered
```

### Description

Returns users associated with an authorised EORI using date filters.

### Request Body

The authorised EORI is supplied in the request body.

```json
{
  "thirdPartyEori": "GB123456789012"
}
```

### Response

Returns a JSON array of `EoriBusinessInfo` objects.

Each object contains:

| Field          | Description                   |
| -------------- | ----------------------------- |
| `eori`         | User EORI                     |
| `businessInfo` | Optional business information |

Example:

```json
[
  {
    "eori": "GB123456789014",
    "businessInfo": "ABC Ltd"
  }
]
```

`businessInfo` may also be `null` when no business information is available.

### Response Codes

| Status | Description                                               |
| ------ | --------------------------------------------------------- |
| 200    | Users returned                                            |
| 400    | Missing or invalid third-party EORI                       |
| 403    | Forbidden                                                 |
| 500    | Failed to fetch users by authorised EORI with date filter |

### Error Response

```text
Failed to fetch users by authorised EORI with date filter
```

---

## Additional Email APIs

### Get Additional Emails

```http
GET /trade-reporting-extracts/get-additional-emails
```

### Description

Returns additional emails configured for notifications.

### Request Body

The EORI is supplied in the request body.

```json
{
  "eori": "GB123456789014"
}
```

### Response

Returns a JSON array of additional email addresses.

Example:

```json
[
  "email1@example.com",
  "email2@example.com"
]
```

### Response Codes

| Status | Description                        |
| ------ | ---------------------------------- |
| 200    | Additional emails returned         |
| 400    | Missing or invalid EORI            |
| 403    | Forbidden                          |
| 500    | Error retrieving additional emails |

---

## Add Additional Email

```http
POST /trade-reporting-extracts/add-additional-email
```

### Description

Adds an additional notification email.

### Request Body

**TBC**

### Response

**TBC**

---

## Remove Additional Email

```http
DELETE /trade-reporting-extracts/remove-additional-email
```

### Description

Removes an additional notification email.

### Request Body

**TBC**

### Response

**TBC**

---

## Personal Email Notification Preference API

**Endpoint**

```http
POST /trade-reporting-extracts/user/update-personal-email-notification-preference
```

### Description

Updates whether personal email notifications are enabled.

### Request Body

The request is an `UpdateEmailPreference` object.

```json
{
  "eori": "GB123456789014",
  "updatedPreference": true
}
```

| Field               | Description                                            |
| ------------------- | ------------------------------------------------------ |
| `eori`              | EORI whose notification preference is being updated    |
| `updatedPreference` | Whether personal email notifications should be enabled |

### Response

A successful update returns HTTP 200.

### Response Codes

| Status | Description                                                   |
| ------ | ------------------------------------------------------------- |
| 200    | Personal email notification preference updated                |
| 400    | Invalid update personal email notification preference request |
| 403    | Forbidden                                                     |
| 500    | Failed to update personal email notification preference       |

### Error Response

For a service failure:

```text
Failed to update personal email notification preference
```

For an invalid request body:

```json
{
  "error": "Invalid update personal email notification preference"
}
```

---

## Add Third Party Request API

**Endpoint**

```http
POST /trade-reporting-extracts/add-third-party-request
```

### Description

Creates a third-party access request.

### Request Body

**TBC**

### Response

**TBC**

---

## Edit Third Party Request API

**Endpoint**

```http
PUT /trade-reporting-extracts/edit-third-party-request
```

### Description

Updates a third-party access request.

### Request Body

**TBC**

### Response

**TBC**

---

## Remove Third Party API

**Endpoint**

```http
DELETE /trade-reporting-extracts/remove-third-party
```

### Description

Removes third-party access.

### Request Body

**TBC**

### Response

**TBC**

---

## Third Party Self Removal API

**Endpoint**

```http
DELETE /trade-reporting-extracts/third-party-access-self-removal
```

### Description

Allows a user to remove their own third-party access.

### Request Body

The request requires both the trader EORI and the third-party EORI.

```json
{
  "traderEori": "GB123456789014",
  "thirdPartyEori": "GB123456789012"
}
```

### Response Codes

| Status | Description                                                                   |
| ------ | ----------------------------------------------------------------------------- |
| 200    | Third-party access successfully removed                                       |
| 400    | Missing or invalid request fields                                             |
| 403    | Forbidden                                                                     |
| 500    | Failed to remove third-party access, reports, or complete the removal request |

---

## Third Party Details API

**Endpoint**

```http
GET /trade-reporting-extracts/third-party-details
```

### Description

Returns third-party access details associated with the user.

### Request Body

The request requires both the user's EORI and the third-party EORI.

```json
{
  "eori": "GB123456789014",
  "thirdPartyEori": "GB123456789012"
}
```

### Response

Returns `ThirdPartyDetails`.

```json
{
  "referenceName": "Example Third Party",
  "accessStartDate": "2025-04-16",
  "accessEndDate": "2025-05-16",
  "dataTypes": [
    "import",
    "export"
  ],
  "dataStartDate": "2025-04-16",
  "dataEndDate": "2025-05-16"
}
```

### Response Codes

| Status | Description                                   |
| ------ | --------------------------------------------- |
| 200    | Third-party details returned                  |
| 400    | Missing or invalid request fields             |
| 403    | Forbidden                                     |
| 404    | No authorised user found for third party EORI |

---

## Users By Authorised EORI Date Filtered API

**Endpoint**

```http
GET /trade-reporting-extracts/get-users-by-authorised-eori-date-filtered
```

### Description

Returns users associated with an authorised EORI using date filters.

### Request Body

```json
{
  "thirdPartyEori": "GB123456789012"
}
```

### Response

Returns a JSON array of `EoriBusinessInfo` objects.

```json
[
  {
    "eori": "GB123456789014",
    "businessInfo": "ABC Ltd"
  }
]
```

### Response Codes

| Status | Description                                               |
| ------ | --------------------------------------------------------- |
| 200    | Users returned                                            |
| 400    | Missing or invalid third-party EORI                       |
| 403    | Forbidden                                                 |
| 500    | Failed to fetch users by authorised EORI with date filter |

---

## Get Additional Emails API

**Endpoint**

```http
GET /trade-reporting-extracts/get-additional-emails
```

### Description

Returns additional email addresses configured for notifications.

### Request Body

```json
{
  "eori": "GB123456789014"
}
```

### Response

Returns a JSON array of email addresses.

```json
[
  "email1@example.com",
  "email2@example.com"
]
```

### Response Codes

| Status | Description                        |
| ------ | ---------------------------------- |
| 200    | Additional emails returned         |
| 400    | Missing or invalid EORI            |
| 403    | Forbidden                          |
| 500    | Error retrieving additional emails |

---

## Add Additional Email API

**Endpoint**

```http
POST /trade-reporting-extracts/add-additional-email
```

### Description

Adds an additional notification email address.

### Request Body

**TBC**

### Response

**TBC**

---

## Remove Additional Email API

**Endpoint**

```http
DELETE /trade-reporting-extracts/remove-additional-email
```

### Description

Removes an additional notification email address.

### Request Body

**TBC**

### Response

**TBC**

---

## Update Personal Email Notification Preference API

**Endpoint**

```http
POST /trade-reporting-extracts/user/update-personal-email-notification-preference
```

### Description

Updates whether personal email notifications are enabled.

### Request Body

```json
{
  "eori": "GB123456789014",
  "updatedPreference": true
}
```

### Response

A successful update returns HTTP 200.

### Response Codes

| Status | Description                                                   |
| ------ | ------------------------------------------------------------- |
| 200    | Personal email notification preference updated                |
| 400    | Invalid update personal email notification preference request |
| 403    | Forbidden                                                     |
| 500    | Failed to update personal email notification preference       |

### Error Response

```json
{
  "error": "Invalid update personal email notification preference"
}
```

---

## Helpful Commands

| Command                                  | Description                |
| ---------------------------------------- | -------------------------- |
| `sbt run`                                | Runs the service           |
| `sbt clean`                              | Cleans build artifacts     |
| `sbt compile`                            | Compiles the project       |
| `sbt coverage`                           | Generates coverage data    |
| `sbt test`                               | Runs unit tests            |
| `sbt it/test`                            | Runs integration tests     |
| `sbt scalafmtCheckAll`                   | Formatting checks          |
| `sbt scalafmtAll`                        | Formats source code        |
| `sbt "test/testOnly *TEST_FILE_NAME*"`   | Run a single test          |
| `sbt clean coverage test coverageReport` | Generate a coverage report |

Coverage report location:

```text
target/scala-3.3.5/scoverage-report/index.html
```
