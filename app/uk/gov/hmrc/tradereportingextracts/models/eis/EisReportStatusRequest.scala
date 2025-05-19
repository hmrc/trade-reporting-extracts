package uk.gov.hmrc.tradereportingextracts.models.eis

import play.api.libs.json.*

case class EisReportStatusRequest(
  applicationComponent: EisReportStatusRequest.ApplicationComponent.Value,
  statusCode: String,
  statusMessage: String,
  statusTimestamp: String,
  statusType: EisReportStatusRequest.StatusType.Value
)

object EisReportStatusRequest {
  implicit lazy val tREAPI6ReportStatusUpdateRequestJsonFormat: Format[EisReportStatusRequest] =
    Json.format[EisReportStatusRequest]

  object ApplicationComponent extends Enumeration {
    val CDAP = Value("CDAP")
    val EIS  = Value("EIS")
    val SDES = Value("SDES")

    type ApplicationComponent = Value
    implicit lazy val ApplicationComponentJsonFormat: Format[Value] =
      Format(Reads.enumNameReads(this), Writes.enumNameWrites[EisReportStatusRequest.ApplicationComponent.type])
  }

  object StatusType extends Enumeration {
    val INFORMATION = Value("INFORMATION")
    val ERROR       = Value("ERROR")

    type StatusType = Value
    implicit lazy val StatusTypeJsonFormat: Format[Value] =
      Format(Reads.enumNameReads(this), Writes.enumNameWrites[EisReportStatusRequest.StatusType.type])
  }
}
