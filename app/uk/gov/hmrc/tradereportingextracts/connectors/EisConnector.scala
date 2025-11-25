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

import play.api.Logging
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.*
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.tradereportingextracts.config.AppConfig
import uk.gov.hmrc.tradereportingextracts.connectors.ConnectorFailureLogger.FromResultToConnectorFailureLogger
import uk.gov.hmrc.tradereportingextracts.models.eis.EisReportRequest

import java.time.format.DateTimeFormatter
import java.time.{ZoneOffset, ZonedDateTime}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EisConnector @Inject() (
  appConfig: AppConfig,
  httpClient: HttpClientV2
)(implicit ec: ExecutionContext)
    extends Logging {
  def requestTraderReport(
    payload: EisReportRequest,
    correlationId: String
  )(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    val requestTraderReportUrl = url"${appConfig.eis}"
    val currentDate            = ZonedDateTime
      .now(ZoneOffset.UTC)
      .format(DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'"))
    val eisAuthToken           = "Bearer " + appConfig.eisAPI6AuthToken
    logger.info(s"Requesting trader report at : $requestTraderReportUrl")
    httpClient
      .put(requestTraderReportUrl)
      .withBody(Json.toJson(payload))
      .setHeader(
        "accept"           -> "application/json",
        "content-type"     -> "application/json",
        "authorization"    -> eisAuthToken,
        "date"             -> currentDate,
        "x-correlation-id" -> correlationId,
        "x-forwarded-host" -> "MDTP"
      )
      .execute[HttpResponse]
      .logFailureReason(connectorName = "TradeReportConnector on requestTraderReport")
  }

}
