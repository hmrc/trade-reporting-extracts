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

import play.api.libs.json.{Format, Json, Reads, Writes}
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats.Implicits.*
import uk.gov.hmrc.tradereportingextracts.models.eis.EisReportStatusRequest

import java.time.Instant

case class ReportRequest(
  reportRequestId: String,
  correlationId: String,
  reportName: String,
  requesterEORI: String,
  eoriRole: EoriRole,
  reportEORIs: Seq[String],
  userEmail: Option[String],
  recipientEmails: Seq[String],
  reportTypeName: ReportTypeName,
  reportStart: Instant,
  reportEnd: Instant,
  createDate: Instant,
  notifications: Seq[EisReportStatusRequest],
  fileNotifications: Option[Seq[FileNotification]],
  updateDate: Instant
)

case class FileNotification(
  fileName: String,
  fileSize: Long,
  retentionDays: Int,
  fileType: String,
  mDTPReportXCorrelationID: String,
  mDTPReportRequestID: String,
  mDTPReportTypeName: String,
  reportFilesParts: String,
  reportLastFile: String,
  fileCreationTimestamp: String
)

object ReportRequest:
  private val instantReads: Reads[Instant]     = Reads { js =>
    (js \ "$date" \ "$numberLong").validate[String].map(str => Instant.ofEpochMilli(str.toLong))
  }
  private val instantWrites: Writes[Instant]   =
    (instant: Instant) => Json.obj("$date" -> instant.toEpochMilli)
  implicit val instantFormat: Format[Instant]  = Format(instantReads, instantWrites)
  given format: Format[ReportRequest]          = Json.format[ReportRequest]
  given CanEqual[ReportRequest, ReportRequest] = CanEqual.derived

object FileNotification:
  given format: Format[FileNotification]             = Json.format[FileNotification]
  given CanEqual[FileNotification, FileNotification] = CanEqual.derived
