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
import uk.gov.hmrc.internalauth.client.*
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.tradereportingextracts.services.CompanyInformationService
import uk.gov.hmrc.tradereportingextracts.utils.PermissionsUtil.readPermission

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CompanyInformationController @Inject() (
  cc: ControllerComponents,
  auth: BackendAuthComponents,
  companyInformationService: CompanyInformationService
)(using ec: ExecutionContext)
    extends BackendController(cc) {

  def getCompanyInformation: Action[JsValue] =
    auth.authorizedAction(readPermission).async(parse.json) { implicit request =>
      (request.body \ "eori").asOpt[String] match {
        case Some(eori) =>
          companyInformationService.getVisibleCompanyInformation(eori).map { info =>
            Ok(Json.toJson(info))
          }

        case None =>
          Future.successful(BadRequest(Json.obj("error" -> "Missing or invalid EORI in request body")))
      }
    }
}
