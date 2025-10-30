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

package uk.gov.hmrc.tradereportingextracts.models

import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsSuccess, Json}

import java.time.Instant

class AvailableReportResponseSpec extends PlaySpec {

  val instantNow: Instant = Instant.now()

  val sampleAction: AvailableReportAction = AvailableReportAction(
    fileName = "report.csv",
    fileURL = "http://example.com/report.csv",
    size = 2048L,
    fileType = FileType.CSV
  )

  val sampleUserReport: AvailableUserReportResponse = AvailableUserReportResponse(
    referenceNumber = "ref123",
    reportName = "User Report 1",
    expiryDate = instantNow,
    reportType = ReportTypeName.IMPORTS_ITEM_REPORT,
    action = Seq(sampleAction)
  )

  val sampleThirdPartyReport: AvailableThirdPartyReportResponse = AvailableThirdPartyReportResponse(
    referenceNumber = "ref456",
    reportName = "Third Party Report 1",
    expiryDate = instantNow,
    reportType = ReportTypeName.EXPORTS_ITEM_REPORT,
    companyName = "Test Company",
    action = Seq(sampleAction)
  )

  "AvailableReportResponse" should {
    "serialize and deserialize correctly when both user and third-party reports are present" in {
      val response = AvailableReportResponse(
        availableUserReports = Some(Seq(sampleUserReport)),
        availableThirdPartyReports = Some(Seq(sampleThirdPartyReport))
      )
      val json     = Json.toJson(response)
      json.validate[AvailableReportResponse] mustBe JsSuccess(response)
    }

    "serialize and deserialize correctly when only user reports are present" in {
      val response = AvailableReportResponse(
        availableUserReports = Some(Seq(sampleUserReport)),
        availableThirdPartyReports = None
      )
      val json     = Json.toJson(response)
      json.validate[AvailableReportResponse] mustBe JsSuccess(response)
    }

    "serialize and deserialize correctly when only third-party reports are present" in {
      val response = AvailableReportResponse(
        availableUserReports = None,
        availableThirdPartyReports = Some(Seq(sampleThirdPartyReport))
      )
      val json     = Json.toJson(response)
      json.validate[AvailableReportResponse] mustBe JsSuccess(response)
    }

    "serialize and deserialize correctly when no reports are present" in {
      val response = AvailableReportResponse(
        availableUserReports = None,
        availableThirdPartyReports = None
      )
      val json     = Json.toJson(response)
      json.validate[AvailableReportResponse] mustBe JsSuccess(response)
    }

    "serialize and deserialize correctly with empty sequences of reports" in {
      val response = AvailableReportResponse(
        availableUserReports = Some(Seq.empty),
        availableThirdPartyReports = Some(Seq.empty)
      )
      val json     = Json.toJson(response)
      json.validate[AvailableReportResponse] mustBe JsSuccess(response)
    }
  }

  "AvailableReportAction" should {
    "serialize and deserialize to/from JSON" in {
      val action = AvailableReportAction(
        fileName = "report.csv",
        fileURL = "http://example.com/report.csv",
        size = 2048L,
        fileType = FileType.CSV
      )

      val json = Json.toJson(action)
      json.as[AvailableReportAction] mustEqual action
    }

    "support equality" in {
      val action1 = AvailableReportAction("a.txt", "url", 1L, FileType.CSV)
      val action2 = AvailableReportAction("a.txt", "url", 1L, FileType.CSV)
      val action3 = AvailableReportAction("b.txt", "url", 1L, FileType.CSV)

      action1 mustEqual action2
      action1 must not equal action3
    }
  }

  "AvailableThirdPartyReportResponse" should {
    "serialize and deserialize correctly" in {
      val report = AvailableThirdPartyReportResponse(
        referenceNumber = "ref789",
        reportName = "Third Party Report 2",
        expiryDate = instantNow,
        reportType = ReportTypeName.EXPORTS_ITEM_REPORT,
        companyName = "Another Company",
        action = Seq(sampleAction, sampleAction.copy(fileName = "report2.csv", fileType = FileType.CSV))
      )
      val json   = Json.toJson(report)
      json.validate[AvailableThirdPartyReportResponse] mustBe JsSuccess(report)
    }

    "serialize and deserialize correctly with an empty action sequence" in {
      val report = AvailableThirdPartyReportResponse(
        referenceNumber = "ref101",
        reportName = "Third Party Report 3",
        expiryDate = instantNow,
        reportType = ReportTypeName.EXPORTS_ITEM_REPORT,
        companyName = "Empty Action Co",
        action = Seq.empty
      )
      val json   = Json.toJson(report)
      json.validate[AvailableThirdPartyReportResponse] mustBe JsSuccess(report)
    }
  }

  "AvailableUserReportResponse" should {
    "serialize and deserialize correctly" in {
      val report = AvailableUserReportResponse(
        referenceNumber = "refABC",
        reportName = "User Report 2",
        expiryDate = instantNow,
        reportType = ReportTypeName.IMPORTS_ITEM_REPORT,
        action = Seq(sampleAction, sampleAction.copy(fileName = "user_report.csv", fileType = FileType.CSV))
      )
      val json   = Json.toJson(report)
      json.validate[AvailableUserReportResponse] mustBe JsSuccess(report)
    }

    "serialize and deserialize correctly with an empty action sequence" in {
      val report = AvailableUserReportResponse(
        referenceNumber = "refDEF",
        reportName = "User Report 3",
        expiryDate = instantNow,
        reportType = ReportTypeName.EXPORTS_ITEM_REPORT,
        action = Seq.empty
      )
      val json   = Json.toJson(report)
      json.validate[AvailableUserReportResponse] mustBe JsSuccess(report)
    }
  }
}
