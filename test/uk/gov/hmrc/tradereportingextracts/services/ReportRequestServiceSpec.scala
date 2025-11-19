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

import org.mockito.Mockito.{reset, verify, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers.{mustBe, mustEqual}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.mvc.Headers
import uk.gov.hmrc.crypto.Sensitive.SensitiveString
import uk.gov.hmrc.tradereportingextracts.connectors.{CustomsDataStoreConnector, EmailConnector}
import uk.gov.hmrc.tradereportingextracts.models.*
import uk.gov.hmrc.tradereportingextracts.models.eis.EisReportStatusRequest
import uk.gov.hmrc.tradereportingextracts.models.eis.EisReportStatusRequest.StatusType
import uk.gov.hmrc.tradereportingextracts.repositories.ReportRequestRepository
import uk.gov.hmrc.tradereportingextracts.utils.WireMockHelper
import org.mockito.Mockito.*
import org.mockito.ArgumentMatchers.{eq as eqTo, *}
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.tradereportingextracts.models.StatusCode.FAILED
import uk.gov.hmrc.tradereportingextracts.models.audit.{AuditEvent, ReportGenerationFailureEvent}

import java.time.{Instant, LocalDate}

class ReportRequestServiceSpec
    extends AnyWordSpec
    with Matchers
    with MockitoSugar
    with ScalaFutures
    with WireMockHelper {

  val service                                              =
    new ReportRequestService(null, null, null, null) // nulls are fine here since we’re only testing private logic
  implicit val hc: HeaderCarrier                           = HeaderCarrier()
  implicit val ec: ExecutionContext                        = scala.concurrent.ExecutionContext.Implicits.global
  val mockReportRequestRepository: ReportRequestRepository = mock[ReportRequestRepository]
  val mockEmailConnector: EmailConnector                   = mock[EmailConnector]
  val mockAuditService: AuditService                       = mock[AuditService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockReportRequestRepository)
    reset(mockEmailConnector)
    reset(mockAuditService)
  }
  "determineReportStatus" should {

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
        Seq(SensitiveString("test@example.com")),
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
        Seq(SensitiveString("test@example.com")),
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
        Seq(SensitiveString("test@example.com")),
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
        Seq(SensitiveString("test@example.com")),
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

  "processReportStatus" should {
    val service = new ReportRequestService(mockReportRequestRepository, null, mockEmailConnector, mockAuditService)
    val headers = new Headers(Seq("X-Correlation-ID" -> "correlationId"))

    val reportRequest = ReportRequest(
      "RE123456",
      "correlationId",
      "reportName",
      "trader",
      EoriRole.TRADER,
      Seq("trader"),
      Some(SensitiveString("user@email.com")),
      Seq(SensitiveString("additionalUser@email.com")),
      ReportTypeName.IMPORTS_ITEM_REPORT,
      Instant.now(),
      Instant.now(),
      Instant.now(),
      Seq.empty,
      None,
      Instant.now()
    )

    val statusReq = EisReportStatusRequest(
      applicationComponent = EisReportStatusRequest.ApplicationComponent.TRE,
      statusCode = FAILED.toString,
      statusMessage = "FAIL",
      statusTimestamp = LocalDate.now().toString,
      statusType = EisReportStatusRequest.StatusType.ERROR
    )

    "send failure emails and update when new ERROR and no previous ERROR" in {

      when(mockReportRequestRepository.findByCorrelationId(any())(any()))
        .thenReturn(Future.successful(Some(reportRequest)))
      when(mockReportRequestRepository.update(any())(any())).thenReturn(Future.successful(true))
      when(mockEmailConnector.sendEmailRequest(any(), any(), any())(any())).thenReturn(Future.successful(()))
      doNothing()
        .when(mockAuditService)
        .audit(any())(using any(), any())

      service.processReportStatus(headers, statusReq).futureValue

      verify(mockReportRequestRepository).update(any())(any())
      verify(mockAuditService, times(1))
        .audit(any())(using any(), any())
      verify(mockEmailConnector).sendEmailRequest(
        eqTo("tre_report_failed"),
        eqTo("user@email.com"),
        eqTo(Map("reportRequestId" -> "XXXXX456"))
      )(any())
      verify(mockEmailConnector).sendEmailRequest(
        eqTo("tre_report_failed_non_verified"),
        eqTo("additionalUser@email.com"),
        eqTo(Map("reportRequestId" -> "XXXXX456"))
      )(any())
    }

    "only update when previous ERROR exists" in {

      val reportRequestWithExistingError =
        reportRequest.copy(notifications = Seq(EisReportStatusRequest(null, "CODE", "msg", "ts", StatusType.ERROR)))

      when(mockReportRequestRepository.findByCorrelationId(any())(any()))
        .thenReturn(Future.successful(Some(reportRequestWithExistingError)))
      when(mockReportRequestRepository.update(any())(any())).thenReturn(Future.successful(true))
      service.processReportStatus(headers, statusReq).futureValue

      verify(mockReportRequestRepository).update(any())(any())
      verifyNoInteractions(mockEmailConnector)
    }

    "do nothing if no reportRequest found" in {

      when(mockReportRequestRepository.findByCorrelationId(any())(any())).thenReturn(Future.successful(None))

      service.processReportStatus(headers, statusReq)

      verify(mockReportRequestRepository, never).update(any())(any())
      verifyNoInteractions(mockEmailConnector)
    }
  }

  "countReportSubmissionsForEoriOnDate" should {
    val mockCustomsDataStoreConnector = mock[CustomsDataStoreConnector]

    val service = new ReportRequestService(mockReportRequestRepository, mockCustomsDataStoreConnector, null, null)

    val eori  = "EORI123456"
    val limit = 5
    val date  = LocalDate.of(2025, 7, 16)

    "must return true when submissions are greater than or equal to the limit" in {
      implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global

      when(mockReportRequestRepository.countReportSubmissionsForEoriOnDate(eori, date)).thenReturn(Future.successful(5))

      val result = service.countReportSubmissionsForEoriOnDate(eori, limit, date)

      result.futureValue mustBe true
      verify(mockReportRequestRepository).countReportSubmissionsForEoriOnDate(eori, date)
    }

    "must return false when submissions are below the limit" in {
      when(mockReportRequestRepository.countReportSubmissionsForEoriOnDate(eori, date))
        .thenReturn(Future.successful(3))

      val resultFuture = service.countReportSubmissionsForEoriOnDate(eori, limit, date)

      val result = resultFuture.futureValue
      result mustBe false

      verify(mockReportRequestRepository).countReportSubmissionsForEoriOnDate(eori, date)
    }

    "must fail when repository throws an exception" in {
      val expectedException = new RuntimeException("failure")
      when(mockReportRequestRepository.countReportSubmissionsForEoriOnDate(eori, date))
        .thenReturn(Future.failed(expectedException))

      val result = service.countReportSubmissionsForEoriOnDate(eori, limit, date)

      whenReady(result.failed) { ex =>
        ex mustEqual expectedException
      }
    }
  }

  "getReportRequestsForUser" should {
    val mockCustomsDataStoreConnector = mock[CustomsDataStoreConnector]
    val mockReportRequestRepository   = mock[ReportRequestRepository]
    val service                       = new ReportRequestService(mockReportRequestRepository, mockCustomsDataStoreConnector, null, null)
    implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

    val eori                    = "GB123456789000"
    val thirdPartyEori          = "GB999999999999"
    val userReportRequest       = ReportRequest(
      "REQ123",
      "corr1",
      "Monthly Report",
      eori,
      EoriRole.TRADER,
      Seq(eori),
      None,
      Seq.empty,
      ReportTypeName.EXPORTS_ITEM_REPORT,
      Instant.parse("2024-06-01T00:00:00Z"),
      Instant.parse("2024-06-30T23:59:59Z"),
      Instant.parse("2024-07-01T10:00:00Z"),
      Seq.empty,
      None,
      Instant.now()
    )
    val thirdPartyReportRequest = userReportRequest.copy(reportEORIs = Seq(thirdPartyEori))

    "return user reports and third party reports correctly" in {
      when(mockCustomsDataStoreConnector.getEoriHistory(eori))
        .thenReturn(Future.successful(EoriHistoryResponse(Seq.empty)))
      when(mockReportRequestRepository.getRequestedReportsByHistory(Seq(eori)))
        .thenReturn(Future.successful(Seq(userReportRequest, thirdPartyReportRequest)))
      when(mockCustomsDataStoreConnector.getCompanyInformation(thirdPartyEori))
        .thenReturn(Future.successful(CompanyInformation("Unknown company")))

      val result = service.getReportRequestsForUser(eori).futureValue

      result.userReports mustBe Some(
        Seq(
          UserReport(
            referenceNumber = "REQ123",
            reportName = "Monthly Report",
            requestedDate = Instant.parse("2024-07-01T10:00:00Z"),
            reportType = ReportTypeName.EXPORTS_ITEM_REPORT,
            reportStatus = ReportStatus.IN_PROGRESS,
            reportStartDate = Instant.parse("2024-06-01T00:00:00Z"),
            reportEndDate = Instant.parse("2024-06-30T23:59:59Z")
          )
        )
      )
      result.thirdPartyReports mustBe Some(
        Seq(
          ThirdPartyReport(
            referenceNumber = "REQ123",
            reportName = "Monthly Report",
            requestedDate = Instant.parse("2024-07-01T10:00:00Z"),
            reportType = ReportTypeName.EXPORTS_ITEM_REPORT,
            companyName = "Unknown",
            reportStatus = ReportStatus.IN_PROGRESS,
            reportStartDate = Instant.parse("2024-06-01T00:00:00Z"),
            reportEndDate = Instant.parse("2024-06-30T23:59:59Z")
          )
        )
      )
    }

    "return user reports and third party reports correctly with no Company name" in {
      when(mockCustomsDataStoreConnector.getEoriHistory(eori))
        .thenReturn(Future.successful(EoriHistoryResponse(Seq.empty)))
      when(mockReportRequestRepository.getRequestedReportsByHistory(Seq(eori)))
        .thenReturn(Future.successful(Seq(userReportRequest, thirdPartyReportRequest)))
      when(mockCustomsDataStoreConnector.getCompanyInformation(thirdPartyEori))
        .thenReturn(Future.successful(CompanyInformation()))

      val result = service.getReportRequestsForUser(eori).futureValue

      result.userReports mustBe Some(
        Seq(
          UserReport(
            referenceNumber = "REQ123",
            reportName = "Monthly Report",
            requestedDate = Instant.parse("2024-07-01T10:00:00Z"),
            reportType = ReportTypeName.EXPORTS_ITEM_REPORT,
            reportStatus = ReportStatus.IN_PROGRESS,
            reportStartDate = Instant.parse("2024-06-01T00:00:00Z"),
            reportEndDate = Instant.parse("2024-06-30T23:59:59Z")
          )
        )
      )
      result.thirdPartyReports mustBe Some(
        Seq(
          ThirdPartyReport(
            referenceNumber = "REQ123",
            reportName = "Monthly Report",
            requestedDate = Instant.parse("2024-07-01T10:00:00Z"),
            reportType = ReportTypeName.EXPORTS_ITEM_REPORT,
            companyName = "Unknown",
            reportStatus = ReportStatus.IN_PROGRESS,
            reportStartDate = Instant.parse("2024-06-01T00:00:00Z"),
            reportEndDate = Instant.parse("2024-06-30T23:59:59Z")
          )
        )
      )
    }

    "return None for both when no reports" in {
      when(mockReportRequestRepository.getRequestedReportsByHistory(Seq(eori)))
        .thenReturn(Future.successful(Seq.empty))

      val result = service.getReportRequestsForUser(eori).futureValue

      result.userReports mustBe None
      result.thirdPartyReports mustBe None
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
