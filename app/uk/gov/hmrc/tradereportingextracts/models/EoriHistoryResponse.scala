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
    val fromInstant: Instant  = from.atStartOfDay(ZoneOffset.UTC).toInstant
    val untilInstant: Instant = until.atTime(23, 59, 59).atZone(ZoneOffset.UTC).toInstant
    eoriHistory.filter { h =>
      val validFrom  = h.validFrom.getOrElse(Instant.MIN)
      val validUntil = h.validUntil.getOrElse(Instant.MAX)
      !validUntil.isBefore(fromInstant) && !validFrom.isAfter(untilInstant)
    }
}

object EoriHistoryResponse:
  implicit val format: OFormat[EoriHistoryResponse] = Json.format[EoriHistoryResponse]
