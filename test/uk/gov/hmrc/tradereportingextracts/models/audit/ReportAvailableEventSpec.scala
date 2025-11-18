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
import org.scalatest.matchers.must.Matchers.mustBe
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.Json

class ReportAvailableEventSpec extends AnyFreeSpec with Matchers {

  "ReportAvailableEvent" - {

    "should serialise to the correct JSON structure" in {
      val event = ReportAvailableEvent(
        xCorrelationId = "corr-123"
      )

      Json.toJson(event) mustBe Json.obj(
        "xCorrelationId"         -> "corr-123"
      )
    }

    "should deserialise correctly from valid JSON" in {
      val json = Json.obj(
        "xCorrelationId" -> "corr-123"
      )

      json.as[ReportAvailableEvent] mustBe
        ReportAvailableEvent("corr-123")
    }

  }

}
