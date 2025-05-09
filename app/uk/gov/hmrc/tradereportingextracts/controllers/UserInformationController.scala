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

import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.tradereportingextracts.services.UserInformationService

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class UserInformationController @Inject() (
  userService: UserInformationService,
  cc: ControllerComponents
)(using executionContext: ExecutionContext)
    extends BackendController(cc):

  def getUserInformation(): Action[JsValue] = Action.async(parse.json) { implicit request =>
    request.body
      .validate[JsValue]
      .fold(
        _ => Future.successful(BadRequest("Invalid JSON")),
        json => {
          val eori = (json \ "eori").as[String]
          userService.getUserByEori(eori).flatMap {
            case Left(error) =>
              Future.successful(Forbidden(error))
            case Right(user) =>
              Future.successful(Ok(Json.toJson(user)))
          }
        }
      )
  }
