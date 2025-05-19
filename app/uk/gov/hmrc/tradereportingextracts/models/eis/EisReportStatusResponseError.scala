package uk.gov.hmrc.tradereportingextracts.models.eis

import play.api.libs.json.*

case class EisReportStatusResponseError(
  errorDetail: EisReportStatusResponseErrorDetail
)

object EisReportStatusResponseError {
  implicit lazy val notifyReportStatus400ResponseJsonFormat: Format[EisReportStatusResponseError] =
    Json.format[EisReportStatusResponseError]

}
