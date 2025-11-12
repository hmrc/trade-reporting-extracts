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
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status.{BAD_REQUEST, CREATED, NOT_FOUND}
import uk.gov.hmrc.crypto.Sensitive.SensitiveString
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tradereportingextracts.connectors.EmailConnector
import uk.gov.hmrc.tradereportingextracts.models.sdes.{FileNotificationMetadata, FileNotificationResponse}
import uk.gov.hmrc.tradereportingextracts.models.{ReportRequest, ReportTypeName, FileNotification as TreFileNotification}
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

  val fileNotification: FileNotificationResponse = FileNotificationResponse(
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
      FileNotificationMetadata.ReportFilesPartsMetadataItem("1")
    )
  )

  val completeFileNotification: FileNotificationResponse = FileNotificationResponse(
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
      FileNotificationMetadata.ReportFilesPartsMetadataItem("1"),
      FileNotificationMetadata.ReportLastFileMetadataItem("true")
    )
  )

  val reportRequest: ReportRequest = ReportRequest(
    reportRequestId = "RE123456",
    correlationId = "corr-1",
    reportName = "name",
    requesterEORI = "GB123456789012",
    eoriRole = null,
    reportEORIs = Seq("GB123456789012"),
    userEmail = Some(SensitiveString("test@example.com")),
    recipientEmails = Seq(SensitiveString("test@example.com")),
    reportTypeName = ReportTypeName.IMPORTS_HEADER_REPORT,
    reportStart = null,
    reportEnd = null,
    createDate = null,
    notifications = Seq.empty,
    fileNotifications = None,
    updateDate = null
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

    "send tre_report_available only to userEmail if present" in {
      val reportWithUserEmail = reportRequest.copy(
        userEmail = Some(SensitiveString("user@verified.com")),
        recipientEmails =
          Seq(SensitiveString("recipient1@nonverified.com"), SensitiveString("recipient2@nonverified.com"))
      )
      when(mockReportRequestService.get(eqTo("RE123456"))(any()))
        .thenReturn(Future.successful(Some(reportWithUserEmail)))
      when(mockReportRequestService.update(any())(any()))
        .thenReturn(Future.successful(true))
      when(mockEmailConnector.sendEmailRequest(any(), any(), any())(any()))
        .thenReturn(Future.successful(()))

      val result = service.processFileNotification(completeFileNotification)
      whenReady(result) { _ =>
        verify(mockEmailConnector).sendEmailRequest(
          eqTo("tre_report_available"),
          eqTo("user@verified.com"),
          eqTo(Map("reportRequestId" -> "XXXXX456"))
        )(any())
        verify(mockEmailConnector, never).sendEmailRequest(
          eqTo("tre_report_available"),
          eqTo("recipient1@nonverified.com"),
          any()
        )(any())
        verify(mockEmailConnector, never).sendEmailRequest(
          eqTo("tre_report_available"),
          eqTo("recipient2@nonverified.com"),
          any()
        )(any())
      }
    }

    "send tre_report_available_non_verified to all recipientEmails" in {
      val reportWithRecipients = reportRequest.copy(
        userEmail = Some(SensitiveString("user@verified.com")),
        recipientEmails =
          Seq(SensitiveString("recipient1@nonverified.com"), SensitiveString("recipient2@nonverified.com"))
      )
      when(mockReportRequestService.get(eqTo("RE123456"))(any()))
        .thenReturn(Future.successful(Some(reportWithRecipients)))
      when(mockReportRequestService.update(any())(any()))
        .thenReturn(Future.successful(true))
      when(mockEmailConnector.sendEmailRequest(any(), any(), any())(any()))
        .thenReturn(Future.successful(()))

      val result = service.processFileNotification(completeFileNotification)
      whenReady(result) { _ =>
        verify(mockEmailConnector).sendEmailRequest(
          eqTo("tre_report_available_non_verified"),
          eqTo("recipient1@nonverified.com"),
          eqTo(Map("reportRequestId" -> "XXXXX456"))
        )(any())
        verify(mockEmailConnector).sendEmailRequest(
          eqTo("tre_report_available_non_verified"),
          eqTo("recipient2@nonverified.com"),
          eqTo(Map("reportRequestId" -> "XXXXX456"))
        )(any())
      }
    }

    "when userEmail is missing do not send request with tre_report_available" in {
      val reportNoUserEmail = reportRequest.copy(
        userEmail = None,
        recipientEmails = Seq(SensitiveString("recipient@nonverified.com"))
      )
      when(mockReportRequestService.get(eqTo("RE123456"))(any()))
        .thenReturn(Future.successful(Some(reportNoUserEmail)))
      when(mockReportRequestService.update(any())(any()))
        .thenReturn(Future.successful(true))
      when(mockEmailConnector.sendEmailRequest(any(), any(), any())(any()))
        .thenReturn(Future.successful(()))

      val result = service.processFileNotification(completeFileNotification)
      whenReady(result) { _ =>
        verify(mockEmailConnector, never).sendEmailRequest(
          eqTo("tre_report_available"),
          any(),
          any()
        )(any())
        verify(mockEmailConnector).sendEmailRequest(
          eqTo("tre_report_available_non_verified"),
          eqTo("recipient@nonverified.com"),
          any()
        )(any())
      }
    }

    "mask the first 5 digits of reportRequestId in params when sending email" in {
      val maskedReportRequest = reportRequest.copy(
        reportRequestId = "12345-6789",
        recipientEmails = Seq(SensitiveString("masked@example.com"))
      )
      when(mockReportRequestService.get(eqTo("12345-6789"))(any()))
        .thenReturn(Future.successful(Some(maskedReportRequest)))
      when(mockReportRequestService.update(any())(any()))
        .thenReturn(Future.successful(true))
      when(mockEmailConnector.sendEmailRequest(any(), any(), any())(any()))
        .thenReturn(Future.successful(()))

      val result = service.processFileNotification(
        fileNotification.copy(
          metadata = completeFileNotification.metadata.map {
            case FileNotificationMetadata.MDTPReportRequestIDMetadataItem(_) =>
              FileNotificationMetadata.MDTPReportRequestIDMetadataItem("12345-6789")
            case m                                                           => m
          }
        )
      )
      whenReady(result) { case (status, message) =>
        val paramsCaptor = ArgumentCaptor.forClass(classOf[Map[String, String]])
        verify(mockEmailConnector).sendEmailRequest(
          any(),
          eqTo("masked@example.com"),
          paramsCaptor.capture()
        )(any())
        val params       = paramsCaptor.getValue
        params("reportRequestId") shouldBe "XXXXX-6789"
      }
    }
  }
}
