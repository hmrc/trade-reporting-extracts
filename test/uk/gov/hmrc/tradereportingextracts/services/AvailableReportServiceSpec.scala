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

import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.*
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json
import play.api.mvc.Results.{BadRequest, NotFound}
import uk.gov.hmrc.crypto.Sensitive.SensitiveString
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tradereportingextracts.config.AppConfig
import uk.gov.hmrc.tradereportingextracts.connectors.{CustomsDataStoreConnector, SDESConnector}
import uk.gov.hmrc.tradereportingextracts.models.*
import uk.gov.hmrc.tradereportingextracts.models.audit.AuditDownloadRequest
import uk.gov.hmrc.tradereportingextracts.models.eis.EisReportStatusRequest
import uk.gov.hmrc.tradereportingextracts.models.sdes.{FileAvailableMetadataItem, FileAvailableResponse}

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

class AvailableReportServiceSpec extends AnyWordSpec with Matchers with ScalaFutures with MockitoSugar {
  implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val hc: HeaderCarrier    = HeaderCarrier()
  "getAvailableReports" should {
    "return empty availableUserReports when no report requests exist" in {
      val mockReportRequestService: ReportRequestService = mock[ReportRequestService]
      val mockAppConfig: AppConfig                       = mock[AppConfig]
      val mockSDESConnector                              = mock[SDESConnector]
      val mockCustomsDataStoreConnector                  = mock[CustomsDataStoreConnector]
      val mockAuditService                               = mock[AuditService]
      val eori                                           = "GB123456789000"
      when(mockSDESConnector.fetchAvailableReportFileUrl(any())(any()))
        .thenReturn(Future.successful(Seq.empty))
      when(mockCustomsDataStoreConnector.getEoriHistory(any()))
        .thenReturn(Future.successful(EoriHistoryResponse(Seq(EoriHistory(eori, Some("2023-01-01"), None)))))
      when(mockReportRequestService.getAvailableReportsByHistory(any())(using any()))
        .thenReturn(Future.successful(Seq.empty))

      val service =
        new AvailableReportService(
          mockReportRequestService,
          mockSDESConnector,
          mockCustomsDataStoreConnector,
          mockAuditService,
          mockAppConfig
        )(using
          ExecutionContext.global
        )

      whenReady(service.getAvailableReports(eori)(using hc)) { result =>
        result.availableUserReports       shouldBe Some(Seq.empty)
        result.availableThirdPartyReports shouldBe Some(Seq.empty)
      }
    }

    "return availableUserReports with actions when report requests and SDES responses exist" in {
      val mockReportRequestService      = mock[ReportRequestService]
      val mockSDESConnector             = mock[SDESConnector]
      val mockCustomsDataStoreConnector = mock[CustomsDataStoreConnector]
      val eori                          = "GB123456789000"
      val reportRequestId               = "req-1"
      val mockAppConfig: AppConfig      = mock[AppConfig]
      val mockAuditService              = mock[AuditService]
      val fileNotification              = FileNotification(
        "file.csv",
        123L,
        30,
        "CSV",
        "Core-ID",
        "req-id-456",
        "IMPORTS-ITEM-REPORT",
        "1",
        "true",
        ""
      )
      val reportRequest                 = ReportRequest(
        reportRequestId = reportRequestId,
        correlationId = "ABCD-DEFG",
        reportName = "Jan Report",
        requesterEORI = "GB123456789000",
        eoriRole = EoriRole.TRADER,
        reportEORIs = Array("GB123456789000", "EORI2").toIndexedSeq,
        userEmail = Some(SensitiveString("test@example.com")),
        recipientEmails = Seq(SensitiveString("email1@example.com"), SensitiveString("email2@example.com")),
        reportTypeName = ReportTypeName.IMPORTS_ITEM_REPORT,
        reportStart = Instant.parse("2023-01-01T00:00:00Z"),
        reportEnd = Instant.parse("2023-12-31T23:59:59Z"),
        createDate = Instant.parse("2023-01-01T10:00:00Z"),
        notifications = Seq(
          EisReportStatusRequest(
            applicationComponent = EisReportStatusRequest.ApplicationComponent.CDAP,
            statusType = EisReportStatusRequest.StatusType.INFORMATION,
            statusCode = StatusCode.FILESENT.toString,
            statusMessage = "Message1",
            statusTimestamp = Instant.parse("2023-01-01T10:00:00Z").toString
          )
        ),
        fileNotifications = Some(Seq(fileNotification)),
        updateDate = Instant.parse("2023-01-03T10:00:00Z")
      )
      val sdesResponse                  = Seq(
        FileAvailableResponse(
          filename = "file.csv",
          downloadURL = "http://example.com/file.csv",
          fileSize = 123L,
          metadata = Seq(FileAvailableMetadataItem.MDTPReportRequestIDMetadataItem(reportRequestId))
        )
      )

      when(mockSDESConnector.fetchAvailableReportFileUrl(any())(any()))
        .thenReturn(Future.successful(sdesResponse))
      when(mockCustomsDataStoreConnector.getEoriHistory(any()))
        .thenReturn(Future.successful(EoriHistoryResponse(Seq(EoriHistory(eori, Some("2023-01-01"), None)))))
      when(mockReportRequestService.getAvailableReportsByHistory(any())(using any()))
        .thenReturn(Future.successful(Seq(reportRequest)))

      val service =
        new AvailableReportService(
          mockReportRequestService,
          mockSDESConnector,
          mockCustomsDataStoreConnector,
          mockAuditService,
          mockAppConfig
        )

      whenReady(service.getAvailableReports(eori)) { result =>
        result.availableUserReports should not be empty
        val userReport = result.availableUserReports.get.head
        userReport.referenceNumber shouldBe reportRequestId
      }
    }

    "return availableUserReports when report requests and SDES responses in multiples exist" in {
      val mockReportRequestService      = mock[ReportRequestService]
      val mockSDESConnector             = mock[SDESConnector]
      val mockCustomsDataStoreConnector = mock[CustomsDataStoreConnector]
      val eori                          = "GB123456789000"
      val reportRequestId               = "req-1"
      val mockAppConfig: AppConfig      = mock[AppConfig]
      val mockAuditService              = mock[AuditService]
      val fileNotification              = FileNotification(
        "file.csv",
        123L,
        30,
        "CSV",
        "Core-ID",
        "req-id-456",
        "IMPORTS-ITEM-REPORT",
        "1",
        "true",
        ""
      )
      val reportRequest                 = ReportRequest(
        reportRequestId = reportRequestId,
        correlationId = "ABCD-DEFG",
        reportName = "Jan Report",
        requesterEORI = "GB123456789000",
        eoriRole = EoriRole.TRADER,
        reportEORIs = Array("GB123456789000", "EORI2").toIndexedSeq,
        userEmail = Some(SensitiveString("test@example.com")),
        recipientEmails = Seq(SensitiveString("email1@example.com"), SensitiveString("email2@example.com")),
        reportTypeName = ReportTypeName.IMPORTS_ITEM_REPORT,
        reportStart = Instant.parse("2023-01-01T00:00:00Z"),
        reportEnd = Instant.parse("2023-12-31T23:59:59Z"),
        createDate = Instant.parse("2023-01-01T10:00:00Z"),
        notifications = Seq(
          EisReportStatusRequest(
            applicationComponent = EisReportStatusRequest.ApplicationComponent.CDAP,
            statusType = EisReportStatusRequest.StatusType.INFORMATION,
            statusCode = StatusCode.FILESENT.toString,
            statusMessage = "Message1",
            statusTimestamp = Instant.parse("2023-01-01T10:00:00Z").toString
          )
        ),
        fileNotifications = Some(Seq(fileNotification)),
        updateDate = Instant.parse("2023-01-03T10:00:00Z")
      )
      val sdesResponse                  = Seq(
        FileAvailableResponse(
          filename = "file.csv",
          downloadURL = "http://example.com/file.csv",
          fileSize = 123L,
          metadata = Seq(FileAvailableMetadataItem.MDTPReportRequestIDMetadataItem(reportRequestId))
        ),
        FileAvailableResponse(
          filename = "file.csv",
          downloadURL = "http://example.com/file.csv",
          fileSize = 123L,
          metadata = Seq(FileAvailableMetadataItem.MDTPReportRequestIDMetadataItem("9999"))
        )
      )

      when(mockSDESConnector.fetchAvailableReportFileUrl(any())(any()))
        .thenReturn(Future.successful(sdesResponse))
      when(mockCustomsDataStoreConnector.getEoriHistory(any()))
        .thenReturn(Future.successful(EoriHistoryResponse(Seq(EoriHistory(eori, Some("2023-01-01"), None)))))
      when(mockReportRequestService.getAvailableReportsByHistory(any())(using any()))
        .thenReturn(Future.successful(Seq(reportRequest)))

      val service =
        new AvailableReportService(
          mockReportRequestService,
          mockSDESConnector,
          mockCustomsDataStoreConnector,
          mockAuditService,
          mockAppConfig
        )

      whenReady(service.getAvailableReports(eori)) { result =>
        result.availableUserReports should not be empty
        val userReport = result.availableUserReports.get.head
        userReport.referenceNumber shouldBe reportRequestId
        userReport.action.size     shouldBe 1
      }
    }
  }

