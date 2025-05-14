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

import play.api.http.Status
import uk.gov.hmrc.audit.handler.HttpResult.Response
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.tradereportingextracts.config.AppConfig
import uk.gov.hmrc.tradereportingextracts.models.eis.EisHttpReader.StatusHttpReader
import uk.gov.hmrc.tradereportingextracts.models.eis.{EisHttpErrorHandler, EisHttpErrorResponse}
import uk.gov.hmrc.tradereportingextracts.models.{CompanyInformation, ReportRequest}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EISConnector @Inject() (appConfig: AppConfig, httpClient: HttpClientV2)(using
  ec: ExecutionContext,
  hc: HeaderCarrier
) extends EisHttpErrorHandler:

  def submitReportRequest(reportRequest: ReportRequest): Future[Either[EisHttpErrorResponse, HttpResponse]] =
    httpClient
      .get(url"${appConfig.eisRequestURL}")
      .execute(StatusHttpReader(reportRequest.reportRequestId, handleErrorResponse), ec)
      .flatMap:
      response => Future.successful(response)
