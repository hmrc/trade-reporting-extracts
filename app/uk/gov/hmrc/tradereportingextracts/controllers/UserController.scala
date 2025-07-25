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
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.tradereportingextracts.services.UserService

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton()
class UserController @Inject() (
  userService: UserService,
  cc: ControllerComponents
)(using executionContext: ExecutionContext)
    extends BackendController(cc):

  def setupUser(): Action[JsValue] = Action.async(parse.json) { implicit request =>
    (request.body \ "eori").validate[String] match {
      case JsSuccess(eori, _) =>
        userService.getOrCreateUser(eori).map { userDetails =>
          Created(Json.toJson(userDetails))
        }
      case JsError(_)         =>
        Future.successful(BadRequest("Missing or invalid 'eori' field"))
    }
  }

  def getAuthorisedEoris(eori: String): Action[AnyContent] = Action.async {
    userService
      .getAuthorisedEoris(eori)
      .map { authorisedEoris =>
        Ok(Json.toJson(authorisedEoris))
      }
      .recover { case e: Exception =>
        InternalServerError(e.getMessage)
      }
  }

  def getNotificationEmail: Action[JsValue] = Action.async(parse.json) { implicit request =>
    (request.body \ "eori").validate[String] match {
      case JsSuccess(eori, _) =>
        userService
          .getNotificationEmail(eori)
          .map(email => Ok(Json.toJson(email)))
          .recover { case e: Exception =>
            InternalServerError(e.getMessage)
          }

      case JsError(_) =>
        Future.successful(BadRequest("Missing or invalid 'eori' field"))
    }
  }
