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

import play.api.libs.json.Json
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.tradereportingextracts.config.AppConfig
import uk.gov.hmrc.tradereportingextracts.models.{CompanyInformation, EoriHistory, EoriHistoryResponse, NotificationEmail}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.ws.writeableOf_JsValue

class CustomsDataStoreConnector @Inject() (appConfig: AppConfig, httpClient: HttpClientV2)(using ec: ExecutionContext):
  implicit val hc: HeaderCarrier = HeaderCarrier()

  def getCompanyInformation()(using hc: HeaderCarrier): Future[CompanyInformation] =
    httpClient
      .get(url"${appConfig.customsDataStore}/eori/company-information")
      .execute[CompanyInformation]
      .flatMap:
      response => Future.successful(response)

  def getEoriHistory(eori: String): Future[EoriHistoryResponse] =
    httpClient
      .get(url"${appConfig.customsDataStore}/eori/eori-history")
      .withBody(Json.obj("eori" -> eori))
      .execute[EoriHistoryResponse]
      .flatMap:
      response => Future.successful(response)

  def getVerifiedEmail(eori: String)(using hc: HeaderCarrier): Future[Either[String, NotificationEmail]] =
    httpClient
      .get(url"${appConfig.customsDataStore}/eori/verified-email")
      .withBody(Json.obj("eori" -> eori))
      .execute[NotificationEmail]
      .map(response => Right(response))
      .recover { case ex: Exception =>
        Left("Failed to retrieve verified email")
      }
