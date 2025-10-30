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

package uk.gov.hmrc.tradereportingextracts.services

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.pattern.after
import play.api.Logging
import play.api.http.Status.*
import play.api.libs.json.{JsError, JsSuccess, Json}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tradereportingextracts.config.AppConfig
import uk.gov.hmrc.tradereportingextracts.connectors.EisConnector
import uk.gov.hmrc.tradereportingextracts.models.ReportRequest
import uk.gov.hmrc.tradereportingextracts.models.StatusCode.*
import uk.gov.hmrc.tradereportingextracts.models.eis.{EisReportRequest, EisReportResponseError, EisReportStatusRequest}

import java.time.{Clock, LocalDate}
import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.*
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EisService @Inject() (
  connector: EisConnector,
  reportRequestService: ReportRequestService,
  actorSystem: ActorSystem,
  appConfig: AppConfig
)(implicit
  ec: ExecutionContext
) extends Logging {

  private val MaxRetries = appConfig.eisRequestTraderReportMaxRetries
  private val RetryDelay = appConfig.eisRequestTraderReportRetryDelay

  def requestTraderReport(payload: EisReportRequest, reportRequest: ReportRequest)(implicit
    hc: HeaderCarrier
  ): Future[ReportRequest] = {

    def attempt(remainingAttempts: Int): Future[ReportRequest] = {
      val clock = Clock.systemUTC()
      connector
        .requestTraderReport(payload, reportRequest.correlationId)
        .flatMap { response =>
          response.status match {
            case OK | ACCEPTED | NO_CONTENT =>
              val updatedRequest = reportRequest.copy(notifications =
                reportRequest.notifications :+
                  EisReportStatusRequest(
                    applicationComponent = EisReportStatusRequest.ApplicationComponent.TRE,
                    statusCode = INITIATED.toString,
                    statusMessage = "Report sent to EIS successfully",
                    statusTimestamp = LocalDate.now(clock).toString,
                    statusType = EisReportStatusRequest.StatusType.INFORMATION
                  )
              )
              reportRequestService.update(updatedRequest).map(_ => updatedRequest)

            case INTERNAL_SERVER_ERROR | SERVICE_UNAVAILABLE | BAD_GATEWAY | GATEWAY_TIMEOUT if remainingAttempts > 1 =>
              logger.warn(
                s"EIS request failed with status ${response.status}. Retrying... Attempts left: ${remainingAttempts - 1}"
              )
              after(RetryDelay.second, actorSystem.scheduler)(attempt(remainingAttempts - 1))
            case status                                                                                               =>
              val errorMessage = Json.toJson(response.body).validate[EisReportResponseError] match {
                case JsError(errors)        =>
                  logger.error(s"Unexpected response from EIS: ${response.body}")
                  s"Unexpected response from EIS: ${response.body}"
                case JsSuccess(value, path) =>
                  logger.error(s"Failed to send report to EIS. Status: $status, Body: ${response.body}")
                  s"EIS Error: ${value.errorDetail.errorMessage}"
              }

              val updatedRequest = reportRequest.copy(notifications =
                reportRequest.notifications :+
                  EisReportStatusRequest(
                    applicationComponent = EisReportStatusRequest.ApplicationComponent.TRE,
                    statusCode = FAILED.toString,
                    statusMessage = errorMessage,
                    statusTimestamp = LocalDate.now(clock).toString,
                    statusType = EisReportStatusRequest.StatusType.ERROR
                  )
              )
              reportRequestService.update(updatedRequest).map(_ => updatedRequest)
          }
        }
    }

    attempt(MaxRetries)
  }
}
