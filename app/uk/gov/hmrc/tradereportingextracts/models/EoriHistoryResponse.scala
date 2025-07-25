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

import play.api.libs.json.{Json, OFormat}

import java.time.{Instant, LocalDate, ZoneOffset}

case class EoriHistoryResponse(var eoriHistory: Seq[EoriHistory]) {
  def filterByDateRange(from: LocalDate, until: LocalDate): Seq[EoriHistory] =
    eoriHistory.filter { history =>
      val validFrom  = history.validFromLocalDate
      val validUntil = history.validUntilLocalDate
      (validFrom.compareTo(from) >= 0 && validFrom.compareTo(until) <= 0) ||
      (validUntil.compareTo(from) >= 0 && validUntil.compareTo(until) <= 0)
    }
}

object EoriHistoryResponse:
  implicit val format: OFormat[EoriHistoryResponse] = Json.format[EoriHistoryResponse]
