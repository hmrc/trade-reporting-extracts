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

import org.apache.pekko.Done
import play.api.http.Status.{ACCEPTED, INTERNAL_SERVER_ERROR, NO_CONTENT, OK}
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.tradereportingextracts.config.AppConfig
import uk.gov.hmrc.tradereportingextracts.connectors.EisConnector
import uk.gov.hmrc.tradereportingextracts.models.ReportRequest
import uk.gov.hmrc.tradereportingextracts.models.eis.{EisReportRequest, EisReportStatusRequest}
import uk.gov.hmrc.tradereportingextracts.models.StatusCode.*
import java.time.{Clock, LocalDate}
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EisService @Inject() (connector: EisConnector, reportRequestService: ReportRequestService, appConfig: AppConfig)(
  implicit ec: ExecutionContext
) {

  private val MaxRetries = appConfig.eisRequestTraderReportMaxRetries

  def requestTraderReport(payload: EisReportRequest, reportRequest: ReportRequest)(implicit
    hc: HeaderCarrier
  ): Future[Done] = {

    def attempt(remainingAttempts: Int): Future[Done] =
      val clock = Clock.systemUTC()
      connector
        .requestTraderReport(payload, reportRequest.correlationId)
        .flatMap { response =>
          response.status match {
            case OK | ACCEPTED | NO_CONTENT                     =>
              val updatedRequest: ReportRequest =
                reportRequest
                  .copy(notifications =
                    Seq(
                      EisReportStatusRequest(
                        applicationComponent = EisReportStatusRequest.ApplicationComponent.TRE,
                        statusCode = INITIATED.toString,
                        statusMessage = "Report sent to EIS successfully",
                        statusTimestamp = LocalDate.now(clock).toString,
                        statusType = EisReportStatusRequest.StatusType.INFORMATION
                      )
                    )
                  )
              reportRequestService.update(updatedRequest).flatMap { _ =>
                Future.successful(Done)
              }
            case INTERNAL_SERVER_ERROR if remainingAttempts > 1 =>
              attempt(remainingAttempts - 1)
            case status                                         =>
              val updatedRequest: ReportRequest =
                reportRequest
                  .copy(notifications =
                    Seq(
                      EisReportStatusRequest(
                        applicationComponent = EisReportStatusRequest.ApplicationComponent.TRE,
                        statusCode = FAILED.toString,
                        statusMessage = "Report failed to send to EIS",
                        statusTimestamp = LocalDate.now(clock).toString,
                        statusType = EisReportStatusRequest.StatusType.ERROR
                      )
                    )
                  )
              reportRequestService.update(updatedRequest).flatMap { _ =>
                Future.failed(UpstreamErrorResponse(response.body, status))
              }
          }
        }
    attempt(MaxRetries)
  }
}
