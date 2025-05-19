package uk.gov.hmrc.tradereportingextracts.models.eis

import play.api.libs.json.*

case class EisReportStatusResponseErrorDetail(
  correlationId: String,
  errorCode: Option[String],
  errorMessage: Option[String],
  source: Option[String],
  sourceFaultDetail: Option[EisReportStatusResponseErrorDetailSourceFaultDetail],
  timestamp: String
)

object EisReportStatusResponseErrorDetail {
  implicit lazy val eisReportStatusResponseErrorDetailJsonFormat: Format[EisReportStatusResponseErrorDetail] =
    Json.format[EisReportStatusResponseErrorDetail]
}
