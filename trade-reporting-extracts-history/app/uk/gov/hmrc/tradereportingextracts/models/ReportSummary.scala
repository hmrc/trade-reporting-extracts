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

import play.api.libs.json.{Format, Json}

import java.time.Instant

sealed trait ReportSummary {
  def referenceNumber: String
  def reportName: String
  def requestedDate: Instant
  def reportType: ReportTypeName
  def reportStatus: ReportStatus
}

case class UserReport(
  referenceNumber: String,
  reportName: String,
  requestedDate: Instant,
  reportType: ReportTypeName,
  reportStatus: ReportStatus,
  reportStartDate: Instant,
  reportEndDate: Instant
) extends ReportSummary

object UserReport:
  given format: Format[UserReport] = Json.format[UserReport]

case class ThirdPartyReport(
  referenceNumber: String,
  reportName: String,
  requestedDate: Instant,
  reportType: ReportTypeName,
  companyName: String,
  reportStatus: ReportStatus,
  reportStartDate: Instant,
  reportEndDate: Instant
) extends ReportSummary

object ThirdPartyReport:
  given format: Format[ThirdPartyReport] = Json.format[ThirdPartyReport]

case class GetReportRequestsResponse(
  userReports: Option[Seq[UserReport]],
  thirdPartyReports: Option[Seq[ThirdPartyReport]]
)

object GetReportRequestsResponse:
  given format: Format[GetReportRequestsResponse] = Json.format[GetReportRequestsResponse]
