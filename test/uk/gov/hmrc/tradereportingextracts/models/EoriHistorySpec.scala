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

package models
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.*
import uk.gov.hmrc.tradereportingextracts.models.EoriHistory

import java.time.LocalDate

class EoriHistorySpec extends AnyFreeSpec with Matchers {

  "EoriHistory" - {

    "must serialize and deserialize correctly with valid dates" in {
      val eoriHistory = EoriHistory("GB1234567890", Some("2023-01-01"), Some("2024-01-01"))
      val json        = Json.toJson(eoriHistory)
      json mustBe Json.obj(
        "eori"       -> "GB1234567890",
        "validFrom"  -> "2023-01-01",
        "validUntil" -> "2024-01-01"
      )
      Json.fromJson[EoriHistory](json) mustBe JsSuccess(eoriHistory)
    }
  }

  "must serialize and deserialize correctly with missing dates" in {
    val eoriHistory = EoriHistory("GB1234567890", None, None)
    val json        = Json.toJson(eoriHistory)
    json mustBe Json.obj(
      "eori" -> "GB1234567890"
    )
    Json.fromJson[EoriHistory](json) mustBe JsSuccess(eoriHistory)
  }

  "must parse validFrom and validUntil as LocalDate" in {
    val eoriHistory = EoriHistory("GB1234567890", Some("2023-01-01"), Some("2024-01-01"))
    eoriHistory.validFromLocalDate mustBe LocalDate.parse("2023-01-01")
    eoriHistory.validUntilLocalDate mustBe LocalDate.parse("2024-01-01")
  }

  "must handle missing validFrom and validUntil dates as LocalDate.MIN and LocalDate.MAX" in {
    val eoriHistory = EoriHistory("GB1234567890", None, None)
    eoriHistory.validFromLocalDate mustBe LocalDate.MIN
    eoriHistory.validUntilLocalDate mustBe LocalDate.MAX
  }

}
