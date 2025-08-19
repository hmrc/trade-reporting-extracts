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
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}
import uk.gov.hmrc.tradereportingextracts.config.AppConfig
import uk.gov.hmrc.tradereportingextracts.connectors.ConnectorFailureLogger.*
import uk.gov.hmrc.tradereportingextracts.models.sdes.FileAvailableResponse
import uk.gov.hmrc.tradereportingextracts.utils.ApplicationConstants.{xClientId, xSDESKey}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SDESConnector @Inject() (appConfig: AppConfig, httpClient: HttpClientV2)(using ec: ExecutionContext)
    extends Logging {

  def fetchAvailableReportFileUrl(eori: String)(implicit
    hc: HeaderCarrier
  ): Future[Seq[FileAvailableResponse]] = {
    val requestAvailableReportFileUrl = url"${appConfig.sdes}"
    logger.warn(s"Fetching available reports from : $requestAvailableReportFileUrl")
    httpClient
      .get(requestAvailableReportFileUrl)
      .setHeader(
        xClientId -> appConfig.treXClientId,
        xSDESKey  -> eori
      )
      .execute[Seq[FileAvailableResponse]]
      .logFailureReason(connectorName = "SDESConnector on fetchAvailableReportFileUrl")
  }
}
