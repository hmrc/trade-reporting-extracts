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

import play.api.libs.json.Json
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers.mustBe

import java.time.LocalDate

class ThirdPartyDetailsSpec extends AnyFreeSpec {

  "ThirdPartyDetails" - {

    "should serialise and deserialise to/from JSON correctly" in {
      val details = ThirdPartyDetails(
        referenceName = Some("ref"),
        accessStartDate = LocalDate.parse("2024-06-01"),
        accessEndDate = Some(LocalDate.parse("2024-06-30")),
        dataTypes = Set("import", "export"),
        dataStartDate = Some(LocalDate.parse("2024-05-01")),
        dataEndDate = Some(LocalDate.parse("2024-05-31"))
      )

      val json   = Json.toJson(details)
      val parsed = json.as[ThirdPartyDetails]

      parsed mustBe details
    }
  }
}
