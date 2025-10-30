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

| Name                                                              | Endpoint                                                    | Description                                                                         |
|-------------------------------------------------------------------|-------------------------------------------------------------|-------------------------------------------------------------------------------------|
| [ReportRequest API](#reportrequest-api)                           | POST `/trade-reporting-extracts/create-report-request`      | Submit a request for a trade report.                                                |
| [Available Reports API](#available-reports-api)                   | GET `/trade-reporting-extracts/api/available-reports`       | Retrieve a list of available trade reports for the authenticated user or EORI.      |
| [Available Reports Count API](#available-reports-count-api)       | GET `/trade-reporting-extracts/api/available-reports-count` | Get the total number of available trade reports for the authenticated user or EORI. |
| [Requested Reports API](#requested-reports-api)                   | GET `/trade-reporting-extracts/requested-reports`           | Retrieve a list of trade reports that have been requested by the user or EORI.      |
| [Setup User API](#setup-user-api)                                 | GET `/trade-reporting-extracts/eori/setup-user`             | Initialize or set up a user in the system by EORI.                                  |
| [Authorised EORIs API](#authorised-eoris-api)                     | GET `/trade-reporting-extracts/user/:eori/authorised-eoris` | Retrieve a list of EORI numbers the user is authorized to access.                   |
| [Notify Report Available API](#notify-report-available-api)       | POST `/trade-reporting-extracts/notify-report-available`    | SDES notifies this system when a report file is available for download.             |
| [Report Status Notification API](#report-status-notification-api) | PUT `/tre/reportstatusnotification/v1`                      | EIS notifies this system about the status of a report request.                      |

## ReportRequest API

The `ReportRequest` API allows clients to request trade reports by submitting a JSON payload. The API endpoint is:

- **POST** `/trade-reporting-extracts/create-report-request`

### Request Body

The request should be a JSON object with the following fields:

- `eori`: String, the EORI number of the requester.
- `reportStartDate`: String (YYYY-MM-DD), the start date for the report.
- `reportEndDate`: String (YYYY-MM-DD), the end date for the report.
- `whichEori`: String, the EORI number for which the report is requested.
- `reportName`: String, a name for the report.
- `eoriRole`: Array of Strings, roles associated with the EORI (e.g., `["declarant"]`).
- `reportType`: Array of Strings, types of reports requested (e.g., `["importHeader"]`).
- `dataType`: String, type of data (e.g., `import`).
- `additionalEmail`: Array of Strings, additional email addresses for notifications.

#### Example Request

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

### Response Codes

| Status Code     | Description                                                                         |
|-----------------|-------------------------------------------------------------------------------------|
| 200 OK          | Report request(s) created successfully. Returns references to the created requests. |
| 400 Bad Request | The request body is invalid or missing required fields.                             |

## Available Reports API

The `Available Reports` API allows clients to retrieve a list of available trade reports for the authenticated user or EORI.

- **GET** `/trade-reporting-extracts/api/available-reports`

### Request Body
The request must be a JSON object containing the EORI:

```json
{
  "eori": "GB123456789012"
}
```

### Description
Returns a list of available trade reports that can be downloaded or accessed by the user. This endpoint is typically used to display available reports in a user interface or to automate report retrieval.

### Response
The response is a JSON array, where each object represents an available report with details such as report name, type, date range, and download link.

#### Example Response
```json
[
  {
    "reportId": "12345",
    "reportName": "Import Report April 2025",
    "reportType": "importHeader",
    "reportStartDate": "2025-04-01",
    "reportEndDate": "2025-04-30",
    "downloadUrl": "/trade-reporting-extracts/download/12345"
  },
  {
    "reportId": "67890",
    "reportName": "Export Report May 2025",
    "reportType": "exportHeader",
    "reportStartDate": "2025-05-01",
    "reportEndDate": "2025-05-31",
    "downloadUrl": "/trade-reporting-extracts/download/67890"
  }
]
```

### Response Codes

| Status Code     | Description                                              |
|-----------------|----------------------------------------------------------|
| 200 OK          | List of available reports returned successfully.         |
| 400 Bad Request | The request body is missing or contains an invalid EORI. |

## Available Reports Count API

The `Available Reports Count` API allows clients to retrieve the total number of available trade reports for the authenticated user or EORI.

- **GET** `/trade-reporting-extracts/api/available-reports-count`

### Request Body
The request must be a JSON object containing the EORI:

```json
{
  "eori": "GB123456789012"
}
```

### Description
Returns a JSON object containing the count of available trade reports. This endpoint is useful for displaying summary information or for pagination purposes in a user interface.

### Response
The response is a JSON object with a single field `count` indicating the number of available reports.

#### Example Response
```json
{
  "count": 2
}
```

### Response Codes

| Status Code     | Description                                              |
|-----------------|----------------------------------------------------------|
| 200 OK          | Count of available reports returned successfully.        |
| 400 Bad Request | The request body is missing or contains an invalid EORI. |

## Requested Reports API

The `Requested Reports` API allows clients to retrieve a list of trade reports that have been requested by the user or EORI.

- **GET** `/trade-reporting-extracts/requested-reports`

### Request Body
The request must be a JSON object containing the EORI:

```json
{
  "eori": "GB123456789012"
}
```

### Description
Returns a list of trade report requests made by the user. This endpoint is typically used to display the status or history of report requests in a user interface.

### Response
The response is a JSON array, where each object represents a requested report with details such as request ID, report name, type, date range, status, and submission date.

#### Example Response
```json
[
  {
    "requestId": "req-12345",
    "reportName": "Import Report April 2025",
    "reportType": "importHeader",
    "reportStartDate": "2025-04-01",
    "reportEndDate": "2025-04-30",
    "status": "Completed",
    "submittedAt": "2025-05-01T10:00:00Z"
  },
  {
    "requestId": "req-67890",
    "reportName": "Export Report May 2025",
    "reportType": "exportHeader",
    "reportStartDate": "2025-05-01",
    "reportEndDate": "2025-05-31",
    "status": "Processing",
    "submittedAt": "2025-06-01T09:30:00Z"
  }
]
```

### Response Codes

| Status Code               | Description                                              |
|---------------------------|----------------------------------------------------------|
| 200 OK                    | List of requested reports returned successfully.         |
| 204 No Content            | No reports found for the given EORI.                     |
| 400 Bad Request           | The request body is missing or contains an invalid EORI. |
| 500 Internal Server Error | An error occurred while fetching reports.                |

## Setup User API

The `Setup User` API allows clients to initialize or set up a user in the system, typically by providing an EORI (Economic Operators Registration and Identification) number.

- **POST** `/trade-reporting-extracts/eori/setup-user`

### Request Body
The request must be a JSON object containing the EORI to set up:

```json
{
  "eori": "GB123456789014"
}
```

### Description
Initializes a user profile or environment for the given EORI. This endpoint is generally used during onboarding or when a new user needs to be registered in the system.

### Response
Returns a JSON object with the user details. Responds with HTTP status 201 (Created) on success.

#### Example Response
```json
{
  "eori": "GB123456789014",
  "additionalEmails": [
    "user1@example.com",
    "user2@example.com"
  ],
  "authorisedUsers": [
    {
      "name": "John Doe",
      "email": "john.doe@example.com"
    }
  ],
  "companyInformation": {
    "companyName": "Example Ltd",
    "address": "123 Example Street, London, UK"
  },
  "notificationEmail": {
    "address": "notify@example.com",
    "verified": true
  }
}
```

### Response Codes

| Status Code | Description                        |
|-------------|------------------------------------|
| 201 Created | User setup completed successfully. |

## Authorised EORIs API

The `Authorised EORIs` API allows clients to retrieve a list of EORI numbers that the user is authorized to access or act on behalf of.

- **GET** `/trade-reporting-extracts/user/:eori/authorised-eoris`

### Description
Returns a list of EORI numbers associated with the user, indicating which EORIs the user is authorized to use for report requests or other actions.

### Response
The response is a JSON array of EORI numbers or objects containing EORI details.

#### Example Response
```json
[
  "GB123456789014",
  "GB987654321000"
]
```

### Response Codes

| Status Code               | Description                                        |
|---------------------------|----------------------------------------------------|
| 200 OK                    | List of authorised EORIs returned successfully.    |
| 500 Internal Server Error | An error occurred while fetching authorised EORIs. |

## Notify Report Available API

The `Notify Report Available` API is consumed by the SDES service to notify this system when a report file is available for download.

- **POST** `/trade-reporting-extracts/notify-report-available`

### Description
This endpoint receives notifications from SDES when a new report file is available. It is typically called by SDES, not by end users.

### Request Body
The request should be a JSON object containing details about the available file, such as file name, location, and metadata.

#### Example Request
```json
{
  "fileName": "import_report_2025_04.csv",
  "location": "/sdes/files/import_report_2025_04.csv",
  "eori": "GB123456789014",
  "reportType": "importHeader",
  "availableAt": "2025-05-01T10:00:00Z"
}
```

### Response
Returns a JSON object indicating success or failure of the notification processing.

#### Example Response
```json
{
  "status": "success",
  "message": "Notification received and processed."
}
```

### Response Codes

| Status Code     | Description                             |
|-----------------|-----------------------------------------|
| 200 OK          | Notification processed successfully.    |
| 400 Bad Request | The request body is missing or invalid. |

### Notes
- This endpoint is intended for SDES integration.

## Report Status Notification API

The `Report Status Notification` API is consumed by the EIS service to notify this system about the status of a report request.

- **PUT** `/tre/reportstatusnotification/v1`

### Description
This endpoint receives status updates from EIS regarding report requests, such as when a report is being processed, completed, or failed.

### Request Body
The request should be a JSON object containing the report request ID, status, and any relevant details or error messages.

#### Example Request
```json
{
  "requestId": "req-12345",
  "status": "Completed",
  "completedAt": "2025-05-01T10:00:00Z",
  "details": "Report successfully generated."
}
```

### Response
Returns a JSON object indicating the result of the status update processing.

#### Example Response
```json
{
  "status": "success",
  "message": "Status update received."
}
```

### Response Codes

| Status Code     | Description                                                            |
|-----------------|------------------------------------------------------------------------|
| 201 Created     | Report status notification processed successfully.                     |
| 400 Bad Request | The request body is missing, invalid, or required headers are missing. |
| 403 Forbidden   | Authorization failed.                                                  |

### Notes
- This endpoint is intended for EIS integration.

## Helpful Commands

| Command                                | Description                                                                                                 |
|----------------------------------------|-------------------------------------------------------------------------------------------------------------|
| sbt run                                | Runs the service locally                                                                                    |
| sbt clean                              | Cleans code                                                                                                 |
| sbt compile                            | Compiles the code                                                                                           |
| sbt coverage                           | Prints code coverage                                                                                        |
| sbt test                               | Runs unit tests                                                                                             |
| sbt it/test                            | Runs integration tests                                                                                      |
| sbt scalafmtCheckAll                   | Runs code formatting checks based on .scalafmt.conf                                                         |
| sbt scalafmtAll                        | Formats all the necessary files based on .scalafmt.conf                                                     |
| sbt "test/testOnly *TEST_FILE_NAME*"   | Runs tests for a single file                                                                                |
| sbt clean coverage test coverageReport | Generates a unit test coverage report that you can find here target/scala-3.3.5/scoverage-report/index.html |
