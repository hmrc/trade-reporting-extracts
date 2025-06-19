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

package uk.gov.hmrc.tradereportingextracts.connectors

import org.apache.pekko.Done
import play.api.Logging
import play.api.libs.json._
import play.api.http.Status.{NOT_FOUND, OK}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps, UpstreamErrorResponse}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.tradereportingextracts.config.AppConfig
import play.api.libs.ws.JsonBodyWritables.*
import uk.gov.hmrc.tradereportingextracts.models.EmailRenderRequest
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.http.HttpReads.Implicits.*

class EmailRendererConnector @Inject() (
                               appConfig: AppConfig,
                               httpClient: HttpClientV2
                             )(implicit ec: ExecutionContext) extends Logging {

  def sendEmailRequest(templateId: String,
                          email: String,
                          params: Map[String, String]
                         )(implicit hc: HeaderCarrier): Future[Done] = {

    val emailUrl = url"${appConfig.emailRenderer}/templates/${templateId}"

    httpClient
      .post(emailUrl)
      .withBody(Json.toJson(EmailRenderRequest(params, Some(email))))
      .execute[HttpResponse]
      .flatMap { response =>
        response.status match {
          case OK => Future.successful(Done)
          case NOT_FOUND =>
            logger.warn(s"Template with ID $templateId not found in email renderer")
            Future.failed(
              UpstreamErrorResponse(
                s"Template with ID $templateId not found in email renderer",
                NOT_FOUND
              )
            )
          case _  =>
            logger.error(s"Unexpected response from call to email renderer")
            Future.failed(
              UpstreamErrorResponse(
                "Unexpected response from email renderer",
                response.status
              )
            )
        }
      }
  }
}
