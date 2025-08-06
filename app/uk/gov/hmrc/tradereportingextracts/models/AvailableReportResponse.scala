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

package uk.gov.hmrc.tradereportingextracts.models

import play.api.libs.json.{Json, OFormat}

import java.time.Instant

case class AvailableReportResponse(
  availableUserReports: Option[Seq[AvailableUserReportResponse]],
  availableThirdPartyReports: Option[Seq[AvailableThirdPartyReportResponse]]
)
object AvailableReportResponse {
  implicit val format: play.api.libs.json.OFormat[AvailableReportResponse] =
    play.api.libs.json.Json.format[AvailableReportResponse]
}

case class AvailableThirdPartyReportResponse(
  referenceNumber: String,
  reportName: String,
  expiryDate: Instant,
  reportType: ReportTypeName,
  companyName: String,
  action: Seq[AvailableReportAction]
)
object AvailableThirdPartyReportResponse {
  implicit val format: OFormat[AvailableThirdPartyReportResponse] = Json.format[AvailableThirdPartyReportResponse]
}

case class AvailableUserReportResponse(
  referenceNumber: String,
  reportName: String,
  expiryDate: Instant,
  reportType: ReportTypeName,
  action: Seq[AvailableReportAction]
)

object AvailableUserReportResponse {
  implicit val format: OFormat[AvailableUserReportResponse] = Json.format[AvailableUserReportResponse]
}

case class AvailableReportAction(
  fileName: String,
  fileURL: String,
  size: Long,
  fileType: FileType
)

object AvailableReportAction {
  implicit val format: OFormat[AvailableReportAction] = Json.format[AvailableReportAction]
}
