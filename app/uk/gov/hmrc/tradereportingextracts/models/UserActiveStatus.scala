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

import java.time.{Clock, Instant, LocalDate, ZoneOffset}

sealed abstract class UserActiveStatus(val displayName: String, val cssClass: String)

object UserActiveStatus {
  case object Active extends UserActiveStatus("Active", "govuk-tag--green")
  case object Upcoming extends UserActiveStatus("Upcoming", "govuk-tag--blue")

  def fromInstants(
    accessStart: Instant,
    reportDataStart: Option[Instant],
    clock: Clock = Clock.systemUTC()
  ): UserActiveStatus = {
    val now        = LocalDate.now(clock).atStartOfDay().toInstant(ZoneOffset.UTC)
    val cutoffDate = LocalDate.now(clock).minusDays(3).atStartOfDay().toInstant(ZoneOffset.UTC)

    val isAccessStarted     = !accessStart.isAfter(now)
    val isReportDataStarted = reportDataStart.forall(!_.isAfter(cutoffDate))

    if (isAccessStarted && isReportDataStarted) Active
    else Upcoming
  }

  implicit val userActiveStatusFormat: Format[UserActiveStatus] = new Format[UserActiveStatus] {
    override def reads(json: JsValue): JsResult[UserActiveStatus] = json match {
      case JsString("Active")   => JsSuccess(Active)
      case JsString("Upcoming") => JsSuccess(Upcoming)
      case _                    => JsError("Unknown UserActiveStatus")
    }

    override def writes(status: UserActiveStatus): JsValue = JsString(status.displayName)
  }

}
