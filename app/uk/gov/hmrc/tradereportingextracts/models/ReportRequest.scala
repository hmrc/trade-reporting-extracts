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
import uk.gov.hmrc.crypto.Sensitive.SensitiveString
import uk.gov.hmrc.crypto.json.JsonEncryption
import uk.gov.hmrc.crypto.{Decrypter, Encrypter}
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats.Implicits.*
import uk.gov.hmrc.tradereportingextracts.models.eis.EisReportStatusRequest

import java.time.Instant
import scala.util.{Failure, Success, Try}

case class ReportRequest(
  reportRequestId: String,
  correlationId: String,
  reportName: String,
  requesterEORI: String,
  eoriRole: EoriRole,
  reportEORIs: Seq[String],
  userEmail: Option[SensitiveString],
  recipientEmails: Seq[SensitiveString],
  reportTypeName: ReportTypeName,
  reportStart: Instant,
  reportEnd: Instant,
  createDate: Instant,
  notifications: Seq[EisReportStatusRequest],
  fileNotifications: Option[Seq[FileNotification]],
  updateDate: Instant
) {

  def isReportStatusComplete: Boolean =
    fileNotifications.exists { notifications =>
      val notificationsCount = notifications.size
      val lastNotification   = notifications.find(_.reportLastFile == "true")
      lastNotification match {
        case Some(last) =>
          Try(last.reportFilesParts.toInt) match {
            case Success(parts) => notificationsCount == parts
            case Failure(_)     => false
          }
        case _          => false
      }
    }
}

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
  def encryptedFormat(implicit crypto: Encrypter with Decrypter): OFormat[ReportRequest] = {
    val instantReads: Reads[Instant]                      = Reads { js =>
      (js \ "$date" \ "$numberLong").validate[String].map(str => Instant.ofEpochMilli(str.toLong))
    }
    val instantWrites: Writes[Instant]                    =
      (instant: Instant) => Json.obj("$date" -> instant.toEpochMilli)
    implicit val instantFormat: Format[Instant]           = Format(instantReads, instantWrites)
    implicit val sensitiveFormat: Format[SensitiveString] =
      JsonEncryption.sensitiveEncrypterDecrypter(SensitiveString.apply)
    Json.format[ReportRequest]
  }

  given CanEqual[ReportRequest, ReportRequest] = CanEqual.derived

object FileNotification:
  given format: Format[FileNotification]             = Json.format[FileNotification]
  given CanEqual[FileNotification, FileNotification] = CanEqual.derived
