package uk.gov.hmrc.tradereportingextracts.services

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers.{must, mustEqual}
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import uk.gov.hmrc.tradereportingextracts.models.Report
import uk.gov.hmrc.tradereportingextracts.repositories.ReportRequestRepository

import scala.concurrent.{ExecutionContext, Future}

class ReportRequestServiceSpec extends AnyWordSpec, GuiceOneAppPerSuite, ScalaFutures:

  given ExecutionContext = ExecutionContext.global

  lazy val mockReportRequestRepository: ReportRequestRepository = mock[ReportRequestRepository]

  private val reportRequestService = new ReportRequestService(mockReportRequestRepository)

  private val report = Report(
    userid = 12345L,
    reportId = "reportId123",
    templateId = "templateId123",
    recipientEmails = Array("example1@example.com", "example2@example.com"),
    reportEORIs = Array("EORI123", "EORI456"),
    reportType = "TypeA",
    reportStart = "2023-01-01",
    reportEnd = "2023-12-31",
    status = "Completed",
    statusDetails = "Report generated successfully"
  )

  "ReportRequestService" should {

    "insertReportRequest" should {
      "must insert a report request successfully" in {
        when(mockReportRequestRepository.insertReportRequest(report)).thenReturn(Future.successful(true))

        val result = reportRequestService.create(report).futureValue

        result mustEqual true

        verify(mockReportRequestRepository, times(1)).insertReportRequest(any)(using any())
      }
    }

    "findByReportId" should {
      "must retrieve a report successfully using a reportId" in {
        when(mockReportRequestRepository.findByReportId(report.reportId)).thenReturn(Future.successful(Some(report)))

        val fetchedRecord = reportRequestService.get(report.reportId).futureValue

        fetchedRecord mustEqual Some(report)
      }

      "must return None if reportId not found" in {
        when(mockReportRequestRepository.findByReportId("nonExistentId")).thenReturn(Future.successful(None))

        val fetchedRecord = reportRequestService.get("nonExistentId").futureValue

        fetchedRecord mustEqual None
      }
    }

    "updateByReportId" should {
      "must update an existing report successfully" in {
        when(mockReportRequestRepository.updateByReportId(report)).thenReturn(Future.successful(true))

        val updatedRecord = reportRequestService.update(report).futureValue

        updatedRecord mustEqual true
      }
    }

    "deleteByReportId" should {
      "must delete an existing report successfully" in {
        when(mockReportRequestRepository.deleteByReportId(report.reportId)).thenReturn(Future.successful(true))

        val deletedRecord = reportRequestService.delete(report.reportId).futureValue

        deletedRecord mustEqual true
      }
    }
  }