  "getAvailableReportsCount" should {
    "return the count from reportRequestService" in {
      val mockReportRequestService      = mock[ReportRequestService]
      val mockSDESConnector             = mock[SDESConnector]
      val mockCustomsDataStoreConnector = mock[CustomsDataStoreConnector]
      val eori                          = "GB123456789000"
      val mockAppConfig: AppConfig      = mock[AppConfig]
      val mockAuditService              = mock[AuditService]
      when(mockReportRequestService.countAvailableReports(any())(using any()))
        .thenReturn(Future.successful(5L))

      val service =
        new AvailableReportService(
          mockReportRequestService,
          mockSDESConnector,
          mockCustomsDataStoreConnector,
          mockAuditService,
          mockAppConfig
        )

      whenReady(service.getAvailableReportsCount(eori)) { count =>
        count shouldBe 5L
      }
    }

    "return 0 if reportRequestService.countAvailableReports fails" in {
      val mockReportRequestService      = mock[ReportRequestService]
      val mockSDESConnector             = mock[SDESConnector]
      val mockCustomsDataStoreConnector = mock[CustomsDataStoreConnector]
      val eori                          = "GB123456789000"
      val mockAppConfig: AppConfig      = mock[AppConfig]
      val mockAuditService              = mock[AuditService]
      when(mockReportRequestService.countAvailableReports(any())(using any()))
        .thenReturn(Future.failed(new RuntimeException("error - this is a error generated for testing!!!")))

      val service =
        new AvailableReportService(
          mockReportRequestService,
          mockSDESConnector,
          mockCustomsDataStoreConnector,
          mockAuditService,
          mockAppConfig
        )

      whenReady(service.getAvailableReportsCount(eori)) { count =>
        count shouldBe 0L
      }
    }
  }

