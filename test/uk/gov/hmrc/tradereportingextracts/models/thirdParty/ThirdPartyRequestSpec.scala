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

import java.time.Instant

class ThirdPartyRequestSpec extends AnyFreeSpec with Matchers {

  "ThirdPartyRequest" - {

    "must serialize and deserialize correctly" in {
      val request = ThirdPartyRequest(
        userEORI = "GB987654321098",
        thirdPartyEORI = "GB123456123456",
        accessStart = Instant.parse("2025-09-09T00:00:00Z"),
        accessEnd = Some(Instant.parse("2025-09-09T10:59:38.334682780Z")),
        reportDateStart = Some(Instant.parse("2025-09-10T00:00:00Z")),
        reportDateEnd = Some(Instant.parse("2025-09-09T10:59:38.334716742Z")),
        accessType = Set("IMPORT", "EXPORT"),
        referenceName = Some("TestReport")
      )

      val json = Json.toJson(request)
      json.as[ThirdPartyRequest] mustBe request
    }

    "must have correct field values" in {
      val request = ThirdPartyRequest(
        userEORI = "GB1",
        thirdPartyEORI = "GB2",
        accessStart = Instant.parse("2024-01-01T00:00:00Z"),
        accessEnd = None,
        reportDateStart = None,
        reportDateEnd = None,
        accessType = Set("IMPORT"),
        referenceName = None
      )

      request.userEORI mustBe "GB1"
      request.accessType must contain("IMPORT")
      request.accessEnd mustBe None
    }
  }
}
