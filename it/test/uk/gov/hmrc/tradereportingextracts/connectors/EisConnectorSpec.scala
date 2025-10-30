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

import com.github.tomakehurst.wiremock.client.WireMock.*
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, NO_CONTENT, OK}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.tradereportingextracts.connectors.EisConnector
import uk.gov.hmrc.tradereportingextracts.utils.WireMockHelper
import uk.gov.hmrc.tradereportingextracts.models.eis.EisReportRequest

import java.time.format.DateTimeFormatter
import java.time.{ZoneOffset, ZonedDateTime}
import java.util.UUID

class EisConnectorSpec
  extends AnyFreeSpec
    with MockitoSugar
    with ScalaFutures
    with Matchers
    with WireMockHelper
    with BeforeAndAfterEach
    with BeforeAndAfterAll
    with IntegrationPatience {

  
  implicit private lazy val hc: HeaderCarrier = HeaderCarrier()

  private lazy val app: Application = new GuiceApplicationBuilder()
    .configure(
      "microservice.services.eis.port" -> server.port
    )
    .build()

  private lazy val connector: EisConnector = app.injector.instanceOf[EisConnector]

  private val requestUrl = "/trade-reporting-extracts-stub/gbe/requesttraderreport/v1"

  private def sampleRequest: EisReportRequest = EisReportRequest(
    endDate = "2024-06-01",
    eori = List("GB123456789000"),
    eoriRole = EisReportRequest.EoriRole.TRADER,
    reportTypeName = EisReportRequest.ReportTypeName.IMPORTSITEMREPORT,
    requestID = UUID.randomUUID().toString,
    requestTimestamp = DateTimeFormatter.ISO_INSTANT.format(ZonedDateTime.now(ZoneOffset.UTC)),
    requesterEori = "GB123456789000",
    startDate = "2024-05-01"
  )

  "EisConnector" - {
    "should return HttpResponse with OK when EIS responds with 200" in {
      val correlationId = UUID.randomUUID().toString
      val responseBody = Json.obj("result" -> "success").toString()

      server.stubFor(
        put(urlEqualTo(requestUrl))
          .willReturn(aResponse().withStatus(OK).withBody(responseBody))
      )

      val result = connector.requestTraderReport(sampleRequest, correlationId).futureValue
      result.status shouldBe OK
      result.body should include ("success")
    }

    "should return HttpResponse with NO_CONTENT when EIS responds with 204" in {
      val correlationId = UUID.randomUUID().toString

      server.stubFor(
        put(urlEqualTo(requestUrl))
          .willReturn(aResponse().withStatus(NO_CONTENT))
      )

      val result = connector.requestTraderReport(sampleRequest, correlationId).futureValue
      result.status shouldBe NO_CONTENT
    }

    "should return INTERNAL_SERVER_ERROR when EIS responds with error" in {
      val correlationId = UUID.randomUUID().toString

      server.stubFor(
        put(urlEqualTo(requestUrl))
          .willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR))
      )

      val result = connector.requestTraderReport(sampleRequest, correlationId).futureValue
      result.status shouldBe INTERNAL_SERVER_ERROR
    }
  }
}