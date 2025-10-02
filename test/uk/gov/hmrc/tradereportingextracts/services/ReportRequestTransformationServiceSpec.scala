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

import org.mockito.Mockito.*
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.crypto.Sensitive.SensitiveString
import uk.gov.hmrc.tradereportingextracts.models.*
import uk.gov.hmrc.tradereportingextracts.models.eis.EisReportRequest

import java.time.{Instant, LocalDate, ZoneOffset}
import scala.concurrent.Future

class ReportRequestTransformationServiceSpec extends AsyncFreeSpec with Matchers with MockitoSugar {

  val mockRequestReferenceService: RequestReferenceService = mock[RequestReferenceService]
  when(mockRequestReferenceService.generateUnique()).thenReturn(Future.successful("REF-00000001"))

  val service = new ReportRequestTransformationService(mockRequestReferenceService)

  val reportRequestTemplate: ReportRequestUserAnswersModel = ReportRequestUserAnswersModel(
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
      service
        .transformReportRequest(
          "GB123456789000",
          reportRequestTemplate,
          Seq("GB123456789001"),
          "user@email.com"
        )
        .map { result =>
          result.reportRequestId mustBe "REF-00000001"
          result.reportName mustBe "TestReport"
          result.requesterEORI mustBe "GB123456789000"
          result.eoriRole mustBe EoriRole.DECLARANT
          result.reportEORIs                                must contain allOf ("GB123456789001", "GB123456789000")
          result.userEmail.get.decryptedValue               must be("user@email.com")
          result.recipientEmails.map(e => e.decryptedValue) must contain("test@example.com")
          result.recipientEmails.size mustBe reportRequestTemplate.additionalEmail.get.size
          result.reportTypeName mustBe ReportTypeName.IMPORTS_HEADER_REPORT
          result.reportStart mustBe LocalDate.parse("2025-04-01").atStartOfDay(ZoneOffset.UTC).toInstant
          result.reportEnd mustBe LocalDate.parse("2025-04-30").atStartOfDay(ZoneOffset.UTC).toInstant
        }
    }

    "create a ReportRequest for importItem and trader" in {
      val model = reportRequestTemplate.copy(eoriRole = Set("importer"), reportType = Set("importItem"))
      service
        .transformReportRequest(
          "GB123456789000",
          model,
          Seq(),
          "user@email.com"
        )
        .map { result =>
          result.eoriRole mustBe EoriRole.TRADER
          result.reportTypeName mustBe ReportTypeName.IMPORTS_ITEM_REPORT
        }
    }

    "create a ReportRequest for importTaxLine and trader-declarant" in {
      val model = reportRequestTemplate.copy(eoriRole = Set("declarant", "importer"), reportType = Set("importTaxLine"))
      service
        .transformReportRequest(
          "GB123456789000",
          model,
          Seq(),
          "user@email.com"
        )
        .map { result =>
          result.eoriRole mustBe EoriRole.TRADER_DECLARANT
          result.reportTypeName mustBe ReportTypeName.IMPORTS_TAXLINE_REPORT
        }
    }

