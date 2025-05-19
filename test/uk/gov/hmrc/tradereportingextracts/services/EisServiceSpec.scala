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
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.*
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import play.api.http.Status.{ACCEPTED, BAD_REQUEST, INTERNAL_SERVER_ERROR, NO_CONTENT, OK}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, UpstreamErrorResponse}
import uk.gov.hmrc.tradereportingextracts.connectors.EisConnector
import uk.gov.hmrc.tradereportingextracts.models.eis.EisReportRequest
import uk.gov.hmrc.tradereportingextracts.models.{EoriRole, Notification, ReportRequest, ReportTypeName}
import uk.gov.hmrc.tradereportingextracts.services.{EisService, ReportRequestService}
import uk.gov.hmrc.tradereportingextracts.config.AppConfig

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

class EisServiceSpec extends AnyWordSpec with Matchers with MockitoSugar with ScalaFutures {

  implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val hc: HeaderCarrier    = HeaderCarrier()

  val mockConnector: EisConnector                    = mock[EisConnector]
  val mockReportRequestService: ReportRequestService = mock[ReportRequestService]
  val mockAppConfig: AppConfig                       = mock[AppConfig]

  when(mockAppConfig.eisRequestTraderReportMaxRetries).thenReturn(3)

  val service = new EisService(mockConnector, mockReportRequestService, mockAppConfig)

  val payload = EisReportRequest(
    endDate = "2024-01-01",
    eori = List("GB123456789000"),
    eoriRole = EisReportRequest.EoriRole.TRADER,
    reportTypeName = EisReportRequest.ReportTypeName.IMPORTSITEMREPORT,
    requestID = "req-1",
    requestTimestamp = "2024-01-01T00:00:00Z",
    requesterEori = "GB123456789000",
    startDate = "2023-01-01"
  )

  val reportRequest = ReportRequest(
    reportRequestId = "id",
    correlationId = "corr-1",
    reportName = "name",
    requesterEORI = "GB123456789000",
    eoriRole = EoriRole.DECLARANT,
    reportEORIs = Seq("GB123456789000"),
    recipientEmails = Seq("test@example.com"),
    reportTypeName = ReportTypeName.IMPORTS_HEADER_REPORT,
    reportStart = Instant.now,
    reportEnd = Instant.now,
    createDate = Instant.now,
    notifications = Seq.empty,
    fileAvailableTime = null,
    linkAvailableTime = null
  )

  def httpResponse(status: Int, body: String = ""): HttpResponse =
    HttpResponse(status, body = body)

  "EisService.requestTraderReport" should {

    "return Done for OK, ACCEPTED, or NO_CONTENT" in {
      Seq(OK, ACCEPTED, NO_CONTENT).foreach { status =>
        reset(mockConnector, mockReportRequestService)
        when(mockConnector.requestTraderReport(any(), any())(any()))
          .thenReturn(Future.successful(httpResponse(status)))
        val result = service.requestTraderReport(payload, reportRequest)
        whenReady(result)(_ shouldBe Done)
        verify(mockConnector, times(1)).requestTraderReport(eqTo(payload), eqTo(reportRequest.correlationId))(any())
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
      val result = service.requestTraderReport(payload, reportRequest)
      whenReady(result)(_ shouldBe Done)
      verify(mockConnector, times(3)).requestTraderReport(eqTo(payload), eqTo(reportRequest.correlationId))(any())
    }

    "fail with UpstreamErrorResponse if all retries exhausted with INTERNAL_SERVER_ERROR" in {
      reset(mockConnector, mockReportRequestService)
      when(mockConnector.requestTraderReport(any(), any())(any()))
        .thenReturn(
          Future.successful(httpResponse(INTERNAL_SERVER_ERROR)),
          Future.successful(httpResponse(INTERNAL_SERVER_ERROR)),
          Future.successful(httpResponse(INTERNAL_SERVER_ERROR))
        )
      when(mockReportRequestService.update(any())).thenReturn(Future.successful(true))
      val result = service.requestTraderReport(payload, reportRequest)
      whenReady(result.failed) { ex =>
        ex                                              shouldBe a[UpstreamErrorResponse]
        ex.asInstanceOf[UpstreamErrorResponse].reportAs shouldBe INTERNAL_SERVER_ERROR
      }
      verify(mockConnector, times(3)).requestTraderReport(eqTo(payload), eqTo(reportRequest.correlationId))(any())
      verify(mockReportRequestService, times(1)).update(any())
    }
  }
}
