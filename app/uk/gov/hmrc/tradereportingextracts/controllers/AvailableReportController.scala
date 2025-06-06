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

import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.tradereportingextracts.services.AvailableReportService
import uk.gov.hmrc.tradereportingextracts.utils.ApplicationConstants.eori

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AvailableReportController @Inject() (cc: ControllerComponents, availableReportService: AvailableReportService)(
  using executionContext: ExecutionContext
) extends BackendController(cc) {
  private val log: Logger                     = Logger(this.getClass)
  def getAvailableReports: Action[AnyContent] = Action.async { implicit request =>
    implicit val hc: HeaderCarrier = HeaderCarrier()
    request.body.asJson.flatMap(json => (json \ eori).asOpt[String]) match {
      case Some(eoriValue) =>
        availableReportService.getAvailableReports(eoriValue).map { reports =>
          Ok(Json.toJson(reports))
        }
      case _               =>
        Future.successful(BadRequest("Missing or invalid EORI in request body"))
    }
  }

  def getAvailableReportsCount: Action[AnyContent] = Action.async { implicit request =>
    request.body.asJson.flatMap(json => (json \ eori).asOpt[String]) match {
      case Some(eoriValue) =>
        availableReportService.getAvailableReportsCount(eoriValue).map { count =>
          Ok(Json.toJson(count))
        }
      case _               =>
        Future.successful(BadRequest("Missing or invalid EORI in request body"))
    }
  }
}
