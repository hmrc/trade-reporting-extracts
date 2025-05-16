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

package uk.gov.hmrc.tradereportingextracts.controllers

import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.tradereportingextracts.connectors.CustomsDataStoreConnector
import uk.gov.hmrc.tradereportingextracts.models.{EoriRole, ReportRequest, ReportRequestUserAnswersModel, ReportTypeName}
import uk.gov.hmrc.tradereportingextracts.repositories.ReportRequestRepository
import uk.gov.hmrc.tradereportingextracts.services.RequestReferenceService

import java.time.format.DateTimeFormatter
import java.time.{Instant, LocalDate, ZoneOffset}
import java.util.UUID
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ReportRequestController @Inject() (
  cc: ControllerComponents,
  customsDataStoreConnector: CustomsDataStoreConnector,
  requestReferenceService: RequestReferenceService,
  reportRequestRepository: ReportRequestRepository
)(implicit executionContext: ExecutionContext)
    extends BackendController(cc) {

  def createReportRequest: Action[JsValue] = Action.async(parse.json) { implicit request =>
    request.body.validate[ReportRequestUserAnswersModel] match {
      case JsSuccess(value, _) =>
        val formatter    = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val startEndDate =
          (LocalDate.parse(value.reportStartDate, formatter), LocalDate.parse(value.reportEndDate, formatter))
        for {
          userEmail   <- customsDataStoreConnector.getVerifiedEmailForReport(value.eori).map(i => i.address)
          eoriHistory <- customsDataStoreConnector
                           .getEoriHistory(value.whichEori.get)
                           .map(i => i.filterByDateRange(startEndDate._1, startEndDate._2).map(j => j.eori))
          newRequest   = transformReportRequest(value.eori, value, eoriHistory, userEmail)
          _           <- reportRequestRepository.insert(newRequest)
        } yield Ok(Json.obj("references" -> Seq(newRequest.reportRequestId)))
      case JsError(errors)     =>
        Future.successful(BadRequest)
    }

  }

  private def transformReportRequest(
    eoriValue: String,
    reportRequestUserAnswersModel: ReportRequestUserAnswersModel,
    historicalEoris: Seq[String],
    userEmail: String
  ): ReportRequest = {

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
        case roles if roles.contains("declarant") && (roles.contains("importer")) || roles.contains("exporter") =>
          EoriRole.TRADER_DECLARANT
      }

    ReportRequest(
      reportRequestId = requestReferenceService.random(),
      correlationId = UUID.randomUUID().toString,
      reportName = userAnswers.reportName,
      requesterEORI = eoriValue,
      eoriRole = getRole(userAnswers.eoriRole),
      reportEORIs = historicalEoris :+ userAnswers.whichEori.getOrElse(""),
      recipientEmails = userAnswers.additionalEmail.getOrElse(Seq()).toSeq :+ userEmail,
      reportTypeName = getReportType(userAnswers.reportType.head),
      reportStart = LocalDate.parse(userAnswers.reportStartDate).atStartOfDay(ZoneOffset.UTC).toInstant,
      reportEnd = LocalDate.parse(userAnswers.reportEndDate).atStartOfDay(ZoneOffset.UTC).toInstant,
      createDate = Instant.now,
      notifications = Seq(),
      linkAvailableTime = None,
      fileAvailableTime = None
    )
  }
}
