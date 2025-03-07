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
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.tradereportingextracts.connectors.CustomsDataStoreConnector

import javax.inject.Inject
import scala.concurrent.ExecutionContext
import uk.gov.hmrc.http.HeaderCarrier

import scala.util.control.NonFatal

class EoriHistoryController @Inject()(customsDataStoreConnector: CustomsDataStoreConnector,
                                      cc: ControllerComponents)(using executionContext: ExecutionContext)
  extends BackendController(cc) {

  private val log: Logger = Logger(this.getClass)

  def getEoriHistory(): Action[AnyContent] = Action.async { implicit request =>
    implicit val hc: HeaderCarrier = HeaderCarrier()
    customsDataStoreConnector.getEoriHistory()
      .map(response => Ok(Json.toJson(response)))
      .recover { case NonFatal(error) =>
        logErrorAndReturnServiceUnavailable(error)
      }
  }
  private def logErrorAndReturnServiceUnavailable(error: Throwable) = {
    log.error(s"getEoriHistory failed: ${error.getMessage}")
    ServiceUnavailable
  }
}
