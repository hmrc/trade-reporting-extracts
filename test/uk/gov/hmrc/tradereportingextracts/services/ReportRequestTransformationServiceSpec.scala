package uk.gov.hmrc.tradereportingextracts.services

import org.scalatest.matchers.must.Matchers
import org.mockito.Mockito.*
import org.scalatest.freespec.AnyFreeSpec
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.tradereportingextracts.models.*
import uk.gov.hmrc.tradereportingextracts.models.eis.EisReportRequest

import java.time.{Instant, LocalDate, ZoneOffset}

class ReportRequestTransformationServiceSpec extends AnyFreeSpec with Matchers with MockitoSugar {

  val mockRequestReferenceService: RequestReferenceService = mock[RequestReferenceService]
  when(mockRequestReferenceService.random()).thenReturn("RE00000001")

  val service = new ReportRequestTransformationService(mockRequestReferenceService)

  val reportRequestTemplate = ReportRequestUserAnswersModel(
    eori = "GB123456789000",
    reportStartDate = "2025-04-01",
    reportEndDate = "2025-04-30",
    whichEori = Some("GB123456789000"),
    reportName = "TestReport",
    eoriRole = Set("declarant"),
    reportType = Set("importHeader"),
    dataType = "import",
    additionalEmail = Some(Set("test@example.com"))
  )

  "transformReportRequest" - {
    "create a ReportRequest for importHeader and declarant" in {
      val result = service.transformReportRequest(
        "GB123456789000",
        reportRequestTemplate,
        Seq("GB123456789001"),
        "user@email.com"
      )
      result.reportRequestId mustBe "RE00000001"
      result.reportName mustBe "TestReport"
      result.requesterEORI mustBe "GB123456789000"
      result.eoriRole mustBe EoriRole.DECLARANT
      result.reportEORIs must contain allOf ("GB123456789001", "GB123456789000")
      result.recipientEmails must contain allOf ("test@example.com", "user@email.com")
      result.reportTypeName mustBe ReportTypeName.IMPORTS_HEADER_REPORT
      result.reportStart mustBe LocalDate.parse("2025-04-01").atStartOfDay(ZoneOffset.UTC).toInstant
      result.reportEnd mustBe LocalDate.parse("2025-04-30").atStartOfDay(ZoneOffset.UTC).toInstant
    }

    "create a ReportRequest for importItem and trader" in {
      val model = reportRequestTemplate.copy(eoriRole = Set("importer"), reportType = Set("importItem"))
      val result = service.transformReportRequest(
        "GB123456789000",
        model,
        Seq(),
        "user@email.com"
      )
      result.eoriRole mustBe EoriRole.TRADER
      result.reportTypeName mustBe ReportTypeName.IMPORTS_ITEM_REPORT
    }

    "create a ReportRequest for importTaxLine and trader-declarant" in {
      val model = reportRequestTemplate.copy(eoriRole = Set("declarant", "importer"), reportType = Set("importTaxLine"))
      val result = service.transformReportRequest(
        "GB123456789000",
        model,
        Seq(),
        "user@email.com"
      )
      result.eoriRole mustBe EoriRole.TRADER_DECLARANT
      result.reportTypeName mustBe ReportTypeName.IMPORTS_TAXLINE_REPORT
    }

    "create a ReportRequest for exportItem" in {
      val model = reportRequestTemplate.copy(eoriRole = Set("exporter"), reportType = Set("exportItem"))
      val result = service.transformReportRequest(
        "GB123456789000",
        model,
        Seq(),
        "user@email.com"
      )
      result.reportTypeName mustBe ReportTypeName.EXPORTS_ITEM_REPORT
    }
  }

  "toEisReportRequest" - {
    "convert ReportRequest to EisReportRequest" in {
      val reportRequest = service.transformReportRequest(
        "GB123456789000",
        reportRequestTemplate,
        Seq("GB123456789001"),
        "user@email.com"
      )
      val eisRequest = service.toEisReportRequest(reportRequest)
      eisRequest.eori must contain allOf ("GB123456789001", "GB123456789000")
      eisRequest.eoriRole mustBe EisReportRequest.EoriRole.DECLARANT
      eisRequest.reportTypeName mustBe EisReportRequest.ReportTypeName.IMPORTSHEADERREPORT
      eisRequest.requestID mustBe "RE00000001"
      eisRequest.requesterEori mustBe "GB123456789000"
      eisRequest.startDate mustBe "2025-04-01"
      eisRequest.endDate mustBe "2025-04-30"
    }
  }
}