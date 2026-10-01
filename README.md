
# trade-reporting-extracts
[![License: Apache 2.0](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)

Trade Reporting Extracts (TRE) is a backend service that receives report requests from the frontend, persists them in the TRE data store, and manages notifications about report availability and status. It enables users to request, track, and download trade reports efficiently.

## Running the Service Locally

You can run the service locally in two ways:

### 1. Using sbt
After cloning the repository, run the following command in the root directory:

```
sbt run
```

### 2. Using Service Manager CLI
To use the service manager CLI (sm2), please refer to the [official setup guide](https://docs.tax.service.gov.uk/mdtp-handbook/documentation/developer-set-up/set-up-service-manager.html) for instructions on how to install and configure it locally.

Once set up:
- To start the service:
  ```sh
  sm2 --start TRE_ALL
  ```
- To stop the service:
  ```sh
  sm2 --stop TRE_ALL
  ```

## Login enrolments

The service's endpoints (that need Enrolment to access) can be accessed by using the enrolments below:

| Enrolment Key | Identifier Name | Identifier Value |  
|---------------|-----------------|------------------|  
| HMRC-CUS-ORG  | EORINumber      | GB123456789012   |  
| HMRC-CUS-ORG  | EORINumber      | GB123456789014   |  

## Testing

The minimum requirement for test coverage is **90%**. Builds will fail when the project drops below this threshold.

| Command                                | Description                  |  
|----------------------------------------|------------------------------|  
| `sbt test`                             | Runs unit tests locally      |  
| `sbt "test/testOnly *TEST_FILE_NAME*"` | Runs tests for a single file |  

## Coverage

| Command                                             | Description                                                                                                        |  
|-----------------------------------------------------|--------------------------------------------------------------------------------------------------------------------|  
| `sbt clean coverage test coverageReport`            | Generates a unit test coverage report. The report can be found at `target/scala-3.3.4/scoverage-report/index.html` |  


## API Endpoints Overview

| Name                                                                                                    | Method | Endpoint                                                                       | Description                                                                         |  
|---------------------------------------------------------------------------------------------------------|--------|--------------------------------------------------------------------------------|-------------------------------------------------------------------------------------|  
| [Report Request API](#reportrequest-api)                                                                | POST   | `/trade-reporting-extracts/create-report-request`                              | Submit a request for a trade report.                                                |  
| [Available Reports API](#available-reports-api)                                                         | GET    | `/trade-reporting-extracts/api/available-reports`                              | Retrieve a list of available trade reports for the authenticated user or EORI.      |  
| [Available Reports Count API](#available-reports-count-api)                                             | GET    | `/trade-reporting-extracts/api/available-reports-count`                        | Get the total number of available trade reports for the authenticated user or EORI. |  
| [Requested Reports API](#requested-reports-api)                                                         | GET    | `/trade-reporting-extracts/requested-reports`                                  | Retrieve a list of trade reports that have been requested by the user or EORI.      |  
| [Setup User API](#setup-user-api)                                                                       | GET    | `/trade-reporting-extracts/eori/setup-user`                                    | Initialize or set up a user in the system by EORI.                                  |  
| [Authorised EORIs API](#authorised-eoris-api)                                                           | POST   | `/trade-reporting-extracts/user/authorised-eoris`                              | Retrieve a list of EORI numbers the user is authorized to access.                   |  
| [Notification Email API]()                                                                              | POST   | `/trade-reporting-extracts/user/notification-email`                            | Retrieve notification email details.                                                |  
| [Notify Report Available API](#notify-report-available-api)                                             | POST   | `/trade-reporting-extracts/notify-report-available`                            | SDES notifies this system when a report file is available for download.             |  
| [Report Status Notification API](#report-status-notification-api)                                       | PUT    | `/trade-reporting-extracts/reportstatusnotification/v1`                        | EIS notifies this system about the status of a report request.                      |  
| [EORI Update API](#eori-update-api)                                                                     | PUT    | `/trade-reporting-extracts/updateeori/v1`                                      | Update EORI references.                                                             |  
| [Report Submission Limit API](#report-submission-limit-api)                                             | GET    | `/trade-reporting-extracts/report-submission-limit/:eori`                      | Determine whether submission limit has been reached.                                |  
| [Report Request Limit Number API](#report-request-limit-number-api)                                     | GET    | `/trade-reporting-extracts/report-request-limit-number`                        | Retrieve configured report request limit.                                           |  
| [User Detail API](#user-detail-api)                                                                     | GET    | `/trade-reporting-extracts/eori/get-user-detail`                               | Retrieve user and notification details.                                             |  
| [Download Audit API](#download-audit-api)                                                               | GET    | `/trade-reporting-extracts/downloaded-audit`                                   | Audit report download activity.                                                     |  
| [Company Information API](#company-information-api)                                                     | POST   | `/trade-reporting-extracts/company-information`                                | Retrieve company information.                                                       |  
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
 "eori": "GB123456789014", "reportStartDate": "2025-04-16", "reportEndDate": "2025-05-16", "whichEori": "GB123456789014", "reportName": "MyReport", "eoriRole": ["declarant"], "reportType": ["importHeader"], "dataType": "import", "additionalEmail": ["email1@gmail.com"]}  
```  

### Response Codes

| Status | Description |  
|----------|-------------|  
| 200 | Request created successfully |  
| 400 | Invalid request |  
  
---  

## Available Reports API

**Endpoint**

```http  
GET /trade-reporting-extracts/api/available-reports  
```  

### Description

Returns available reports for a user.

### Response Codes

| Status | Description |  
|----------|-------------|  
| 200 | Reports returned |  
| 400 | Invalid request |  
  
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
 "count": 2}  
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

| Status | Description |  
|----------|-------------|  
| 200 | Reports returned |  
| 204 | No reports found |  
| 400 | Invalid request |  
| 500 | Server error |  
  
---  

## Setup User API

**Endpoint**

```http  
GET /trade-reporting-extracts/eori/setup-user  
```  

### Description

Retrieves an existing TRE user or creates a new user when one does not exist.

### Response Codes

| Status | Description |  
|----------|-------------|  
| 200 | User found |  
| 201 | User created |  
  
---  

## Authorised EORIs API

**Endpoint**

```http  
POST /trade-reporting-extracts/user/authorised-eoris  
```  

### Description

Returns EORIs the user is authorised to access.

### Request Body

**TBC**

### Response

**TBC**
  
---  

## Notification Email API

**Endpoint**

```http  
POST /trade-reporting-extracts/user/notification-email  
```  

### Description

Returns the user's configured notification email.

### Request Body

**TBC**

### Response

**TBC**
  
---  

## User Detail API

**Endpoint**

```http  
GET /trade-reporting-extracts/eori/get-user-detail  
```  

### Description

Retrieves user details and email information.

### Response

**TBC**
  
---  

## Company Information API

**Endpoint**

```http  
POST /trade-reporting-extracts/company-information  
```  

### Description

Retrieves company information associated with an authorised EORI.

### Request Body

**TBC**

### Response

**TBC**
  
---  

## Report Submission Limit API

**Endpoint**

```http  
GET /trade-reporting-extracts/report-submission-limit/:eori  
```  

### Description

Checks whether an EORI has reached the configured report request submission limit.

### Response

**TBC**
  
---  

## Report Request Limit Number API

**Endpoint**

```http  
GET /trade-reporting-extracts/report-request-limit-number  
```  

### Description

Returns the configured maximum report request limit.

### Example Response

```json  
{  
 "limit": 100}  
```  

> Example only. Actual response may differ.
  
---  

## Download Audit API

**Endpoint**

```http  
GET /trade-reporting-extracts/downloaded-audit  
```  

### Description

Records report download audits.

### Request Parameters

**TBC**

### Response

**TBC**
  
---  

## Notify Report Available API

**Endpoint**

```http  
POST /trade-reporting-extracts/notify-report-available  
```  

### Description

Used by SDES to notify TRE that a file is available.

### Request Body

**TBC**

### Response Codes

| Status | Description |  
|----------|-------------|  
| 200 | Notification processed |  
| 400 | Invalid request |  
  
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

**TBC**

### Response Codes

| Status | Description |  
|----------|-------------|  
| 201 | Notification processed |  
| 400 | Invalid request |  
| 403 | Forbidden |  
  
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

**TBC**

### Response

**TBC**
  
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

**TBC**
  
---  

## Third Party Details

```http  
GET /trade-reporting-extracts/third-party-details  
```  

### Description

Returns third-party information associated with the user.

### Response

**TBC**
  
---  

## Authorised Business Details API

**Endpoint**

```http  
GET /trade-reporting-extracts/authorised-business-details  
```  

### Description

Returns authorised business details.

### Response

**TBC**
  
---  

## Users By Authorised EORI API

### Get Users By Authorised EORI

```http  
GET /trade-reporting-extracts/get-users-by-authorised-eori  
```  

### Description

Returns users associated with an authorised EORI.

### Response

**TBC**
  
---  

## Get Users By Authorised EORI Date Filtered

```http  
GET /trade-reporting-extracts/get-users-by-authorised-eori-date-filtered  
```  

### Description

Returns users associated with an authorised EORI using date filters.

### Response

**TBC**
  
---  

## Additional Email APIs

### Get Additional Emails

```http  
GET /trade-reporting-extracts/get-additional-emails  
```  

### Description

Returns additional emails configured for notifications.

### Response

**TBC**
  
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

**TBC**

### Response

**TBC**
  
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

**TBC**

### Response

**TBC**

---

## Third Party Details API

**Endpoint**

```http
GET /trade-reporting-extracts/third-party-details
```

### Description

Returns third-party access details associated with the user.

### Response

**TBC**

---

## Users By Authorised EORI Date Filtered API

**Endpoint**

```http
GET /trade-reporting-extracts/get-users-by-authorised-eori-date-filtered
```

### Description

Returns users associated with an authorised EORI using date filters.

### Response

**TBC**

---

## Get Additional Emails API

**Endpoint**

```http
GET /trade-reporting-extracts/get-additional-emails
```

### Description

Returns additional email addresses configured for notifications.

### Response

**TBC**

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

**TBC**

### Response

**TBC**


## Helpful Commands

| Command | Description |  
|----------|-------------|  
| `sbt run` | Runs the service |  
| `sbt clean` | Cleans build artifacts |  
| `sbt compile` | Compiles the project |  
| `sbt coverage` | Generates coverage data |  
| `sbt test` | Runs unit tests |  
| `sbt it/test` | Runs integration tests |  
| `sbt scalafmtCheckAll` | Formatting checks |  
| `sbt scalafmtAll` | Formats source code |  
| `sbt "test/testOnly *TEST_FILE_NAME*"` | Run a single test |  
| `sbt clean coverage test coverageReport` | Generate a coverage report |  

Coverage report location:

```text  
target/scala-3.3.5/scoverage-report/index.html  
```