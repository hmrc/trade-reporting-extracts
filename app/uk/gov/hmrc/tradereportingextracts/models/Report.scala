package uk.gov.hmrc.tradereportingextracts.models

import play.api.libs.json.{Format, Json}

case class Report(userid: Long,
                  reportId: String,
                  templateId: String,
                  recipientEmails: Array[String],
                  reportEORIs: Array[String],
                  reportType: String,
                  reportStart: String,
                  reportEnd: String,
                  status: String,
                  statusDetails: String
                 )


object Report:
  given mongoFormat: Format[Report] = Json.format[Report]