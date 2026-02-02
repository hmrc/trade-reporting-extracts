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
import play.api.Logging
import uk.gov.hmrc.tradereportingextracts.config.AppConfig
import uk.gov.hmrc.tradereportingextracts.models.etmp.*
import uk.gov.hmrc.tradereportingextracts.models.etmp.EoriUpdateHeaders.*
import uk.gov.hmrc.tradereportingextracts.services.{AdditionalEmailService, UserService}
import uk.gov.hmrc.tradereportingextracts.utils.HttpDateFormatter.getCurrentHttpDate
import uk.gov.hmrc.tradereportingextracts.utils.HeaderUtils

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class EoriUpdateController @Inject() (
  cc: ControllerComponents,
  userService: UserService,
  additionalEmailService: AdditionalEmailService,
  appConfig: AppConfig
) extends AbstractController(cc)
    with Logging {

  private def respond(status: Status)(implicit request: RequestHeader): Future[Result] =
    Future.successful(
      status.withHeaders(
        date.toString           -> getCurrentHttpDate,
        xCorrelationID.toString -> request.headers.get(xCorrelationID.toString).getOrElse("")
      )
    )

  def eoriUpdate(): Action[AnyContent] = Action.async { request =>
    def missingHeaders: Seq[String] =
      HeaderUtils.missingHeaders(request, EoriUpdateHeaders.allHeaders)

    def isAuthorized: Boolean =
      HeaderUtils.isAuthorized(request, appConfig.eoriUpdateAuthToken, authorization.toString)

    (missingHeaders, isAuthorized, request.body.asJson) match {
      case (headers, _, _) if headers.nonEmpty =>
        logger.error(
          s"eoriUpdate missing required headers: ${headers
              .mkString(", ")} for CorrelationID: ${request.headers.get(xCorrelationID.toString).getOrElse("")}"
        )
        respond(BadRequest)(request)

      case (_, false, _) =>
        logger.error(
          s"eoriUpdate unauthorised request for CorrelationID: ${request.headers.get(xCorrelationID.toString).getOrElse("")}"
        )
        respond(Forbidden)(request)

      case (_, _, None) =>
        logger.error(
          s"eoriUpdate missing request body for CorrelationID: ${request.headers.get(xCorrelationID.toString).getOrElse("")}"
        )
        respond(BadRequest)(request)

      case (_, _, Some(json)) =>
        json.validate[EoriUpdate] match {
          case JsError(_) =>
            logger.error(
              s"eoriUpdate invalid request body for CorrelationID: ${request.headers.get(xCorrelationID.toString).getOrElse("")}"
            )
            respond(BadRequest)(request)

          case JsSuccess(_, _) =>
            userService.updateEori(json.as[EoriUpdate])
            additionalEmailService.updateEori(json.as[EoriUpdate])
            respond(Created)(request)
        }
    }
  }
}
