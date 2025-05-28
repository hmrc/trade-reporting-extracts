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
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, ControllerComponents, Request}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.tradereportingextracts.connectors.CustomsDataStoreConnector

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton()
class VerifiedEmailController @Inject() (
  customsDataStoreConnector: CustomsDataStoreConnector,
  cc: ControllerComponents
)(using ec: ExecutionContext)
    extends BackendController(cc)
    with Logging:

  def getVerifiedEmail(): Action[JsValue] = Action.async(parse.json) { implicit request: Request[JsValue] =>
    val jsonBody  = request.body
    val eoriValue = (jsonBody \ "eori").asOpt[String].getOrElse("defaultValue")

    customsDataStoreConnector.getVerifiedEmail(eoriValue).flatMap {
      case Right(email) =>
        Future.successful(Ok(Json.toJson(email)))
      case Left(error)  =>
        Future.failed(new RuntimeException(error))
    }
  }
