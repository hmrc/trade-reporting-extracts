package uk.gov.hmrc.tradereportingextracts.models.eis

import play.api.libs.json.*

case class EisReportStatusResponseErrorDetailSourceFaultDetail(
  detail: List[String],
  restFault: Option[JsObject],
  soapFault: Option[JsObject]
)

object EisReportStatusResponseErrorDetailSourceFaultDetail {
  implicit lazy val eisReportStatusResponseErrorDetailSourceFaultDetailFormat
    : Format[EisReportStatusResponseErrorDetailSourceFaultDetail] =
    Json.format[EisReportStatusResponseErrorDetailSourceFaultDetail]
}
