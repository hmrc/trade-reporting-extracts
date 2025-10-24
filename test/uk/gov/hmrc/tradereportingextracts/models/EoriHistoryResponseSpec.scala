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

import java.time.LocalDate

class EoriHistoryResponseSpec extends AnyFreeSpec with Matchers {

  "EoriHistoryResponse" - {
    "filterByDateRange must correctly filter EoriHistory entries by date range" in {
      val eoriHistories = Seq(EoriHistory("GB250520228000", Some("2009-05-16"), Some("2025-10-07")))

      // left outside
      EoriHistoryResponse(eoriHistories).filterByDateRange(
        LocalDate.parse("2000-04-16"),
        LocalDate.parse("2009-04-16")
      ) mustBe Seq.empty

      // left edge
      EoriHistoryResponse(eoriHistories).filterByDateRange(
        LocalDate.parse("2009-04-17"),
        LocalDate.parse("2009-05-17")
      ) mustBe eoriHistories

      // inside
      EoriHistoryResponse(eoriHistories).filterByDateRange(
        LocalDate.parse("2025-04-16"),
        LocalDate.parse("2025-05-16")
      ) mustBe eoriHistories

      // right edge
      EoriHistoryResponse(eoriHistories).filterByDateRange(
        LocalDate.parse("2025-10-06"),
        LocalDate.parse("2025-10-08")
      ) mustBe eoriHistories

      // right outside
      EoriHistoryResponse(eoriHistories).filterByDateRange(
        LocalDate.parse("2025-10-08"),
        LocalDate.parse("2025-11-16")
      ) mustBe Seq.empty
    }

  }
}
