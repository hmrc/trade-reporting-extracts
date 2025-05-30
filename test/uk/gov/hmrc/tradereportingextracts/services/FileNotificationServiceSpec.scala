package uk.gov.hmrc.tradereportingextracts.services

import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.*
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.wordspec.AnyWordSpec
import play.api.http.Status.{BAD_REQUEST, CREATED, NOT_FOUND}
import uk.gov.hmrc.tradereportingextracts.models.sdes.{FileNotification, FileNotificationMetadata}
import uk.gov.hmrc.tradereportingextracts.models.{FileType, ReportTypeName, FileNotification => TreFileNotification}
import uk.gov.hmrc.tradereportingextracts.models.ReportRequest
import scala.concurrent.{ExecutionContext, Future}

class FileNotificationServiceSpec extends AnyWordSpec with Matchers with MockitoSugar with ScalaFutures {

  implicit val ec: ExecutionContext = ExecutionContext.global

  val mockReportRequestService: ReportRequestService = mock[ReportRequestService]
  val service = new FileNotificationService(mockReportRequestService)

  val fileNotification = FileNotification(
    eori = "GB123456789012",
    fileName = "testFileName",
    fileSize = 12345,
    metadata = List(
      FileNotificationMetadata.RetentionDaysMetadataItem("30"),
      FileNotificationMetadata.FileTypeMetadataItem("CSV"),
      FileNotificationMetadata.EORIMetadataItem("GB123456789012"),
      FileNotificationMetadata.MDTPReportXCorrelationIDMetadataItem("asfd-asdf-asdf"),
      FileNotificationMetadata.MDTPReportRequestIDMetadataItem("TRE-19"),
      FileNotificationMetadata.MDTPReportTypeNameMetadataItem("IMPORTS_HEADER_REPORT"),
      FileNotificationMetadata.ReportFilesPartsMetadataItem("1of2")
    )
  )

  val reportRequest = ReportRequest(
    reportRequestId = "TRE-19",
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
      when(mockReportRequestService.get(eqTo("TRE-19"))(any()))
        .thenReturn(Future.successful(Some(reportRequest)))
      when(mockReportRequestService.update(any())(any()))
        .thenReturn(Future.successful(true))

      val result = service.processFileNotification(fileNotification)
      whenReady(result) { case (status, message) =>
        status shouldBe CREATED
        message shouldBe "Created"
        val captor = ArgumentCaptor.forClass(classOf[ReportRequest])
        verify(mockReportRequestService).update(captor.capture())(any())
        val updated = captor.getValue
        updated.fileNotifications.get.size shouldBe 1
        updated.fileNotifications.get.head.fileName shouldBe "testFileName"
      }
    }

    "append to existing fileNotifications" in {
      val existing = reportRequest.copy(fileNotifications = Some(Seq(
        TreFileNotification(
          fileName = "oldFile",
          fileSize = 1,
          retentionDays = 1,
          fileType = FileType.CSV,
          mDTPReportXCorrelationID = "x",
          mDTPReportRequestID = "TRE-19",
          mDTPReportTypeName = ReportTypeName.IMPORTS_HEADER_REPORT,
          reportFilesParts = "1of1"
        )
      )))
      when(mockReportRequestService.get(eqTo("TRE-19"))(any()))
        .thenReturn(Future.successful(Some(existing)))
      when(mockReportRequestService.update(any())(any()))
        .thenReturn(Future.successful(true))

      val result = service.processFileNotification(fileNotification)
      whenReady(result) { case (status, message) =>
        status shouldBe CREATED
        val captor = ArgumentCaptor.forClass(classOf[ReportRequest])
        verify(mockReportRequestService).update(captor.capture())(any())
        val updated = captor.getValue
        updated.fileNotifications.get.size shouldBe 2
      }
    }

    "return NOT_FOUND if reportRequest is not found" in {
      when(mockReportRequestService.get(eqTo("TRE-19"))(any()))
        .thenReturn(Future.successful(None))

      val result = service.processFileNotification(fileNotification)
      whenReady(result) { case (status, message) =>
        status shouldBe NOT_FOUND
        message should include ("ReportRequest not found")
      }
    }

    "return BAD_REQUEST if MDTPReportRequestID is missing" in {
      val noIdNotification = fileNotification.copy(
        metadata = fileNotification.metadata.filterNot(_.isInstanceOf[FileNotificationMetadata.MDTPReportRequestIDMetadataItem])
      )
      val result = service.processFileNotification(noIdNotification)
      whenReady(result) { case (status, message) =>
        status shouldBe BAD_REQUEST
        message should include ("report-requestID not found")
      }
    }
  }
}