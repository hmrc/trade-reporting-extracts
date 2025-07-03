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

import play.api.libs.json.*
import play.api.mvc.*
import sttp.model.MediaType.ApplicationJson
import uk.gov.hmrc.tradereportingextracts.config.AppConfig
import uk.gov.hmrc.tradereportingextracts.models.eis.EisReportStatusHeaders.*
import uk.gov.hmrc.tradereportingextracts.models.eis.*
import uk.gov.hmrc.tradereportingextracts.services.ReportRequestService
import uk.gov.hmrc.tradereportingextracts.utils.HttpDateFormatter.getCurrentHttpDate

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ReportStatusController @Inject() (
  reportRequestService: ReportRequestService,
  cc: ControllerComponents,
  appConfig: AppConfig
)(using ec: ExecutionContext)
    extends AbstractController(cc) {

  def notifyReportStatus(): Action[AnyContent] = Action.async { request =>
    def missingHeaders: Seq[String] =
      EisReportStatusHeaders.allHeaders.filterNot(header => request.headers.get(header).isDefined)

    def isAuthorized: Boolean =
      request.headers.get(Authorization.toString).exists { authHeader =>
        authHeader.startsWith("Bearer ") &&
        authHeader.substring("Bearer ".length) == appConfig.eisAPI6AuthToken
      }

    (missingHeaders, isAuthorized, request.body.asJson) match {
      case (headers, _, _) if headers.nonEmpty =>
        val errorMessage = s"Missing headers: ${headers.mkString(", ")}"
        Future.successful(
          BadRequest(buildBadRequestResponse(request, errorMessage)).withHeaders(
            ContentType.toString    -> ApplicationJson.toString(),
            Date.toString           -> getCurrentHttpDate,
            XCorrelationID.toString -> request.headers.get(XCorrelationID.toString).getOrElse("")
          )
        )
      case (_, false, _)                       =>
        val errorMessage = "Unauthorized request: Invalid or missing authorization header"
        Future.successful(
          Forbidden.withHeaders(
            Date.toString           -> getCurrentHttpDate,
            XCorrelationID.toString -> request.headers.get(XCorrelationID.toString).getOrElse("")
          )
        )
      case (_, _, None)                        =>
        val errorMessage = "Expected application/json request body"
        Future.successful(
          BadRequest(buildBadRequestResponse(request, errorMessage)).withHeaders(
            ContentType.toString    -> ApplicationJson.toString(),
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
            val errorMessage = errors
              .map { case (path, validationErrors) =>
                s"Invalid value at path $path: ${validationErrors.map(_.message).mkString(", ")}"
              }
              .mkString(", ")
            Future.successful(
              BadRequest(buildBadRequestResponse(request, errorMessage)).withHeaders(
                ContentType.toString    -> ApplicationJson.toString(),
                Date.toString           -> getCurrentHttpDate,
                XCorrelationID.toString -> request.headers.get(XCorrelationID.toString).getOrElse("")
              )
            )
        }
    }
  }

  private def buildBadRequestResponse(request: Request[AnyContent], details: String) =
    Json.toJson(
      EisReportStatusResponseError(
        errorDetail = EisReportStatusResponseErrorDetail(
          correlationId = request.headers.get(XCorrelationID.toString).getOrElse(""),
          errorCode = Some("400"),
          errorMessage = Some("Invalid request"),
          source = Some("TRE"),
          sourceFaultDetail = Some(
            EisReportStatusResponseErrorDetailSourceFaultDetail(
              detail = List(details),
              restFault = None,
              soapFault = None
            )
          ),
          timestamp = getCurrentHttpDate
        )
      )
    )
}
