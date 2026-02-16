/*
 * Copyright 2026 HM Revenue & Customs
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

import play.api.libs.json.JsValue
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.internalauth.client.*
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.tradereportingextracts.services.AdditionalEmailService
import uk.gov.hmrc.tradereportingextracts.utils.ApplicationConstants.eori
import uk.gov.hmrc.tradereportingextracts.utils.JsonValidationHelper.validateFields
import uk.gov.hmrc.tradereportingextracts.utils.PermissionsUtil.writePermission

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton()
class AdditionalEmailsController @Inject() (
  additionalEmailService: AdditionalEmailService,
  cc: ControllerComponents,
  auth: BackendAuthComponents
)(using executionContext: ExecutionContext)
    extends BackendController(cc):

  def addAdditionalEmail(): Action[JsValue] =
    auth.authorizedAction(writePermission).async(parse.json) { implicit request =>
      validateFields(
        "eori"         -> (request.body \ eori).validate[String],
        "emailAddress" -> (request.body \ "emailAddress").validate[String]
      ) match {

        case Right(values) =>
          val eori  = values("eori")
          val email = values("emailAddress")

          additionalEmailService.addAdditionalEmail(eori, email).map {
            case true  => Ok
            case false => InternalServerError("Failed to add additional email")
          }

        case Left(errorResult) =>
          Future.successful(errorResult)
      }
    }

  def removeAdditionalEmail(): Action[JsValue] =
    auth.authorizedAction(writePermission).async(parse.json) { implicit request =>
      validateFields(
        "eori"         -> (request.body \ eori).validate[String],
        "emailAddress" -> (request.body \ "emailAddress").validate[String]
      ) match {

        case Right(values) =>
          val eori  = values("eori")
          val email = values("emailAddress")

          additionalEmailService
            .removeAdditionalEmail(eori, email)
            .map {
              case true  => NoContent
              case false => NotFound("Additional email address not found")
            }
            .recover { case _ =>
              InternalServerError("Failed to remove additional email")
            }

        case Left(errorResult) =>
          Future.successful(errorResult)
      }
    }
