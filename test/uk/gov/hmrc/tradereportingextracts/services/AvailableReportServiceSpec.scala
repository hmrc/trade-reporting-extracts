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

import org.mockito.ArgumentMatchers.*
import org.mockito.Mockito.*
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tradereportingextracts.config.AppConfig
import uk.gov.hmrc.tradereportingextracts.connectors.SDESConnector
import uk.gov.hmrc.tradereportingextracts.models.eis.EisReportStatusRequest
import uk.gov.hmrc.tradereportingextracts.models.sdes.{FileAvailableMetadataItem, FileAvailableResponse, FileNotificationMetadata}
import uk.gov.hmrc.tradereportingextracts.models.*

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

class AvailableReportServiceSpec extends AnyWordSpec with Matchers with ScalaFutures with MockitoSugar {
  implicit val ec: ExecutionContext = ExecutionContext.global

  "getAvailableReports" should {
    "return empty availableUserReports when no report requests exist" in {
      val mockReportRequestService: ReportRequestService = mock[ReportRequestService]
      val mockAppConfig: AppConfig                       = mock[AppConfig]
      val mockSDESConnector                              = mock[SDESConnector]
      val eori                                           = "GB123456789000"
      when(mockSDESConnector.fetchAvailableReportFileUrl(any())(any()))
        .thenReturn(Future.successful(Seq.empty))
      when(mockReportRequestService.getAvailableReports(any())(using any()))
        .thenReturn(Future.successful(Seq.empty))

      val service = new AvailableReportService(mockReportRequestService, mockSDESConnector, mockAppConfig)(using
        ExecutionContext.global
      )

      whenReady(service.getAvailableReports(eori)(using hc = HeaderCarrier())) { result =>
        result.availableUserReports       shouldBe Some(Seq.empty)
        result.availableThirdPartyReports shouldBe None
      }
    }

    "return availableUserReports with actions when report requests and SDES responses exist" in {
      val mockReportRequestService                 = mock[ReportRequestService]
      val mockSDESConnector                        = mock[SDESConnector]
      val eori                                     = "GB123456789000"
      var reportRequestId                          = "req-1"
      val mockAppConfig: AppConfig                 = mock[AppConfig]
      val metadata: List[FileNotificationMetadata] = List(
        FileNotificationMetadata.RetentionDaysMetadataItem("30"),
        FileNotificationMetadata.FileTypeMetadataItem("CSV"),
        FileNotificationMetadata.EORIMetadataItem("GB123456789000"),
        FileNotificationMetadata.MDTPReportXCorrelationIDMetadataItem("corr-id-123"),
        FileNotificationMetadata.MDTPReportRequestIDMetadataItem("req-id-456"),
        FileNotificationMetadata.MDTPReportTypeNameMetadataItem("IMPORTS_ITEM_REPORT"),
        FileNotificationMetadata.ReportFilesPartsMetadataItem("1")
      )
      val fileNotification                         = FileNotification(
        "file.csv",
        123L,
        30,
        "CSV",
        "Core-ID",
        "req-id-456",
        "IMPORTS-ITEM-REPORT",
        "1Of1",
        "last",
        ""
      )
      val reportRequest                            = ReportRequest(
        reportRequestId = reportRequestId,
        correlationId = "ABCD-DEFG",
        reportName = "Jan Report",
        requesterEORI = "GB0019",
        eoriRole = EoriRole.TRADER,
        reportEORIs = Array("EORI1", "EORI2"),
        userEmail = Some("test@example.com"),
        recipientEmails = Array("email1@example.com", "email2@example.com"),
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
      val sdesResponse                             = Seq(
        FileAvailableResponse(
          filename = "file.csv",
          downloadURL = "http://example.com/file.csv",
          fileSize = 123L,
          metadata = Seq(FileAvailableMetadataItem.MDTPReportRequestIDMetadataItem(reportRequestId))
        )
      )

      when(mockSDESConnector.fetchAvailableReportFileUrl(any())(any()))
        .thenReturn(Future.successful(sdesResponse))
      when(mockReportRequestService.getAvailableReports(any())(using any()))
        .thenReturn(Future.successful(Seq(reportRequest)))

      val service                    = new AvailableReportService(mockReportRequestService, mockSDESConnector, mockAppConfig)
      implicit val hc: HeaderCarrier = HeaderCarrier()

      whenReady(service.getAvailableReports(eori)) { result =>
        result.availableUserReports should not be empty
        val userReport = result.availableUserReports.get.head
        userReport.referenceNumber shouldBe reportRequestId
      }
    }
  }

  "getAvailableReportsCount" should {
    "return the count from reportRequestService" in {
      val mockReportRequestService = mock[ReportRequestService]
      val mockSDESConnector        = mock[SDESConnector]
      val eori                     = "GB123456789000"
      val mockAppConfig: AppConfig = mock[AppConfig]
      when(mockReportRequestService.countAvailableReports(any())(using any()))
        .thenReturn(Future.successful(5L))

      val service = new AvailableReportService(mockReportRequestService, mockSDESConnector, mockAppConfig)

      whenReady(service.getAvailableReportsCount(eori)) { count =>
        count shouldBe 5L
      }
    }

    "return 0 if reportRequestService.countAvailableReports fails" in {
      val mockReportRequestService = mock[ReportRequestService]
      val mockSDESConnector        = mock[SDESConnector]
      val eori                     = "GB123456789000"
      val mockAppConfig: AppConfig = mock[AppConfig]

      when(mockReportRequestService.countAvailableReports(any())(using any()))
        .thenReturn(Future.failed(new RuntimeException("error")))

      val service = new AvailableReportService(mockReportRequestService, mockSDESConnector, mockAppConfig)

      whenReady(service.getAvailableReportsCount(eori)) { count =>
        count shouldBe 0L
      }
    }
  }
}
