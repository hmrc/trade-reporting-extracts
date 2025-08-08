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

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.Json

class ReportConfirmationSpec extends AnyFreeSpec with Matchers {

  "ReportConfirmation" - {

    "must create an instance with given values" in {
      val rc = ReportConfirmation("Test Name", "Test Type", "REF123")
      rc.reportName mustBe "Test Name"
      rc.reportType mustBe "Test Type"
      rc.reportReference mustBe "REF123"
    }

    "must serialize to JSON" in {
      val rc = ReportConfirmation("Name", "Type", "Ref")
      val json = Json.toJson(rc)
      (json \ "reportName").as[String] mustBe "Name"
      (json \ "reportType").as[String] mustBe "Type"
      (json \ "reportReference").as[String] mustBe "Ref"
    }

    "must deserialize from JSON" in {
      val json = Json.parse("""
        {
          "reportName": "N",
          "reportType": "T",
          "reportReference": "R"
        }
      """)
      val rc = json.as[ReportConfirmation]
      rc mustBe ReportConfirmation("N", "T", "R")
    }
  }
}
