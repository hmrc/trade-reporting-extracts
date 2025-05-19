package uk.gov.hmrc.tradereportingextracts.models.sdes

import play.api.libs.json.{Format, JsError, Json, Reads, Writes}

sealed trait FileAvailableMetadataItem {
  def key: String
  def value: String
}

object FileAvailableMetadataItem {
  case class RetentionDaysAvailableMetadataItem(value: String) extends FileAvailableMetadataItem {
    val key = "RETENTION_DAYS"
  }
  case class FileTypeAvailableMetadataItem(value: String) extends FileAvailableMetadataItem {
    val key = "FILE_TYPE"
  }
  case class EORIAvailableMetadataItem(value: String) extends FileAvailableMetadataItem {
    val key = "EORI"
  }
  case class MDTPReportXCorrelationIDAvailableMetadataItem(value: String) extends FileAvailableMetadataItem {
    val key = "MDTP-report-x-correlationID"
  }
  case class MDTPReportRequestIDAvailableMetadataItem(value: String) extends FileAvailableMetadataItem {
    val key = "MDTP-report-requestID"
  }
  case class MDTPReportTypeNameAvailableMetadataItem(value: String) extends FileAvailableMetadataItem {
    val key = "MDTP-reportTypeName"
  }
  case class ReportFilesPartsAvailableMetadataItem(value: String) extends FileAvailableMetadataItem {
    val key = "Report-files-parts"
  }

  // JSON Reads/Writes
  implicit val reads: Reads[FileAvailableMetadataItem] = Reads { json =>
    (json \ "key").validate[String].flatMap {
      case "RETENTION_DAYS"              => (json \ "value").validate[String].map(RetentionDaysAvailableMetadataItem(_))
      case "FILE_TYPE"                   => (json \ "value").validate[String].map(FileTypeAvailableMetadataItem(_))
      case "EORI"                        => (json \ "value").validate[String].map(EORIAvailableMetadataItem(_))
      case "MDTP-report-x-correlationID" =>
        (json \ "value").validate[String].map(MDTPReportXCorrelationIDAvailableMetadataItem(_))
      case "MDTP-report-requestID"       => (json \ "value").validate[String].map(MDTPReportRequestIDAvailableMetadataItem(_))
      case "MDTP-reportTypeName"         => (json \ "value").validate[String].map(MDTPReportTypeNameAvailableMetadataItem(_))
      case "Report-files-parts"          => (json \ "value").validate[String].map(ReportFilesPartsAvailableMetadataItem(_))
      case other                         => JsError(s"Unknown metadata key: $other")
    }
  }
  implicit val writes: Writes[FileAvailableMetadataItem] = Writes {
    case RetentionDaysAvailableMetadataItem(value)            => Json.obj("key" -> "RETENTION_DAYS", "value" -> value)
    case FileTypeAvailableMetadataItem(value)                 => Json.obj("key" -> "FILE_TYPE", "value" -> value)
    case EORIAvailableMetadataItem(value)                     => Json.obj("key" -> "EORI", "value" -> value)
    case MDTPReportXCorrelationIDAvailableMetadataItem(value) =>
      Json.obj("key" -> "MDTP-report-x-correlationID", "value" -> value)
    case MDTPReportRequestIDAvailableMetadataItem(value)      => Json.obj("key" -> "MDTP-report-requestID", "value" -> value)
    case MDTPReportTypeNameAvailableMetadataItem(value)       => Json.obj("key" -> "MDTP-reportTypeName", "value" -> value)
    case ReportFilesPartsAvailableMetadataItem(value)         => Json.obj("key" -> "Report-files-parts", "value" -> value)
  }
  implicit val format: Format[FileAvailableMetadataItem] = Format(reads, writes)
}