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
import uk.gov.hmrc.internalauth.client.*
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.tradereportingextracts.services.ReportRequestService
import uk.gov.hmrc.tradereportingextracts.utils.PermissionsUtil.readPermission

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RequestedReportsController @Inject() (
  cc: ControllerComponents,
  auth: BackendAuthComponents,
  reportRequestService: ReportRequestService
)(using ec: ExecutionContext)
    extends BackendController(cc)
    with Logging:

  def getRequestedReports: Action[JsValue] =
    auth.authorizedAction(readPermission).async(parse.json) { implicit request: Request[JsValue] =>
      val eoriOpt = (request.body \ "eori").asOpt[String]

      eoriOpt match {
        case Some(eori) =>
          reportRequestService
            .getReportRequestsForUser(eori)
            .map { response =>
              if (response.userReports.isEmpty && response.thirdPartyReports.isEmpty) {
                logger.info(s"No reports found for EORI: $eori")
                NoContent
              } else {
                Ok(Json.toJson(response))
              }
            }
            .recover { case ex: Exception =>
              logger.error(s"Error fetching reports for EORI: $eori", ex)
              InternalServerError(Json.obj("error" -> "Internal server error"))
            }

        case None =>
          logger.warn("Missing 'eori' in request body")
          Future.successful(BadRequest(Json.obj("error" -> "Missing 'eori' in request body")))
      }
    }
