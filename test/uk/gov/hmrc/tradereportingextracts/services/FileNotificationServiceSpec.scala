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

import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.*
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.wordspec.AnyWordSpec
import play.api.http.Status.{BAD_REQUEST, CREATED, NOT_FOUND}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tradereportingextracts.connectors.EmailConnector
import uk.gov.hmrc.tradereportingextracts.models.sdes.{FileNotificationMetadata, FileNotificationResponse}
import uk.gov.hmrc.tradereportingextracts.models.{FileNotification as TreFileNotification, FileType, ReportTypeName}
import uk.gov.hmrc.tradereportingextracts.models.ReportRequest
import uk.gov.hmrc.tradereportingextracts.models.ReportStatus.{COMPLETE, IN_PROGRESS}
import uk.gov.hmrc.tradereportingextracts.utils.WireMockHelper

import scala.concurrent.{ExecutionContext, Future}

class FileNotificationServiceSpec
    extends AnyWordSpec
    with Matchers
    with MockitoSugar
    with ScalaFutures
    with WireMockHelper {

  implicit val hc: HeaderCarrier    = HeaderCarrier()
  implicit val ec: ExecutionContext = ExecutionContext.global

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockReportRequestService)
    reset(mockEmailConnector)
  }

  val mockReportRequestService: ReportRequestService = mock[ReportRequestService]
  val mockEmailConnector: EmailConnector             = mock[EmailConnector]
  val service                                        = new FileNotificationService(mockReportRequestService, mockEmailConnector)

  val fileNotification = FileNotificationResponse(
    eori = "GB123456789012",
    fileName = "testFileName",
    fileSize = 12345,
    metadata = List(
      FileNotificationMetadata.RetentionDaysMetadataItem("30"),
      FileNotificationMetadata.FileTypeMetadataItem("CSV"),
      FileNotificationMetadata.EORIMetadataItem("GB123456789012"),
      FileNotificationMetadata.MDTPReportXCorrelationIDMetadataItem("asfd-asdf-asdf"),
      FileNotificationMetadata.MDTPReportRequestIDMetadataItem("RE123456"),
      FileNotificationMetadata.MDTPReportTypeNameMetadataItem("IMPORTS_HEADER_REPORT"),
      FileNotificationMetadata.ReportFilesPartsMetadataItem("1of2")
    )
  )

  val reportRequest = ReportRequest(
    reportRequestId = "RE123456",
    correlationId = "corr-1",
    reportName = "name",
    requesterEORI = "GB123456789012",
    eoriRole = null,
    reportEORIs = Seq("GB123456789012"),
    recipientEmails = Seq("test@example.com"),
    reportTypeName = ReportTypeName.IMPORTS_HEADER_REPORT,
    reportStart = null,
    reportEnd = null,
    createDate = null,
    notifications = Seq.empty,
    fileNotifications = None,
    linkAvailableTime = null
  )

  "FileNotificationService.processFileNotification" should {

    "return CREATED and update reportRequest when found and no existing fileNotifications" in {
      when(mockReportRequestService.get(eqTo("RE123456"))(any()))
        .thenReturn(Future.successful(Some(reportRequest)))
      when(mockReportRequestService.update(any())(any()))
        .thenReturn(Future.successful(true))

      val result = service.processFileNotification(fileNotification)
      whenReady(result) { case (status, message) =>
        status  shouldBe CREATED
        message shouldBe "Created"
        val captor  = ArgumentCaptor.forClass(classOf[ReportRequest])
        verify(mockReportRequestService).update(captor.capture())(any())
        val updated = captor.getValue
        updated.fileNotifications.get.size          shouldBe 1
        updated.fileNotifications.get.head.fileName shouldBe "testFileName"
      }
    }

    "append to existing fileNotifications" in {
      reset(mockReportRequestService)

      val existing = reportRequest.copy(fileNotifications =
        Some(
          Seq(
            TreFileNotification(
              fileName = "oldFile",
              fileSize = 1,
              retentionDays = 1,
              fileType = "CSV",
              mDTPReportXCorrelationID = "x",
              mDTPReportRequestID = "RE123456",
              mDTPReportTypeName = "IMPORTS-HEADER-REPORT",
              reportFilesParts = "1of1",
              reportLastFile = "last",
              fileCreationTimestamp = "2025-01-01T00:00:00Z"
            )
          )
        )
      )
      when(mockReportRequestService.get(eqTo("RE123456"))(any()))
        .thenReturn(Future.successful(Some(existing)))
      when(mockReportRequestService.update(any())(any()))
        .thenReturn(Future.successful(true))

      val result = service.processFileNotification(fileNotification)
      whenReady(result) { case (status, message) =>
        status shouldBe CREATED
        val captor  = ArgumentCaptor.forClass(classOf[ReportRequest])
        verify(mockReportRequestService).update(captor.capture())(any())
        val updated = captor.getValue
        updated.fileNotifications.get.size shouldBe 2
      }
    }

    "return NOT_FOUND if reportRequest is not found" in {
      when(mockReportRequestService.get(eqTo("RE123456"))(any()))
        .thenReturn(Future.successful(None))

      val result = service.processFileNotification(fileNotification)
      whenReady(result) { case (status, message) =>
        status shouldBe NOT_FOUND
        message  should include("ReportRequest not found")
      }
    }

    "return BAD_REQUEST if MDTPReportRequestID is missing" in {
      val noIdNotification = fileNotification.copy(
        metadata =
          fileNotification.metadata.filterNot(_.isInstanceOf[FileNotificationMetadata.MDTPReportRequestIDMetadataItem])
      )
      val result           = service.processFileNotification(noIdNotification)
      whenReady(result) { case (status, message) =>
        status shouldBe BAD_REQUEST
        message  should include("report-requestID not found")
      }
    }

    "call emailConnector.sendEmailRequest for each recipient when status is COMPLETE" in {
      val reportWithEmails = reportRequest.copy(recipientEmails = Seq("a@example.com", "b@example.com"))
      when(mockReportRequestService.get(eqTo("RE123456"))(any()))
        .thenReturn(Future.successful(Some(reportWithEmails)))
      when(mockReportRequestService.update(any())(any()))
        .thenReturn(Future.successful(true))
      when(mockReportRequestService.determineReportStatus(any())).thenReturn(COMPLETE)
      when(mockEmailConnector.sendEmailRequest(any(), any(), any())(any()))
        .thenReturn(Future.successful(()))

      val result = service.processFileNotification(fileNotification)
      whenReady(result) { case (status, message) =>
        verify(mockEmailConnector, times(2)).sendEmailRequest(
          eqTo("tre_report_available"),
          any(),
          eqTo(Map("reportRequestId" -> "XXXXX456"))
        )(any())
      }
    }

    "not call emailConnector.sendEmailRequest when status is NOT complete" in {
      val reportWithEmails = reportRequest.copy(recipientEmails = Seq("a@example.com", "b@example.com"))
      when(mockReportRequestService.get(eqTo("RE123456"))(any()))
        .thenReturn(Future.successful(Some(reportWithEmails)))
      when(mockReportRequestService.update(any())(any()))
        .thenReturn(Future.successful(true))
      when(mockReportRequestService.determineReportStatus(any())).thenReturn(IN_PROGRESS)

      val result = service.processFileNotification(fileNotification)
      whenReady(result) { case (status, message) =>
        verify(mockEmailConnector, times(0)).sendEmailRequest(
          eqTo("tre_report_available"),
          any(),
          eqTo(Map("reportRequestId" -> "RE123456"))
        )(any())
      }
    }

    "mask the first 5 digits of reportRequestId in params when sending email" in {
      val maskedReportRequest = reportRequest.copy(
        reportRequestId = "12345-6789",
        recipientEmails = Seq("masked@example.com")
      )
      when(mockReportRequestService.get(eqTo("12345-6789"))(any()))
        .thenReturn(Future.successful(Some(maskedReportRequest)))
      when(mockReportRequestService.update(any())(any()))
        .thenReturn(Future.successful(true))
      when(mockReportRequestService.determineReportStatus(any())).thenReturn(COMPLETE)
      when(mockEmailConnector.sendEmailRequest(any(), any(), any())(any()))
        .thenReturn(Future.successful(()))

      val result = service.processFileNotification(
        fileNotification.copy(
          metadata = fileNotification.metadata.map {
            case FileNotificationMetadata.MDTPReportRequestIDMetadataItem(_) =>
              FileNotificationMetadata.MDTPReportRequestIDMetadataItem("12345-6789")
            case m                                                           => m
          }
        )
      )
      whenReady(result) { case (status, message) =>
        val paramsCaptor = ArgumentCaptor.forClass(classOf[Map[String, String]])
        verify(mockEmailConnector).sendEmailRequest(
          eqTo("tre_report_available"),
          eqTo("masked@example.com"),
          paramsCaptor.capture()
        )(any())
        val params       = paramsCaptor.getValue
        params("reportRequestId") shouldBe "XXXXX-6789"
      }
    }
  }
}