  "findByReportRequestId" should {
    "return a report when found" in {
      val mockReportRequestService      = mock[ReportRequestService]
      val mockSDESConnector             = mock[SDESConnector]
      val mockCustomsDataStoreConnector = mock[CustomsDataStoreConnector]
      val mockAppConfig                 = mock[AppConfig]
      val mockAuditService              = mock[AuditService]
      val service                       =
        new AvailableReportService(
          mockReportRequestService,
          mockSDESConnector,
          mockCustomsDataStoreConnector,
          mockAuditService,
          mockAppConfig
        )

      val reportId         = "report-123"
      val fileNotification = FileNotification(
        "file.csv",
        123L,
        30,
        "CSV",
        "Core-ID",
        "req-id-456",
        "IMPORTS-ITEM-REPORT",
        "1",
        "true",
        ""
      )
      val reportRequest    = ReportRequest(
        reportRequestId = reportId,
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
            statusType = EisReportStatusRequest.StatusType.INFORMATION,
            statusCode = StatusCode.FILESENT.toString,
            statusMessage = "Message1",
            statusTimestamp = Instant.parse("2023-01-01T10:00:00Z").toString
          )
        ),
        fileNotifications = Some(Seq(fileNotification)),
        updateDate = Instant.parse("2023-01-03T10:00:00Z")
      )

      when(mockReportRequestService.get(eqTo(reportId))(any())).thenReturn(Future.successful(Some(reportRequest)))
      val result = service.findByReportRequestId(reportId).futureValue

