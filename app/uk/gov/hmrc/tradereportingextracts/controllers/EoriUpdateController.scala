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
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.tradereportingextracts.config.AppConfig
import uk.gov.hmrc.tradereportingextracts.models.etmp.*
import uk.gov.hmrc.tradereportingextracts.models.etmp.EoriUpdateHeaders.*
import uk.gov.hmrc.tradereportingextracts.utils.HttpDateFormatter.getCurrentHttpDate

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EoriUpdateController @Inject() (
  cc: ControllerComponents,
  appConfig: AppConfig
)(using ec: ExecutionContext)
    extends AbstractController(cc) {

  def eoriUpdate(): Action[AnyContent] = Action.async { request =>
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequest(request)

    def missingHeaders: Seq[String] =
      EoriUpdateHeaders.allHeaders.filterNot(header => request.headers.get(header).isDefined)

    def isAuthorized: Boolean =
      request.headers.get(authorization.toString).getOrElse("").equals(appConfig.etmpAuthToken)

    (missingHeaders, isAuthorized, request.body.asJson) match {
      case (headers, _, _) if headers.nonEmpty =>
        Future.successful(
          BadRequest(buildBadRequestHeaderResponse(request, missingHeaders)).withHeaders(
            contentType.toString    -> ApplicationJson.toString(),
            date.toString           -> getCurrentHttpDate,
            xCorrelationID.toString -> request.headers.get(xCorrelationID.toString).getOrElse("")
          )
        )
      case (_, false, _)                       =>
        Future.successful(
          Forbidden.withHeaders(
            date.toString           -> getCurrentHttpDate,
            xCorrelationID.toString -> request.headers.get(xCorrelationID.toString).getOrElse("")
          )
        )
      case (_, _, None)                        =>
        Future.successful(
          BadRequest(buildBadRequestBodyResponse(request, List("Expected application/json request body")))
            .withHeaders(
              contentType.toString    -> ApplicationJson.toString(),
              date.toString           -> getCurrentHttpDate,
              xCorrelationID.toString -> request.headers.get(xCorrelationID.toString).getOrElse("")
            )
        )
      case (_, _, Some(json))                  =>
        json.validate[EoriUpdate] match {
          case JsError(errors) =>
            val errorMessage = errors
              .map { case (path, validationErrors) =>
                s"Invalid value at path $path: ${validationErrors.map(_.message).mkString(", ")}"
              }
              .mkString(", ")
            Future.successful(
              BadRequest(buildBadRequestBodyResponse(request, List(errorMessage))).withHeaders(
                contentType.toString    -> ApplicationJson.toString(),
                date.toString           -> getCurrentHttpDate,
                xCorrelationID.toString -> request.headers.get(xCorrelationID.toString).getOrElse("")
              )
            )
          case JsSuccess(_, _) =>
            Future.successful(
              Created.withHeaders(
                date.toString           -> getCurrentHttpDate,
                xCorrelationID.toString -> request.headers.get(xCorrelationID.toString).getOrElse("")
              )
            )
        }
    }
  }

  private def buildBadRequestHeaderResponse(request: Request[AnyContent], missingHeaders: Seq[String]) =
    Json.toJson(
      EoriUpdate400Error(
        errorDetail = EoriUpdate400ErrorDetail(
          correlationId = request.headers.get(xCorrelationID.toString).getOrElse(""),
          errorCode = Some("400"),
          errorMessage = Some("Failed header validation"),
          source = Some("TRE"),
          sourceFaultDetail = Some(
            EoriUpdate400ErrorDetailSourceFault(
              detail = missingHeaders.map(header => s"Failed header validation: Invalid $header header"),
              restFault = None,
              soapFault = None
            )
          ),
          timestamp = getCurrentHttpDate
        )
      )
    )

  private def buildBadRequestBodyResponse(request: Request[AnyContent], errors: Seq[String]) =
    Json.toJson(
      EoriUpdate400Error(
        errorDetail = EoriUpdate400ErrorDetail(
          correlationId = request.headers.get(xCorrelationID.toString).getOrElse(""),
          errorCode = Some("400"),
          errorMessage = Some("Failed body validation"),
          source = Some("EIS"),
          sourceFaultDetail = Some(
            EoriUpdate400ErrorDetailSourceFault(
              detail = errors.map(error => s"Failed body validation: Invalid $error"),
              restFault = None,
              soapFault = None
            )
          ),
          timestamp = getCurrentHttpDate
        )
      )
    )

  def serverOtherMethods(): Action[AnyContent] = Action.async { request =>
    Future.successful(
      MethodNotAllowed.withHeaders(
        date.toString           -> getCurrentHttpDate,
        xCorrelationID.toString -> request.headers.get(xCorrelationID.toString).getOrElse("")
      )
    )
  }

  def serverOthers(): Action[AnyContent] = Action.async { request =>
    Future.successful(
      NotFound.withHeaders(
        date.toString           -> getCurrentHttpDate,
        xCorrelationID.toString -> request.headers.get(xCorrelationID.toString).getOrElse("")
      )
    )
  }
}
