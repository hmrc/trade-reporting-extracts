/*
 * Copyright 2025 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.tradereportingextracts.models

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.must.Matchers
import uk.gov.hmrc.crypto.Sensitive.SensitiveString

import java.time.Instant
import uk.gov.hmrc.tradereportingextracts.models.eis.EisReportStatusRequest

class ReportRequestSpec extends AnyWordSpec with Matchers {

  val reportRequest1: ReportRequest = ReportRequest(
    reportRequestId = "REQ00001",
    correlationId = "ABCD-DEFG",
    reportName = "Jan Report",
    requesterEORI = "GB0019",
    eoriRole = EoriRole.TRADER,
    reportEORIs = Array("EORI1", "EORI2").toIndexedSeq,
    userEmail = Some(SensitiveString("test@example.com")),
    recipientEmails = Seq(SensitiveString("email1@example.com"), SensitiveString("email2@example.com")),
    reportTypeName = ReportTypeName.IMPORTS_ITEM_REPORT,
    reportStart = Instant.parse("2023-01-01T00:00:00Z"),
    reportEnd = Instant.parse("2023-12-31T23:59:59Z"),
    createDate = Instant.parse("2023-01-01T10:00:00Z"),
    notifications = Seq(
      EisReportStatusRequest(
        applicationComponent = EisReportStatusRequest.ApplicationComponent.CDAP,
        statusCode = StatusCode.FILESENT.toString,
        statusMessage = "Report generated successfully",
        statusTimestamp = "2023-01-01T10:00:00Z",
        statusType = EisReportStatusRequest.StatusType.INFORMATION
      )
    ),
    fileNotifications = Some(
      Seq(
        FileNotification(
          fileName = "example.txt",
          fileSize = 1024,
          retentionDays = 30,
          fileType = "CSV",
          mDTPReportXCorrelationID = "X-Correlation-ID",
          mDTPReportRequestID = "Request-ID",
          mDTPReportTypeName = "IMPORTS-ITEM-REPORT",
          reportFilesParts = "Part1",
          reportLastFile = "LastFile",
          fileCreationTimestamp = "2023-01-01T10:00:00Z"
        )
      )
    ),
    updateDate = Instant.parse("2023-01-03T10:00:00Z")
  )

  "ReportRequest equality" should {
    "return true for identical instances" in {
      val reportRequest2 = reportRequest1.copy()
      reportRequest1 mustEqual reportRequest2
    }

    "return false for instances with different values" in {
      val reportRequest2 = reportRequest1.copy(reportName = "Feb Report")
      reportRequest1 must not equal reportRequest2
    }
  }
}
