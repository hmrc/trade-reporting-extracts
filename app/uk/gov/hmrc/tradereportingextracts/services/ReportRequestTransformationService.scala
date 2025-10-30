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

package uk.gov.hmrc.tradereportingextracts.services

import uk.gov.hmrc.crypto.Sensitive.SensitiveString

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.tradereportingextracts.models.eis.EisReportRequest
import uk.gov.hmrc.tradereportingextracts.models.{EoriRole, ReportConfirmation, ReportRequest, ReportRequestUserAnswersModel, ReportTypeName}

import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.{Instant, LocalDate, ZoneOffset}
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ReportRequestTransformationService @Inject() (
  requestReferenceService: RequestReferenceService
)(implicit ec: ExecutionContext) {

  def transformReportRequest(
    eoriValue: String,
    reportRequestUserAnswersModel: ReportRequestUserAnswersModel,
    historicalEoris: Seq[String],
    userEmail: String
  ): Future[ReportRequest] = {

    val userAnswers = reportRequestUserAnswersModel

    def getReportType(reportTypes: String): ReportTypeName =
      reportTypes match {
        case x if x.contains("importHeader")  => ReportTypeName.IMPORTS_HEADER_REPORT
        case x if x.contains("importItem")    => ReportTypeName.IMPORTS_ITEM_REPORT
        case x if x.contains("importTaxLine") => ReportTypeName.IMPORTS_TAXLINE_REPORT
        case x if x.contains("exportItem")    => ReportTypeName.EXPORTS_ITEM_REPORT
      }

    def getRole(roles: Set[String]): EoriRole =
      roles match {
        case roles if roles == Set("declarant")                                                                 => EoriRole.DECLARANT
        case roles if roles.subsetOf(Set("importer", "exporter"))                                               => EoriRole.TRADER
        case roles if roles.contains("declarant") && (roles.contains("importer") || roles.contains("exporter")) =>
          EoriRole.TRADER_DECLARANT
      }

    requestReferenceService.generateUnique().map { uniqueId =>
      ReportRequest(
        reportRequestId = uniqueId,
        correlationId = UUID.randomUUID().toString,
        reportName = userAnswers.reportName,
        requesterEORI = eoriValue,
        eoriRole = getRole(userAnswers.eoriRole),
        reportEORIs = historicalEoris :+ userAnswers.whichEori.getOrElse(""),
        userEmail = Some(SensitiveString(userEmail)),
        recipientEmails = userAnswers.additionalEmail.getOrElse(Seq()).toSeq.map(email => SensitiveString(email)),
        reportTypeName = getReportType(userAnswers.reportType.head),
        reportStart = LocalDate.parse(userAnswers.reportStartDate).atStartOfDay(ZoneOffset.UTC).toInstant,
        reportEnd = LocalDate.parse(userAnswers.reportEndDate).atStartOfDay(ZoneOffset.UTC).toInstant,
        createDate = Instant.now,
        notifications = Seq(),
        fileNotifications = None,
        updateDate = Instant.now
      )
    }
  }

  def toEisReportRequest(reportRequest: ReportRequest): EisReportRequest =
    EisReportRequest(
      endDate = DateTimeFormatter.ISO_LOCAL_DATE.format(reportRequest.reportEnd.atZone(ZoneOffset.UTC)),
      eori = reportRequest.reportEORIs.toList,
      eoriRole = reportRequest.eoriRole match {
        case EoriRole.TRADER           => EisReportRequest.EoriRole.TRADER
        case EoriRole.DECLARANT        => EisReportRequest.EoriRole.DECLARANT
        case EoriRole.TRADER_DECLARANT => EisReportRequest.EoriRole.TRADERDECLARANT
      },
      reportTypeName = reportRequest.reportTypeName match {
        case ReportTypeName.IMPORTS_ITEM_REPORT    => EisReportRequest.ReportTypeName.IMPORTSITEMREPORT
        case ReportTypeName.IMPORTS_HEADER_REPORT  => EisReportRequest.ReportTypeName.IMPORTSHEADERREPORT
        case ReportTypeName.IMPORTS_TAXLINE_REPORT => EisReportRequest.ReportTypeName.IMPORTSTAXLINEREPORT
        case ReportTypeName.EXPORTS_ITEM_REPORT    => EisReportRequest.ReportTypeName.EXPORTSITEMREPORT
      },
      requestID = reportRequest.reportRequestId,
      requestTimestamp = DateTimeFormatter.ISO_INSTANT.format(reportRequest.createDate.truncatedTo(ChronoUnit.MILLIS)),
      requesterEori = reportRequest.requesterEORI,
      startDate = DateTimeFormatter.ISO_LOCAL_DATE.format(reportRequest.reportStart.atZone(ZoneOffset.UTC))
    )

  def reportConfirmationTransformer(updatedReports: Seq[ReportRequest]): Seq[ReportConfirmation] =
    updatedReports.map { report =>
      ReportConfirmation(
        reportName = report.reportName,
        reportType = report.reportTypeName match {
          case ReportTypeName.IMPORTS_ITEM_REPORT    => "importItem"
          case ReportTypeName.IMPORTS_HEADER_REPORT  => "importHeader"
          case ReportTypeName.IMPORTS_TAXLINE_REPORT => "importTaxLine"
          case ReportTypeName.EXPORTS_ITEM_REPORT    => "exportItem"
        },
        reportReference = report.reportRequestId
      )
    }

}
