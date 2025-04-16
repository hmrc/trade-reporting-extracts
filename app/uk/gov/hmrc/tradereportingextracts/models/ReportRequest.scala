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

import play.api.libs.json.{Format, Json}

import java.time.Instant

case class ReportRequest(
  reportId: String,
  correlationId: String,
  reportName: String,
  requestorId: String,
  eoriRole: String,
  reportEORIs: Array[String],
  recipientEmails: Array[String],
  reportTypeName: String,
  reportStart: Instant,
  reportEnd: Instant,
  createDate: Instant,
  status: String,
  statusDetails: String,
  fileAvailableTime: Instant,
  linkAvailableTime: Instant
) {
  override def equals(obj: Any): Boolean = obj match {
    case that: ReportRequest =>
      this.requestorId == that.requestorId &&
      this.reportId == that.reportId &&
      this.correlationId == that.correlationId
    case _                   => false
  }
}

object ReportRequest:
  given mongoFormat: Format[ReportRequest] = Json.format[ReportRequest]
