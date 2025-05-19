package uk.gov.hmrc.tradereportingextracts.models.sdes

import play.api.libs.json.*

sealed trait FileAvailableMetadataItem {
  def metadata: String
  def value: String
}

object FileAvailableMetadataItem {
  case class RetentionDaysMetadataItem(value: String) extends FileAvailableMetadataItem {
    val metadata = "RETENTION_DAYS"
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

  implicit val reads: Reads[FileAvailableMetadataItem] = Reads { json =>
    (json \ "metadata").validate[String].flatMap {
      case "RETENTION_DAYS"           => (json \ "value").validate[String].map(RetentionDaysMetadataItem(_))
      case "FileType"                 => (json \ "value").validate[String].map(FileTypeMetadataItem(_))
      case "EORI"                     => (json \ "value").validate[String].map(EORIMetadataItem(_))
      case "MdtpReportXCorrelationId" => (json \ "value").validate[String].map(MDTPReportXCorrelationIDMetadataItem(_))
      case "MdtpReportRequestId"      => (json \ "value").validate[String].map(MDTPReportRequestIDMetadataItem(_))
      case "MdtpReportTypeName"       => (json \ "value").validate[String].map(MDTPReportTypeNameMetadataItem(_))
      case "ReportFileCounter"        => (json \ "value").validate[String].map(ReportFileCounterMetadataItem(_))
      case "ReportLastFile"           => (json \ "value").validate[String].map(ReportLastFileMetadataItem(_))
      case other                      => JsError(s"Unknown metadata metadata: $other")
    }
  }

  implicit val writes: Writes[FileAvailableMetadataItem] = Writes {
    case RetentionDaysMetadataItem(value)            => Json.obj("metadata" -> "RETENTION_DAYS", "value" -> value)
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
