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
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.*
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers.mustBe
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status.{ACCEPTED, INTERNAL_SERVER_ERROR, NO_CONTENT, OK}
import uk.gov.hmrc.crypto.Sensitive.SensitiveString
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.tradereportingextracts.config.AppConfig
import uk.gov.hmrc.tradereportingextracts.connectors.EisConnector
import uk.gov.hmrc.tradereportingextracts.models.StatusCode.*
import uk.gov.hmrc.tradereportingextracts.models.eis.EisReportStatusRequest.{ApplicationComponent, StatusType}
import uk.gov.hmrc.tradereportingextracts.models.eis.{EisReportRequest, EisReportStatusRequest}
import uk.gov.hmrc.tradereportingextracts.models.{EoriRole, ReportRequest, ReportTypeName}

import java.time.Instant
import scala.concurrent.duration.*
import scala.concurrent.{ExecutionContext, Future}

class EisServiceSpec extends AnyWordSpec with Matchers with MockitoSugar with ScalaFutures {

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = 5.seconds,
    interval = 100.milliseconds
  )
  implicit val ec: ExecutionContext                    = ExecutionContext.global
  implicit val hc: HeaderCarrier                       = HeaderCarrier()

  val mockConnector: EisConnector                    = mock[EisConnector]
  val mockReportRequestService: ReportRequestService = mock[ReportRequestService]
  val mockAppConfig: AppConfig                       = mock[AppConfig]
  val mockActorSystem: ActorSystem                   = ActorSystem("test-system")

  when(mockAppConfig.eisRequestTraderReportRetryDelay).thenReturn(1)
  when(mockAppConfig.eisRequestTraderReportMaxRetries).thenReturn(3)

  val service = new EisService(mockConnector, mockReportRequestService, mockActorSystem, mockAppConfig)

  val eisReportRequest: EisReportRequest = EisReportRequest(
    endDate = "2024-01-01",
    eori = List("GB123456789000"),
    eoriRole = EisReportRequest.EoriRole.TRADER,
    reportTypeName = EisReportRequest.ReportTypeName.IMPORTSITEMREPORT,
    requestID = "req-1",
    requestTimestamp = "2024-01-01T00:00:00Z",
    requesterEori = "GB123456789000",
    startDate = "2023-01-01"
  )

  val reportRequest: ReportRequest = ReportRequest(
    reportRequestId = "id",
    correlationId = "corr-1",
    reportName = "name",
    requesterEORI = "GB123456789000",
    eoriRole = EoriRole.DECLARANT,
    userEmail = Some(SensitiveString("test@example.com")),
    reportEORIs = Seq("GB123456789000"),
    recipientEmails = Seq(SensitiveString("test@example.com")),
    reportTypeName = ReportTypeName.IMPORTS_HEADER_REPORT,
    reportStart = Instant.now,
    reportEnd = Instant.now,
    createDate = Instant.now,
    notifications = Seq.empty,
    fileNotifications = null,
    updateDate = null
  )

  def httpResponse(status: Int, body: String = ""): HttpResponse =
    HttpResponse(status, body = body)

  "EisService.requestTraderReport" should {

    "return updated ReportRequest for OK, ACCEPTED, or NO_CONTENT" in {
      Seq(OK, ACCEPTED, NO_CONTENT).foreach { responseStatus =>
        reset(mockConnector, mockReportRequestService)

        when(mockConnector.requestTraderReport(any(), any())(any()))
          .thenReturn(Future.successful(httpResponse(responseStatus)))

        when(mockReportRequestService.update(any())(any()))
          .thenReturn(Future.successful(true))

        val result = service.requestTraderReport(eisReportRequest, reportRequest)

        whenReady(result) { updatedReportRequest =>
          val captor = ArgumentCaptor.forClass(classOf[ReportRequest])
          verify(mockReportRequestService).update(captor.capture())(any())

          val persistedReportRequestAfterEis: ReportRequest = captor.getValue
          persistedReportRequestAfterEis.notifications.head.copy(statusTimestamp = null) mustBe
            EisReportStatusRequest(
              applicationComponent = ApplicationComponent.TRE,
              statusCode = INITIATED.toString,
              statusMessage = "Report sent to EIS successfully",
              statusTimestamp = null,
              statusType = StatusType.INFORMATION
            )

          verify(mockConnector, times(1))
            .requestTraderReport(eqTo(eisReportRequest), eqTo(reportRequest.correlationId))(any())

          updatedReportRequest mustBe persistedReportRequestAfterEis
        }
      }
    }

    "retry on INTERNAL_SERVER_ERROR and succeed if a later attempt is OK" in {
      reset(mockConnector, mockReportRequestService)

      when(mockConnector.requestTraderReport(any(), any())(any()))
        .thenReturn(
          Future.successful(httpResponse(INTERNAL_SERVER_ERROR)),
          Future.successful(httpResponse(INTERNAL_SERVER_ERROR)),
          Future.successful(httpResponse(OK))
        )

      when(mockReportRequestService.update(any())(any()))
        .thenReturn(Future.successful(true))

      val result = service.requestTraderReport(eisReportRequest, reportRequest)

      whenReady(result) { updatedReportRequest =>
        val captor = ArgumentCaptor.forClass(classOf[ReportRequest])
        verify(mockReportRequestService).update(captor.capture())(any())

        val persistedReportRequestAfterEis: ReportRequest = captor.getValue
        persistedReportRequestAfterEis.notifications.head.copy(statusTimestamp = null) mustBe
          EisReportStatusRequest(
            applicationComponent = ApplicationComponent.TRE,
            statusCode = INITIATED.toString,
            statusMessage = "Report sent to EIS successfully",
            statusTimestamp = null,
            statusType = StatusType.INFORMATION
          )

        verify(mockConnector, times(3))
          .requestTraderReport(eqTo(eisReportRequest), eqTo(reportRequest.correlationId))(any())

        updatedReportRequest mustBe persistedReportRequestAfterEis
      }
    }

    "return updated ReportRequest with FAILED status if all retries exhausted with INTERNAL_SERVER_ERROR" in {
      reset(mockConnector, mockReportRequestService)

      when(mockConnector.requestTraderReport(any(), any())(any()))
        .thenReturn(
          Future.successful(httpResponse(INTERNAL_SERVER_ERROR)),
          Future.successful(httpResponse(INTERNAL_SERVER_ERROR)),
          Future.successful(httpResponse(INTERNAL_SERVER_ERROR))
        )

      when(mockReportRequestService.update(any())(any()))
        .thenReturn(Future.successful(true))

      val result = service.requestTraderReport(eisReportRequest, reportRequest)

      whenReady(result) { updatedReportRequest =>
        val captor = ArgumentCaptor.forClass(classOf[ReportRequest])
        verify(mockReportRequestService).update(captor.capture())(any())

        val persistedReportRequestAfterEis: ReportRequest = captor.getValue
        val notification                                  = persistedReportRequestAfterEis.notifications.head

        notification.copy(statusTimestamp = null) mustBe
          EisReportStatusRequest(
            applicationComponent = ApplicationComponent.TRE,
            statusCode = FAILED.toString,
            statusMessage = "Unexpected response from EIS: ",
            statusTimestamp = null,
            statusType = StatusType.ERROR
          )

        verify(mockConnector, times(3)).requestTraderReport(eqTo(eisReportRequest), eqTo(reportRequest.correlationId))(
          any()
        )
      }
    }
  }
}
