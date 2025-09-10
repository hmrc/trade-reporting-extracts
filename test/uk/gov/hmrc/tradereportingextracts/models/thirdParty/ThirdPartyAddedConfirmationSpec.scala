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

package uk.gov.hmrc.tradereportingextracts.models.thirdParty

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.Json

class ThirdPartyAddedConfirmationSpec extends AnyFreeSpec with Matchers {

  "ThirdPartyAddedConfirmation" - {

    "must serialize and deserialize correctly" in {
      val confirmation = ThirdPartyAddedConfirmation(
        thirdPartyEori = "GB123456789000"
      )

      val json = Json.toJson(confirmation)
      json.as[ThirdPartyAddedConfirmation] mustBe confirmation
    }

    "must have correct field values" in {
      val confirmation = ThirdPartyAddedConfirmation(
        thirdPartyEori = "GB987654321098"
      )

      confirmation.thirdPartyEori mustBe "GB987654321098"
    }
  }
}
