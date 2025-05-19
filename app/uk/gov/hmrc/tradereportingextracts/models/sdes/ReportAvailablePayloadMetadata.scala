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

sealed trait ReportAvailablePayloadMetadata {
  def key: String
  def value: String
}

object ReportAvailablePayloadMetadata {
  case class RetentionDaysMetadataItem(value: String) extends ReportAvailablePayloadMetadata {
    val key = "RETENTION_DAYS"
  }
  case class FileTypeMetadataItem(value: String) extends ReportAvailablePayloadMetadata {
    val key = "FILE_TYPE"
  }
  case class EORIMetadataItem(value: String) extends ReportAvailablePayloadMetadata {
    val key = "EORI"
  }
  case class MDTPReportXCorrelationIDMetadataItem(value: String) extends ReportAvailablePayloadMetadata {
    val key = "MDTP-report-x-correlationID"
  }
  case class MDTPReportRequestIDMetadataItem(value: String) extends ReportAvailablePayloadMetadata {
    val key = "MDTP-report-requestID"
  }
  case class MDTPReportTypeNameMetadataItem(value: String) extends ReportAvailablePayloadMetadata {
    val key = "MDTP-reportTypeName"
  }
  case class ReportFilesPartsMetadataItem(value: String) extends ReportAvailablePayloadMetadata {
    val key = "Report-files-parts"
  }

  // JSON Reads/Writes
  implicit val reads: Reads[ReportAvailablePayloadMetadata] = Reads { json =>
    (json \ "key").validate[String].flatMap {
      case "RETENTION_DAYS"              => (json \ "value").validate[String].map(RetentionDaysMetadataItem(_))
      case "FILE_TYPE"                   => (json \ "value").validate[String].map(FileTypeMetadataItem(_))
      case "EORI"                        => (json \ "value").validate[String].map(EORIMetadataItem(_))
      case "MDTP-report-x-correlationID" => (json \ "value").validate[String].map(MDTPReportXCorrelationIDMetadataItem(_))
      case "MDTP-report-requestID"       => (json \ "value").validate[String].map(MDTPReportRequestIDMetadataItem(_))
      case "MDTP-reportTypeName"         => (json \ "value").validate[String].map(MDTPReportTypeNameMetadataItem(_))
      case "Report-files-parts"          => (json \ "value").validate[String].map(ReportFilesPartsMetadataItem(_))
      case other                         => JsError(s"Unknown metadata key: $other")
    }
  }

  implicit val writes: Writes[ReportAvailablePayloadMetadata] = Writes {
    case RetentionDaysMetadataItem(value)            => Json.obj("key" -> "RETENTION_DAYS", "value" -> value)
    case FileTypeMetadataItem(value)                 => Json.obj("key" -> "FILE_TYPE", "value" -> value)
    case EORIMetadataItem(value)                     => Json.obj("key" -> "EORI", "value" -> value)
    case MDTPReportXCorrelationIDMetadataItem(value) =>
      Json.obj("key" -> "MDTP-report-x-correlationID", "value" -> value)
    case MDTPReportRequestIDMetadataItem(value)      => Json.obj("key" -> "MDTP-report-requestID", "value" -> value)
    case MDTPReportTypeNameMetadataItem(value)       => Json.obj("key" -> "MDTP-reportTypeName", "value" -> value)
    case ReportFilesPartsMetadataItem(value)         => Json.obj("key" -> "Report-files-parts", "value" -> value)
  }

  implicit val format: Format[ReportAvailablePayloadMetadata] = Format(reads, writes)
}
