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

package uk.gov.hmrc.tradereportingextracts.models.audit

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.{JsSuccess, Json}

import java.time.Instant

class ReportRequestSubmittedEventSpec extends AnyFreeSpec with Matchers {

  "ReportRequestSubmittedEvent" - {

    val now = Instant.parse("2025-07-30T12:00:00Z")

    val reportDetail =
      ReportDetail(requestId = "REQ123", reportTypeName = "IMPORTS_ITEM_REPORT", outcomeIsSuccessful = true)

    val event = ReportRequestSubmittedEvent(
      submissionStatus = "Complete",
      numberOfReports = 1,
      requesterEori = "GB123456789000",
      reportSubjectEori = "GB987654321000",
      reportSubjectRole = "importer",
      reportAlias = "Trader Report",
      reportStart = now,
      reportEnd = now,
      submittedAt = now,
      reports = Seq(reportDetail)
    )

    "must serialize to JSON correctly" in {
      val json = Json.toJson(event)
      (json \ "submissionStatus").as[String] mustBe "Complete"
      (json \ "numberOfReports").as[Int] mustBe 1
      (json \ "reports")(0).\("requestId").as[String] mustBe "REQ123"
    }

    "must deserialize from JSON correctly" in {
      val json   = Json.toJson(event)
      val result = Json.fromJson[ReportRequestSubmittedEvent](json)
      result mustBe JsSuccess(event)
    }
  }
}
