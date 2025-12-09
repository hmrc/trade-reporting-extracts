/*
 * Copyright 2024 HM Revenue & Customs
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

import play.api.Logging
import play.api.http.Status.{NOT_FOUND, OK}
import play.api.libs.json.Json
import play.api.libs.ws.writeableOf_JsValue
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps, UpstreamErrorResponse}
import uk.gov.hmrc.tradereportingextracts.config.AppConfig
import uk.gov.hmrc.tradereportingextracts.models.{CompanyInformation, EoriHistory, EoriHistoryResponse, NotificationEmail}
import uk.gov.hmrc.tradereportingextracts.connectors.ConnectorFailureLogger.*
import uk.gov.hmrc.tradereportingextracts.utils.ApplicationConstants

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CustomsDataStoreConnector @Inject() (appConfig: AppConfig, httpClient: HttpClientV2)(using ec: ExecutionContext)
    extends Logging {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  def getCompanyInformation(eori: String): Future[CompanyInformation] =
    logger.info(s"Requesting company information at : ${appConfig.companyInformationUrl}")
    httpClient
      .post(url"${appConfig.companyInformationUrl}")
      .withBody(Json.obj(ApplicationConstants.eori -> eori))
      .execute[HttpResponse]
      .flatMap { response =>
        response.status match {
          case OK => Future.successful(response.json.as[CompanyInformation])
          case _  =>
            logger.error(s"Unexpected response from : ${appConfig.companyInformationUrl}")
            Future.successful(CompanyInformation())
        }
      }

  def getEoriHistory(eori: String): Future[EoriHistoryResponse] =
    logger.info(s"Requesting EORI history at : ${appConfig.eoriHistoryUrl}")
    httpClient
      .post(url"${appConfig.eoriHistoryUrl}")
      .withBody(Json.obj(ApplicationConstants.eori -> eori))
      .execute[HttpResponse]
      .flatMap { response =>
        response.status match {
          case OK => Future.successful(response.json.as[EoriHistoryResponse])
          case _  =>
            logger.error(s"Unexpected response from : ${appConfig.eoriHistoryUrl}")
            Future.successful(EoriHistoryResponse(Seq.empty[EoriHistory]))
        }
      }

  def getNotificationEmail(eori: String): Future[NotificationEmail] =
    logger.info(s"Requesting notification email at : ${appConfig.verifiedEmailUrl}")
    httpClient
      .post(url"${appConfig.verifiedEmailUrl}")
      .withBody(Json.obj(ApplicationConstants.eori -> eori))
      .execute[HttpResponse]
      .flatMap { response =>
        response.status match {
          case OK        => Future.successful(response.json.as[NotificationEmail])
          case NOT_FOUND =>
            logger.info(s"Email not found")
            Future.successful(NotificationEmail())
          case _         =>
            Future.failed(
              UpstreamErrorResponse(
                s"Unexpected response from getNotifacationEmail : ${response.status}",
                response.status
              )
            )
        }
      }
      .logFailureReason(connectorName = "CustomDataStoreConnector on getNotificationEmail")
}
