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

import play.api.libs.json.*
import org.scalatest.OptionValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

class ReportRequestUserAnswersModelSpec extends AnyFreeSpec with Matchers with OptionValues {

  "ReportRequestUserAnswersModel" - {

    "serialize to JSON" in {
      val model = ReportRequestUserAnswersModel(
        eori = "GB1234567890",
        dataType = "import",
        whichEori = "GB1234567890",
        eoriRole = Set("declarant"),
        reportType = Set("importHeader", "importTaxLine"),
        reportStartDate = "2021-01-01",
        reportEndDate = "2021-12-31",
        reportName = "MyReport",
        additionalEmail = Some(Set("test@example.com", "another@example.com"))
      )

      val json = Json.toJson(model)

      (json \ "eori").as[String] mustBe "GB1234567890"
      (json \ "dataType").as[String] mustBe "import"
      (json \ "whichEori").as[String] mustBe "GB1234567890"
      (json \ "eoriRole").as[Set[String]] mustBe Set("declarant")
      (json \ "reportType").as[Set[String]] mustBe Set("importHeader", "importTaxLine")
      (json \ "reportStartDate").as[String] mustBe "2021-01-01"
      (json \ "reportEndDate").as[String] mustBe "2021-12-31"
      (json \ "reportName").as[String] mustBe "MyReport"
      (json \ "additionalEmail").asOpt[Set[String]] mustBe Some(Set("test@example.com", "another@example.com"))

    }

    "deserialize from JSON" in {
      val json = Json.parse("""
          {
            "eori": "GB1234567890",
            "dataType": "import",
            "whichEori": "GB1234567890",
            "eoriRole": ["declarant"],
            "reportType": ["importHeader", "importTaxLine"],
            "reportStartDate": "2021-01-01",
            "reportEndDate": "2021-12-31",
            "reportName": "MyReport",
            "additionalEmail": ["test@example.com", "another@example.com"]
          }
        """)

      val model = json.as[ReportRequestUserAnswersModel]

      model.eori mustBe "GB1234567890"
      model.dataType mustBe "import"
      model.whichEori mustBe "GB1234567890"
      model.eoriRole mustBe Set("declarant")
      model.reportType mustBe Set("importHeader", "importTaxLine")
      model.reportStartDate mustBe "2021-01-01"
      model.reportEndDate mustBe "2021-12-31"
      model.reportName mustBe "MyReport"
      model.additionalEmail mustBe Some(Set("test@example.com", "another@example.com"))
    }
  }

}
