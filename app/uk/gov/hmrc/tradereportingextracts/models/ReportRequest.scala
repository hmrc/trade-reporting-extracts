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
  reportRequestId: String,
  correlationId: String,
  reportName: String,
  requesterEORI: String,
  eoriRole: EoriRole,
  reportEORIs: Seq[String],
  recipientEmails: Seq[String],
  reportTypeName: ReportTypeName,
  reportStart: Instant,
  reportEnd: Instant,
  createDate: Instant,
  notifications: Seq[Notification],
  fileAvailableTime: Option[Instant],
  linkAvailableTime: Option[Instant]
)

case class Notification(component: Component, statusType: StatusType, statusCode: StatusCode, statusMessage: String)

object ReportRequest:
  given format: Format[ReportRequest]          = Json.format[ReportRequest]
  given CanEqual[ReportRequest, ReportRequest] = CanEqual.derived

object Notification:
  given format: Format[Notification]         = Json.format[Notification]
  given CanEqual[Notification, Notification] = CanEqual.derived
