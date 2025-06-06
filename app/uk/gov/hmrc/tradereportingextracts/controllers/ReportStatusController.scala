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
import uk.gov.hmrc.tradereportingextracts.config.AppConfig
import uk.gov.hmrc.tradereportingextracts.models.eis.EisReportStatusHeaders.*
import uk.gov.hmrc.tradereportingextracts.models.eis.{EisReportStatusHeaders, EisReportStatusRequest}
import uk.gov.hmrc.tradereportingextracts.services.ReportRequestService

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
      request.headers.get(Authorization.toString).contains(appConfig.eisAPI6AuthToken)

    (missingHeaders, isAuthorized, request.body.asJson) match {
      case (headers, _, _) if headers.nonEmpty =>
        Future.successful(BadRequest(s"Failed header validation: Missing headers: ${headers.mkString(", ")}"))
      case (_, false, _)                       =>
        Future.successful(Forbidden)
      case (_, _, None)                        =>
        Future.successful(BadRequest("Expected application/json request body"))
      case (_, _, Some(json))                  =>
        json.validate[EisReportStatusRequest] match {
          case JsSuccess(_, _) =>
            reportRequestService.processReportStatus(request.headers, json.as[EisReportStatusRequest])
            Future.successful(Created)
          case JsError(errors) =>
            val errorMessage = errors
              .map { case (path, validationErrors) =>
                s"Invalid value at path $path: ${validationErrors.map(_.message).mkString(", ")}"
              }
              .mkString(", ")
            Future.successful(BadRequest(errorMessage))
        }
    }
  }

  def serverOtherMethods(): Action[AnyContent] = Action.async { request =>
    Future.successful(
      MethodNotAllowed(
        s"Method ${request.method} not allowed. Only PUT is allowed for this endpoint."
      )
    )
  }
}
