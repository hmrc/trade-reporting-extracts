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

import play.api.libs.json.{Format, JsError, Json, Reads, Writes}

case class FileAvailableResponse(
  filename: String,
  downloadURL: String,
  fileSize: Long,
  metadata: Seq[FileAvailableMetadataItem]
)

object FileAvailableResponse {
  implicit val format: Format[FileAvailableResponse] =
    Json.format[FileAvailableResponse]
}

sealed trait FileAvailableMetadataItem {
  def metadata: String
  def value: String
}

object FileAvailableMetadataItem {
  case class FileCreationTimestampMetadataItem(value: String) extends FileAvailableMetadataItem {
    val metadata = "FileCreationTimestamp"
  }
  case class FileTypeMetadataItem(value: String) extends FileAvailableMetadataItem {
    val metadata = "FileType"
  }
  case class EORIMetadataItem(value: String) extends FileAvailableMetadataItem {
    val metadata = "EORI"
  }
  case class MDTPReportXCorrelationIDMetadataItem(value: String) extends FileAvailableMetadataItem {
    val metadata = "MdtpReportXCorrelationId"
  }
  case class MDTPReportRequestIDMetadataItem(value: String) extends FileAvailableMetadataItem {
    val metadata = "MdtpReportRequestId"
  }
  case class MDTPReportTypeNameMetadataItem(value: String) extends FileAvailableMetadataItem {
    val metadata = "MdtpReportTypeName"
  }
  case class ReportFileCounterMetadataItem(value: String) extends FileAvailableMetadataItem {
    val metadata = "ReportFileCounter"
  }
  case class ReportLastFileMetadataItem(value: String) extends FileAvailableMetadataItem {
    val metadata = "ReportLastFile"
  }

  import play.api.libs.json._

  implicit val reads: Reads[FileAvailableMetadataItem] = Reads { json =>
    (json \ "metadata").validate[String].flatMap {
      case "FileCreationTimestamp"    => (json \ "value").validate[String].map(FileCreationTimestampMetadataItem(_))
      case "FileType"                 => (json \ "value").validate[String].map(FileTypeMetadataItem(_))
      case "EORI"                     => (json \ "value").validate[String].map(EORIMetadataItem(_))
      case "MdtpReportXCorrelationId" => (json \ "value").validate[String].map(MDTPReportXCorrelationIDMetadataItem(_))
      case "MdtpReportRequestId"      => (json \ "value").validate[String].map(MDTPReportRequestIDMetadataItem(_))
      case "MdtpReportTypeName"       => (json \ "value").validate[String].map(MDTPReportTypeNameMetadataItem(_))
      case "ReportFileCounter"        => (json \ "value").validate[String].map(ReportFileCounterMetadataItem(_))
      case "ReportLastFile"           => (json \ "value").validate[String].map(ReportLastFileMetadataItem(_))
    }
  }

  implicit val writes: Writes[FileAvailableMetadataItem] = Writes {
    case FileCreationTimestampMetadataItem(value)    => Json.obj("metadata" -> "FileCreationTimestamp", "value" -> value)
    case FileTypeMetadataItem(value)                 => Json.obj("metadata" -> "FileType", "value" -> value)
    case EORIMetadataItem(value)                     => Json.obj("metadata" -> "EORI", "value" -> value)
    case MDTPReportXCorrelationIDMetadataItem(value) =>
      Json.obj("metadata" -> "MdtpReportXCorrelationId", "value" -> value)
    case MDTPReportRequestIDMetadataItem(value)      => Json.obj("metadata" -> "MdtpReportRequestId", "value" -> value)
    case MDTPReportTypeNameMetadataItem(value)       => Json.obj("metadata" -> "MdtpReportTypeName", "value" -> value)
    case ReportFileCounterMetadataItem(value)        => Json.obj("metadata" -> "ReportFileCounter", "value" -> value)
    case ReportLastFileMetadataItem(value)           => Json.obj("metadata" -> "ReportLastFile", "value" -> value)
  }

  implicit val format: Format[FileAvailableMetadataItem] = Format(reads, writes)
}
