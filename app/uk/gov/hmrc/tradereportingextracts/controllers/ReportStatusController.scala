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

import play.api.Logging
import play.api.libs.json.*
import play.api.mvc.*
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.tradereportingextracts.config.AppConfig
import uk.gov.hmrc.tradereportingextracts.models.eis.*
import uk.gov.hmrc.tradereportingextracts.models.eis.EisReportStatusHeaders.*
import uk.gov.hmrc.tradereportingextracts.services.ReportRequestService
import uk.gov.hmrc.tradereportingextracts.utils.HeaderUtils
import uk.gov.hmrc.tradereportingextracts.utils.HttpDateFormatter.getCurrentHttpDate

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ReportStatusController @Inject() (
  reportRequestService: ReportRequestService,
  cc: ControllerComponents,
  appConfig: AppConfig
)(using ec: ExecutionContext)
    extends AbstractController(cc)
    with Logging {

  def notifyReportStatus(): Action[AnyContent] = Action.async { request =>
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequest(request)
    def missingHeaders: Seq[String] =
      HeaderUtils.missingHeaders(request, EisReportStatusHeaders.allHeaders)

    def isAuthorized: Boolean =
      HeaderUtils.isAuthorized(request, appConfig.eisAPI6AuthToken, Authorization.toString)

    (missingHeaders, isAuthorized, request.body.asJson) match {
      case (headers, _, _) if headers.nonEmpty =>
        logger.error(
          s"reportstatusnotification missing required headers for CorrelationID: ${request.headers.get(XCorrelationID.toString).getOrElse("")}"
        )
        Future.successful(
          BadRequest.withHeaders(
            Date.toString           -> getCurrentHttpDate,
            XCorrelationID.toString -> request.headers.get(XCorrelationID.toString).getOrElse("")
          )
        )
      case (_, false, _)                       =>
        logger.error(
          s"reportstatusnotification unauthorised request for CorrelationID: ${request.headers.get(XCorrelationID.toString).getOrElse("")}"
        )
        Future.successful(
          Forbidden.withHeaders(
            Date.toString           -> getCurrentHttpDate,
            XCorrelationID.toString -> request.headers.get(XCorrelationID.toString).getOrElse("")
          )
        )
      case (_, _, None)                        =>
        logger.error(
          s"reportstatusnotification missing request body for CorrelationID: ${request.headers.get(XCorrelationID.toString).getOrElse("")}"
        )
        Future.successful(
          BadRequest.withHeaders(
            Date.toString           -> getCurrentHttpDate,
            XCorrelationID.toString -> request.headers.get(XCorrelationID.toString).getOrElse("")
          )
        )
      case (_, _, Some(json))                  =>
        json.validate[EisReportStatusRequest] match {
          case JsSuccess(_, _) =>
            reportRequestService.processReportStatus(request.headers, json.as[EisReportStatusRequest])
            Future.successful(
              Created.withHeaders(
                Date.toString           -> getCurrentHttpDate,
                XCorrelationID.toString -> request.headers.get(XCorrelationID.toString).getOrElse("")
              )
            )
          case JsError(errors) =>
            logger.error(
              s"reportstatusnotification invalid request body for CorrelationID: ${request.headers.get(XCorrelationID.toString).getOrElse("")}"
            )
            Future.successful(
              BadRequest.withHeaders(
                Date.toString           -> getCurrentHttpDate,
                XCorrelationID.toString -> request.headers.get(XCorrelationID.toString).getOrElse("")
              )
            )
        }
    }
  }
}
