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
import play.api.{Application, inject}
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.tradereportingextracts.config.AppConfig
import org.scalatestplus.play.*
import play.api.test.*
import play.api.test.Helpers.*
import com.github.tomakehurst.wiremock.client.WireMock.*
import org.apache.pekko.Done
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

import java.net.URI

class EmailRendererConnectorSpec extends AnyFreeSpec with ScalaFutures
  with GuiceOneAppPerSuite
  with MockitoSugar
  with Matchers
  with WireMockHelper
  with IntegrationPatience {

  implicit private lazy val hc: HeaderCarrier = HeaderCarrier()

  val appConfig: AppConfig = app.injector.instanceOf[AppConfig]
  val baseUrlCDS: String = appConfig.emailRenderer
  val uri = new URI(baseUrlCDS)
  val path = uri.getPath

  private def application: Application =
    new GuiceApplicationBuilder()
      .configure("microservice.services.hmrc-email-renderer.port" -> server.port)
      .build()

  "sendEmailRequest" - {

    val payload =
      s"""{
         |  "parameters": {},
         |  "email": "email@email.com"
         |}""".stripMargin

    val url = path ++ "/templates/tre"

    "Must return Done when DC returns OK" in {

      val app = application
      running(app) {
        val connector = app.injector.instanceOf[EmailRendererConnector]
        server.stubFor(
          WireMock.post(urlEqualTo(url))
            .withRequestBody(equalToJson(payload))
            .willReturn(ok)
        )
        val result = connector.sendEmailRequest("tre", "email@email.com").futureValue
        result shouldBe Done
        }
      }

      "Must return upstream error response when DC returns Not Found" in {

        val app = application
        running(app) {
          val connector = app.injector.instanceOf[EmailRendererConnector]
          server.stubFor(
            WireMock.post(urlEqualTo(url))
              .withRequestBody(equalToJson(payload))
              .willReturn(notFound)
          )
          val result = connector.sendEmailRequest("tre", "email@email.com").failed.futureValue
          result shouldBe a[UpstreamErrorResponse]
        }
      }

      "Must return upstream error response when DC returns bad request" in {

        val app = application
        running(app) {
          val connector = app.injector.instanceOf[EmailRendererConnector]
          server.stubFor(
            WireMock.post(urlEqualTo(url))
              .willReturn(serverError)
          )
          val result = connector.sendEmailRequest("tre", "email@email.com").failed.futureValue
          result shouldBe a[UpstreamErrorResponse]
        }
      }
    }
  }
