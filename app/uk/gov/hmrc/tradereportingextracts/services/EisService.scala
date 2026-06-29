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
import com.typesafe.config.Config
import play.api.Logging
import play.api.http.Status
import play.api.libs.json.{JsSuccess, Json}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tradereportingextracts.connectors.EisConnector
import uk.gov.hmrc.tradereportingextracts.models.ReportRequest
import uk.gov.hmrc.tradereportingextracts.models.StatusCode.*
import uk.gov.hmrc.tradereportingextracts.models.eis.{EisReportRequest, EisReportResponseError, EisReportStatusRequest}
import uk.gov.hmrc.http.Retries
import scala.util.{Failure, Success}
import java.time.{Clock, LocalDate}
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

@Singleton
class EisService @Inject() (
  connector: EisConnector,
  reportRequestService: ReportRequestService,
  override val configuration: Config,
  override val actorSystem: ActorSystem
)(implicit
  ec: ExecutionContext
) extends Logging
    with Retries {

  def requestTraderReport(payload: EisReportRequest, reportRequest: ReportRequest)(implicit
    hc: HeaderCarrier
  ): Future[ReportRequest] = {
    val clock = Clock.systemUTC()

    val retryCondition: PartialFunction[Exception, Boolean] = { case EisService.EisServerError(_, _) =>
      true
    }

    retryFor("EIS requestTraderReport")(retryCondition) {
      connector
        .requestTraderReport(payload, reportRequest.correlationId)
        .flatMap { response =>
          val status = response.status
          if (Status.isSuccessful(status)) {
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
          } else if (Status.isServerError(status)) {
            Future.failed(EisService.EisServerError(status, response.body))
          } else {
            val errorMessage   = Try(Json.parse(response.body).validate[EisReportResponseError]) match {
              case Success(JsSuccess(value, _)) =>
                logger.error(s"Failed to send report to EIS. Status: $status, Body: ${response.body}")
                s"EIS Error: ${value.errorDetail.errorMessage.getOrElse("Unknown EIS error")}"
              case _                            =>
                logger.error(s"Unexpected response from EIS: ${response.body}")
                s"Unexpected response from EIS: ${response.body}"
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
    }.transformWith {
      case Success(value)                                   => Future.successful(value)
      case Failure(EisService.EisServerError(status, body)) =>
        val errorMessage   = s"EIS server error after retries. Status: $status, Body: $body"
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
      case Failure(ex)                                      => Future.failed(ex)
    }
  }

  object EisService {
    case class EisServerError(status: Int, body: String) extends Exception(s"Server error: $status, $body")
  }
}