      result shouldBe Some(reportRequest)
    }

    "return None when a report is not found" in {
      val mockReportRequestService      = mock[ReportRequestService]
      val mockSDESConnector             = mock[SDESConnector]
      val mockCustomsDataStoreConnector = mock[CustomsDataStoreConnector]
      val mockAppConfig                 = mock[AppConfig]
      val mockAuditService              = mock[AuditService]
      val service                       =
        new AvailableReportService(
          mockReportRequestService,
          mockSDESConnector,
          mockCustomsDataStoreConnector,
          mockAuditService,
          mockAppConfig
        )

      val reportId = "report-404"
      when(mockReportRequestService.get(eqTo(reportId))(any())).thenReturn(Future.successful(None))

      val result = service.findByReportRequestId(reportId).futureValue

      result shouldBe None
    }
  }

  "processReportDownloadAudit" should {
    val reportId = "report-123"
    val fileName = "file1.csv"
    val fileUrl  = "http://example.com/file1.csv"
    val eori     = "GB123456789000"
    val fileSize = 1024L

    val jsonBody = Json.obj(
      "reportReference" -> reportId,
      "fileName"        -> fileName,
      "fileUrl"         -> fileUrl
    )

    val fileNotification = FileNotification(
      fileName,
      fileSize,
      30,
      "CSV",
      "Core-ID",
      "req-id-456",
      "IMPORTS-ITEM-REPORT",
      "1",
      "true",
      ""
    )
    val reportRequest    = ReportRequest(
      reportRequestId = reportId,
      correlationId = "ABCD-DEFG",
      reportName = "Jan Report",
      requesterEORI = eori,
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
          statusType = EisReportStatusRequest.StatusType.INFORMATION,
          statusCode = StatusCode.FILESENT.toString,
          statusMessage = "Message1",
          statusTimestamp = Instant.parse("2023-01-01T10:00:00Z").toString
        )
      ),
      fileNotifications = Some(Seq(fileNotification)),
      updateDate = Instant.parse("2023-01-03T10:00:00Z")
    )

    "successfully process a valid request and send an audit event" in {
      val mockReportRequestService      = mock[ReportRequestService]
      val mockSDESConnector             = mock[SDESConnector]
      val mockCustomsDataStoreConnector = mock[CustomsDataStoreConnector]
      val mockAppConfig                 = mock[AppConfig]
      val mockAuditService              = mock[AuditService]
      val service                       =
        new AvailableReportService(
          mockReportRequestService,
          mockSDESConnector,
          mockCustomsDataStoreConnector,
          mockAuditService,
          mockAppConfig
        )

      when(mockReportRequestService.get(eqTo(reportId))(any()))
        .thenReturn(Future.successful(Some(reportRequest)))

      val result = service.processReportDownloadAudit(jsonBody.asOpt[AuditDownloadRequest]).futureValue
      result shouldBe Right(())
    }

    "return NotFound when the report request is not found" in {
      val mockReportRequestService      = mock[ReportRequestService]
      val mockSDESConnector             = mock[SDESConnector]
      val mockCustomsDataStoreConnector = mock[CustomsDataStoreConnector]
      val mockAppConfig                 = mock[AppConfig]
      val mockAuditService              = mock[AuditService]
      val service                       =
        new AvailableReportService(
          mockReportRequestService,
          mockSDESConnector,
          mockCustomsDataStoreConnector,
          mockAuditService,
          mockAppConfig
        )

      when(mockReportRequestService.get(eqTo(reportId))(any())).thenReturn(Future.successful(None))

      val result = service.processReportDownloadAudit(jsonBody.asOpt[AuditDownloadRequest]).futureValue
      result shouldBe Left(NotFound(s"Report with reference $reportId not found"))
    }

    "return BadRequest when the file is not found in the report notifications" in {
      val mockReportRequestService      = mock[ReportRequestService]
      val mockSDESConnector             = mock[SDESConnector]
      val mockCustomsDataStoreConnector = mock[CustomsDataStoreConnector]
      val mockAppConfig                 = mock[AppConfig]
      val mockAuditService              = mock[AuditService]
      val service                       =
        new AvailableReportService(
          mockReportRequestService,
          mockSDESConnector,
          mockCustomsDataStoreConnector,
          mockAuditService,
          mockAppConfig
        )

      val reportWithDifferentFile =
        reportRequest.copy(fileNotifications = Some(Seq(fileNotification.copy(fileName = "other.csv"))))
      when(mockReportRequestService.get(eqTo(reportId))(any()))
        .thenReturn(Future.successful(Some(reportWithDifferentFile)))

      val result = service.processReportDownloadAudit(jsonBody.asOpt[AuditDownloadRequest]).futureValue
      result shouldBe Left(BadRequest(s"File $fileName not found in report $reportId"))
    }

    "return BadRequest for an empty request body" in {
      val mockReportRequestService      = mock[ReportRequestService]
      val mockSDESConnector             = mock[SDESConnector]
      val mockCustomsDataStoreConnector = mock[CustomsDataStoreConnector]
      val mockAppConfig                 = mock[AppConfig]
      val mockAuditService              = mock[AuditService]
      val service                       =
        new AvailableReportService(
          mockReportRequestService,
          mockSDESConnector,
          mockCustomsDataStoreConnector,
          mockAuditService,
          mockAppConfig
        )

      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result                     = service.processReportDownloadAudit(None).futureValue

      result shouldBe Left(BadRequest("Missing or invalid request parameters"))
    }
  }
}
