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

import com.github.tomakehurst.wiremock.client.WireMock
import uk.gov.hmrc.tradereportingextracts.utils.WireMockHelper
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.Application
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps, UpstreamErrorResponse}
import uk.gov.hmrc.tradereportingextracts.config.AppConfig
import org.scalatestplus.play.*
import play.api.test.Helpers.*
import com.github.tomakehurst.wiremock.client.WireMock.*
import org.apache.pekko.Done
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

class EmailConnectorSpec extends AnyFreeSpec with ScalaFutures
  with GuiceOneAppPerSuite
  with MockitoSugar
  with Matchers
  with WireMockHelper
  with IntegrationPatience {

  implicit private lazy val hc: HeaderCarrier = HeaderCarrier()

  private def application: Application =
    new GuiceApplicationBuilder()
      .configure(
        "microservice.services.email.port" -> server.port,
        "microservice.services.email.host" -> "localhost"
      )
      .build()

  "sendEmailRequest" - {

    val payload =
      s"""{
         |  "to": ["email@email.com"],
         |  "templateId": "tre",
         |  "parameters": {}
         |}""".stripMargin

    "Must return Done when email service returns ACCEPTED" in {
      val app = application
      running(app) {
        val connector = app.injector.instanceOf[EmailConnector]
        val appConfig = app.injector.instanceOf[AppConfig]
        val emailUrl = url"${appConfig.email}"
        server.stubFor(
          WireMock.post(urlEqualTo(emailUrl.getPath))
            .withRequestBody(equalToJson(payload))
            .willReturn(aResponse().withStatus(202))
        )
        val result = connector.sendEmailRequest("tre", "email@email.com", Map.empty).futureValue
        result shouldBe Done
      }
    }

    "Must return upstream error response when email service returns server error" in {
      val app = application
      running(app) {
        val connector = app.injector.instanceOf[EmailConnector]
        val appConfig = app.injector.instanceOf[AppConfig]
        val emailUrl = url"${appConfig.email}"
        server.stubFor(
          WireMock.post(urlEqualTo(emailUrl.getPath))
            .withRequestBody(equalToJson(payload))
            .willReturn(aResponse().withStatus(500))
        )
        val result = connector.sendEmailRequest("tre", "email@email.com", Map.empty).failed.futureValue
        result shouldBe a[UpstreamErrorResponse]
      }
    }
  }
}