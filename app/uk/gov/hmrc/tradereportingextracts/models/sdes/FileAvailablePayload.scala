package uk.gov.hmrc.tradereportingextracts.models.sdes

import play.api.libs.json.{Format, JsError, Json, Reads, Writes}

case class FileAvailablePayload(
  filename: String,
  downloadURL: String,
  fileSize: Long,
  metadata: Seq[FileAvailableMetadataItem]
)

object FileAvailablePayload {
  implicit val format: Format[FileAvailablePayload] =
    Json.format[FileAvailablePayload]
}
  