    "create a ReportRequest for exportItem" in {
      val model = reportRequestTemplate.copy(eoriRole = Set("exporter"), reportType = Set("exportItem"))
      service
        .transformReportRequest(
          "GB123456789000",
          model,
          Seq(),
          "user@email.com"
        )
        .map { result =>
          result.reportTypeName mustBe ReportTypeName.EXPORTS_ITEM_REPORT
        }
    }
  }

  "toEisReportRequest" - {
    "convert ReportRequest to EisReportRequest" in {
      service
        .transformReportRequest(
          "GB123456789000",
          reportRequestTemplate,
          Seq("GB123456789001"),
          "user@email.com"
        )
        .map { reportRequest =>
          val eisRequest = service.toEisReportRequest(reportRequest)
          eisRequest.eori must contain allOf ("GB123456789001", "GB123456789000")
          eisRequest.eoriRole mustBe EisReportRequest.EoriRole.DECLARANT
          eisRequest.reportTypeName mustBe EisReportRequest.ReportTypeName.IMPORTSHEADERREPORT
          eisRequest.requestID mustBe "REF-00000001"
          eisRequest.requesterEori mustBe "GB123456789000"
          eisRequest.startDate mustBe "2025-04-01"
          eisRequest.endDate mustBe "2025-04-30"
        }
    }
  }

  "reportConfirmationTransformer" - {
    "should transform ReportRequest to ReportConfirmation for all report types" in {
      val requests = Seq(
        ReportRequest(
          reportRequestId = "REF-1",
          correlationId = "corr-1",
          reportName = "Name1",
          requesterEORI = "EORI1",
          eoriRole = EoriRole.DECLARANT,
          reportEORIs = Seq("EORI1"),
          userEmail = Some(SensitiveString("user1@test.com")),
          recipientEmails = Seq(SensitiveString("user1@test.com")),
          reportTypeName = ReportTypeName.IMPORTS_HEADER_REPORT,
          reportStart = Instant.now,
          reportEnd = Instant.now,
          createDate = Instant.now,
          notifications = Seq(),
          fileNotifications = None,
          updateDate = Instant.now
        ),
        ReportRequest(
          reportRequestId = "REF-2",
          correlationId = "corr-2",
          reportName = "Name2",
          requesterEORI = "EORI2",
          eoriRole = EoriRole.TRADER,
          reportEORIs = Seq("EORI2"),
          userEmail = Some(SensitiveString("user2@test.com")),
          recipientEmails = Seq(SensitiveString("user2@test.com")),
          reportTypeName = ReportTypeName.IMPORTS_ITEM_REPORT,
          reportStart = Instant.now,
          reportEnd = Instant.now,
          createDate = Instant.now,
          notifications = Seq(),
          fileNotifications = None,
          updateDate = Instant.now
        ),
        ReportRequest(
          reportRequestId = "REF-3",
          correlationId = "corr-3",
          reportName = "Name3",
          requesterEORI = "EORI3",
          eoriRole = EoriRole.TRADER_DECLARANT,
          reportEORIs = Seq("EORI3"),
          userEmail = Some(SensitiveString("user3@test.com")),
          recipientEmails = Seq(SensitiveString("user3@test.com")),
          reportTypeName = ReportTypeName.IMPORTS_TAXLINE_REPORT,
          reportStart = Instant.now,
          reportEnd = Instant.now,
          createDate = Instant.now,
          notifications = Seq(),
          fileNotifications = None,
          updateDate = Instant.now
        )
      )

      val confirmations = service.reportConfirmationTransformer(requests)
      confirmations mustBe Seq(
        ReportConfirmation("Name1", "importHeader", "REF-1"),
        ReportConfirmation("Name2", "importItem", "REF-2"),
        ReportConfirmation("Name3", "importTaxLine", "REF-3")
      )
    }

    "shoudl tranform export items correctly" in {
      val requests = Seq(
        ReportRequest(
          reportRequestId = "REF-1",
          correlationId = "corr-1",
          reportName = "Name1",
          requesterEORI = "EORI1",
          eoriRole = EoriRole.TRADER,
          reportEORIs = Seq("EORI1"),
          userEmail = Some(SensitiveString("user1@test.com")),
          recipientEmails = Seq(SensitiveString("user1@test.com")),
          reportTypeName = ReportTypeName.EXPORTS_ITEM_REPORT,
          reportStart = Instant.now,
          reportEnd = Instant.now,
          createDate = Instant.now,
          notifications = Seq(),
          fileNotifications = None,
          updateDate = Instant.now
        )
      )

      val confirmations = service.reportConfirmationTransformer(requests)
      confirmations mustBe Seq(
        ReportConfirmation("Name1", "exportItem", "REF-1")
      )
    }
  }
}
