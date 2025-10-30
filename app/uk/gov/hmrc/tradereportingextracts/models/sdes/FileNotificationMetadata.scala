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

package uk.gov.hmrc.tradereportingextracts.models.sdes

import play.api.libs.json._

sealed trait FileNotificationMetadata {
  def metadata: String
  def value: String
}

object FileNotificationMetadata {
  case class RetentionDaysMetadataItem(value: String) extends FileNotificationMetadata {
    val metadata = "RETENTION_DAYS"
  }
  case class FileTypeMetadataItem(value: String) extends FileNotificationMetadata {
    val metadata = "FileType"
  }
  case class EORIMetadataItem(value: String) extends FileNotificationMetadata {
    val metadata = "EORI"
  }
  case class MDTPReportXCorrelationIDMetadataItem(value: String) extends FileNotificationMetadata {
    val metadata = "MdtpReportXCorrelationId"
  }
  case class MDTPReportRequestIDMetadataItem(value: String) extends FileNotificationMetadata {
    val metadata = "MdtpReportRequestId"
  }
  case class MDTPReportTypeNameMetadataItem(value: String) extends FileNotificationMetadata {
    val metadata = "MdtpReportTypeName"
  }
  case class ReportFilesPartsMetadataItem(value: String) extends FileNotificationMetadata {
    val metadata = "ReportFileCounter"
  }
  case class ReportLastFileMetadataItem(value: String) extends FileNotificationMetadata {
    val metadata = "ReportLastFile"
  }
  case class FileCreationTimestampMetadataItem(value: String) extends FileNotificationMetadata {
    val metadata = "FileCreationTimestamp"
  }

  // JSON Reads/Writes
  implicit val reads: Reads[FileNotificationMetadata] = Reads { json =>
    (json \ "metadata").validate[String].flatMap {
      case "RETENTION_DAYS"           => (json \ "value").validate[String].map(RetentionDaysMetadataItem(_))
      case "FileType"                 => (json \ "value").validate[String].map(FileTypeMetadataItem(_))
      case "EORI"                     => (json \ "value").validate[String].map(EORIMetadataItem(_))
      case "MdtpReportXCorrelationId" =>
        (json \ "value").validate[String].map(MDTPReportXCorrelationIDMetadataItem(_))
      case "MdtpReportRequestId"      => (json \ "value").validate[String].map(MDTPReportRequestIDMetadataItem(_))
      case "MdtpReportTypeName"       => (json \ "value").validate[String].map(MDTPReportTypeNameMetadataItem(_))
      case "ReportFileCounter"        => (json \ "value").validate[String].map(ReportFilesPartsMetadataItem(_))
      case "ReportLastFile"           => (json \ "value").validate[String].map(ReportLastFileMetadataItem(_))
      case "FileCreationTimestamp"    => (json \ "value").validate[String].map(FileCreationTimestampMetadataItem(_))
    }
  }

  implicit val writes: Writes[FileNotificationMetadata] = Writes {
    case RetentionDaysMetadataItem(value)            => Json.obj("metadata" -> "RETENTION_DAYS", "value" -> value)
    case FileTypeMetadataItem(value)                 => Json.obj("metadata" -> "FileType", "value" -> value)
    case EORIMetadataItem(value)                     => Json.obj("metadata" -> "EORI", "value" -> value)
    case MDTPReportXCorrelationIDMetadataItem(value) =>
      Json.obj("metadata" -> "MdtpReportXCorrelationId", "value" -> value)
    case MDTPReportRequestIDMetadataItem(value)      => Json.obj("metadata" -> "MdtpReportRequestId", "value" -> value)
    case MDTPReportTypeNameMetadataItem(value)       => Json.obj("metadata" -> "MdtpReportTypeName", "value" -> value)
    case ReportFilesPartsMetadataItem(value)         => Json.obj("metadata" -> "ReportFileCounter", "value" -> value)
    case ReportLastFileMetadataItem(value)           => Json.obj("metadata" -> "ReportLastFile", "value" -> value)
    case FileCreationTimestampMetadataItem(value)    => Json.obj("metadata" -> "FileCreationTimestamp", "value" -> value)
  }

  implicit val format: Format[FileNotificationMetadata] = Format(reads, writes)
}
