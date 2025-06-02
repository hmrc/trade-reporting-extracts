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
    val TRE: Value  = Value("TRE")
    val CDAP: Value = Value("CDAP")
    val EIS: Value  = Value("EIS")
    val SDES: Value = Value("SDES")

    type ApplicationComponent = Value
    implicit lazy val ApplicationComponentJsonFormat: Format[Value] =
      Format(Reads.enumNameReads(this), Writes.enumNameWrites[EisReportStatusRequest.ApplicationComponent.type])
  }

  object StatusType extends Enumeration {
    val INFORMATION: Value = Value("INFORMATION")
    val ERROR: Value       = Value("ERROR")

    type StatusType = Value
    implicit lazy val StatusTypeJsonFormat: Format[Value] =
      Format(Reads.enumNameReads(this), Writes.enumNameWrites[EisReportStatusRequest.StatusType.type])
  }
}
