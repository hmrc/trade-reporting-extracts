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

package uk.gov.hmrc.tradereportingextracts.models.eis

import play.api.libs.json.{Format, Json, Reads, Writes}

case class EisReportRequest(
  endDate: String,
  eori: List[String],
  eoriRole: EisReportRequest.EoriRole.EoriRole,
  reportTypeName: EisReportRequest.ReportTypeName.ReportTypeName,
  requestID: String,
  requestTimestamp: String,
  requesterEori: String,
  startDate: String
)

object EisReportRequest {
  implicit lazy val requestJsonFormat: Format[EisReportRequest] = Json.format[EisReportRequest]

  object EoriRole extends Enumeration {
    val TRADER: Value          = Value("TRADER")
    val DECLARANT: Value       = Value("DECLARANT")
    val TRADERDECLARANT: Value = Value("TRADER-DECLARANT")

    type EoriRole = Value
    implicit lazy val EoriRoleJsonFormat: Format[EoriRole] =
      Format(Reads.enumNameReads(EoriRole), Writes.enumNameWrites[EisReportRequest.EoriRole.type])
  }

  object ReportTypeName extends Enumeration {
    val IMPORTSITEMREPORT: Value    = Value("IMPORTS-ITEM-REPORT")
    val IMPORTSHEADERREPORT: Value  = Value("IMPORTS-HEADER-REPORT")
    val IMPORTSTAXLINEREPORT: Value = Value("IMPORTS-TAXLINE-REPORT")
    val EXPORTSITEMREPORT: Value    = Value("EXPORTS-ITEM-REPORT")

    type ReportTypeName = Value
    implicit lazy val ReportTypeNameJsonFormat: Format[ReportTypeName] =
      Format(Reads.enumNameReads(ReportTypeName), Writes.enumNameWrites[EisReportRequest.ReportTypeName.type])
  }
}
