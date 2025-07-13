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

package uk.gov.hmrc.tradereportingextracts.services

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.hmrc.crypto.Sensitive.SensitiveString
import uk.gov.hmrc.tradereportingextracts.models.*
import uk.gov.hmrc.tradereportingextracts.models.eis.EisReportStatusRequest
import uk.gov.hmrc.tradereportingextracts.models.eis.EisReportStatusRequest.StatusType

import java.time.Instant

class ReportRequestServiceSpec extends AnyWordSpec with Matchers {

  val service = new ReportRequestService(null, null) // nulls are fine here since we’re only testing private logic

  "determineReportStatus" should {

    "return COMPLETE when fileNotifications contain the final part" in {
      val fileNotifications = Some(
        Seq(
          FileNotification(
            "file1",
            1000,
            7,
            "CSV",
            "x1",
            "r1",
            "IMPORTS-ITEM-REPORT",
            "1",
            "",
            ""
          ),
          FileNotification(
            "file2",
            1000,
            7,
            "CSV",
            "x2",
            "r1",
            "IMPORTS-ITEM-REPORT",
            "2",
            "",
            ""
          ),
          FileNotification(
            "file3",
            1000,
            7,
            "CSV",
            "x3",
            "r1",
            "IMPORTS-ITEM-REPORT",
            "3",
            "true",
            ""
          )
        )
      )

      val reportRequest = ReportRequest(
        "id",
        "corr",
        "Report",
        "GB123",
        EoriRole.TRADER,
        Seq("GB123"),
        Some(SensitiveString("test@example.com")),
        Seq("test@example.com"),
        ReportTypeName.IMPORTS_ITEM_REPORT,
        Instant.now(),
        Instant.now(),
        Instant.now(),
        Seq.empty,
        fileNotifications,
        Instant.now()
      )

      service.invokePrivateMethod("determineReportStatus", reportRequest) shouldBe ReportStatus.COMPLETE
    }

    "return ERROR when not complete and at least one notification has ERROR status" in {
      val notifications = Seq(
        EisReportStatusRequest(
          EisReportStatusRequest.ApplicationComponent.TRE,
          StatusCode.FAILED.toString,
          "Failure",
          "2025-06-04T12:00:00Z",
          StatusType.ERROR
        )
      )

      val reportRequest = ReportRequest(
        "id",
        "corr",
        "Report",
        "GB123",
        EoriRole.TRADER,
        Seq("GB123"),
        Some(SensitiveString("test@example.com")),
        Seq("test@example.com"),
        ReportTypeName.IMPORTS_ITEM_REPORT,
        Instant.now(),
        Instant.now(),
        Instant.now(),
        notifications,
        None,
        Instant.now
      )

      service.invokePrivateMethod("determineReportStatus", reportRequest) shouldBe ReportStatus.ERROR
    }

    "return NO_DATA_AVAILABLE when not complete and at least one notification has ERROR status with FILENOREC status code" in {
      val notifications = Seq(
        EisReportStatusRequest(
          EisReportStatusRequest.ApplicationComponent.TRE,
          StatusCode.FILENOREC.toString,
          "Failure",
          "2025-06-04T12:00:00Z",
          StatusType.ERROR
        )
      )

      val reportRequest = ReportRequest(
        "id",
        "corr",
        "Report",
        "GB123",
        EoriRole.TRADER,
        Seq("GB123"),
        Some(SensitiveString("test@example.com")),
        Seq("test@example.com"),
        ReportTypeName.IMPORTS_ITEM_REPORT,
        Instant.now(),
        Instant.now(),
        Instant.now(),
        notifications,
        None,
        Instant.now
      )

      service.invokePrivateMethod("determineReportStatus", reportRequest) shouldBe ReportStatus.NO_DATA_AVAILABLE
    }

    "return IN_PROGRESS when all notifications are INFORMATION and report is not complete" in {
      val notifications = Seq(
        EisReportStatusRequest(
          EisReportStatusRequest.ApplicationComponent.TRE,
          "INFO001",
          "Processing",
          "2025-06-04T12:00:00Z",
          StatusType.INFORMATION
        )
      )

      val reportRequest = ReportRequest(
        "id",
        "corr",
        "Report",
        "GB123",
        EoriRole.TRADER,
        Seq("GB123"),
        Some(SensitiveString("test@example.com")),
        Seq("test@example.com"),
        ReportTypeName.IMPORTS_ITEM_REPORT,
        Instant.now(),
        Instant.now(),
        Instant.now(),
        notifications,
        None,
        Instant.now
      )

      service.invokePrivateMethod("determineReportStatus", reportRequest) shouldBe ReportStatus.IN_PROGRESS
    }

    "return IN_PROGRESS when there are no notifications and report is not complete" in {
      val reportRequest = ReportRequest(
        "id",
        "corr",
        "Report",
        "GB123",
        EoriRole.TRADER,
        Seq("GB123"),
        Some(SensitiveString("test@example.com")),
        Seq("test@example.com"),
        ReportTypeName.IMPORTS_ITEM_REPORT,
        Instant.now(),
        Instant.now(),
        Instant.now(),
        Seq.empty,
        None,
        Instant.now
      )

      service.invokePrivateMethod("determineReportStatus", reportRequest) shouldBe ReportStatus.IN_PROGRESS
    }
  }

  // Helper to invoke private method
  extension (service: ReportRequestService)
    def invokePrivateMethod(methodName: String, reportRequest: ReportRequest): ReportStatus = {
      val method = classOf[ReportRequestService].getDeclaredMethod(methodName, classOf[ReportRequest])
      method.setAccessible(true)
      method.invoke(service, reportRequest).asInstanceOf[ReportStatus]
    }
}
