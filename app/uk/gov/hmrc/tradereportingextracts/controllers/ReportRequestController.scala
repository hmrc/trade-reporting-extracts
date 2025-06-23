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
import uk.gov.hmrc.tradereportingextracts.models.eis.EisReportRequest
import uk.gov.hmrc.tradereportingextracts.models.{EoriRole, ReportRequest, ReportRequestUserAnswersModel, ReportTypeName}
import uk.gov.hmrc.tradereportingextracts.services.{EisService, ReportRequestService, ReportRequestTransformationService, RequestReferenceService}

import java.time.format.DateTimeFormatter
import java.time.{Instant, LocalDate, ZoneOffset}
import java.util.UUID
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ReportRequestController @Inject() (
  cc: ControllerComponents,
  customsDataStoreConnector: CustomsDataStoreConnector,
  requestReferenceService: RequestReferenceService,
  reportRequestService: ReportRequestService,
  reportRequestTransformationService: ReportRequestTransformationService,
  eisService: EisService
)(implicit executionContext: ExecutionContext)
    extends BackendController(cc) {

  def createReportRequest: Action[JsValue] = Action.async(parse.json) { implicit request =>
    request.body.validate[ReportRequestUserAnswersModel] match {
      case JsSuccess(value, _) =>
        val formatter    = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val startEndDate =
          (LocalDate.parse(value.reportStartDate, formatter), LocalDate.parse(value.reportEndDate, formatter))
        for {
          userEmail   <- customsDataStoreConnector.getNotificationEmail(value.eori).map(_.address)
          eoriHistory <- customsDataStoreConnector
                           .getEoriHistory(value.whichEori.get)
                           .map(_.filterByDateRange(startEndDate._1, startEndDate._2).map(_.eori))
          requests     = value.reportType.toSeq.map { reportTypeName =>
                           reportRequestTransformationService.transformReportRequest(
                             value.eori,
                             value.copy(reportType = Set(reportTypeName)),
                             eoriHistory,
                             userEmail
                           )
                         }
          _           <- Future.sequence(requests.map { newRequest =>
                           val eisRequest = reportRequestTransformationService.toEisReportRequest(newRequest)
                           for {
                             _      <- reportRequestService.create(newRequest)
                             result <- eisService.requestTraderReport(eisRequest, newRequest)
                           } yield result
                         })
        } yield Ok(Json.obj("references" -> requests.map(_.reportRequestId)))
      case JsError(_)          =>
        Future.successful(BadRequest)
    }
  }
}
