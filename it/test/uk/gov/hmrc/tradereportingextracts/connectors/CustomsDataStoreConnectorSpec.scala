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
import org.scalatest.matchers.must.Matchers.mustBe
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.{Application, inject}
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.tradereportingextracts.config.AppConfig
import uk.gov.hmrc.tradereportingextracts.models.NotificationEmail
import org.scalatestplus.play.*
import play.api.test.*
import play.api.test.Helpers.*
import com.github.tomakehurst.wiremock.client.WireMock.*
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

import java.net.{URI, URL}
import java.time.LocalDateTime

class CustomsDataStoreConnectorSpec extends AnyFreeSpec with ScalaFutures
  with GuiceOneAppPerSuite
  with MockitoSugar
  with Matchers
  with WireMockHelper
  with IntegrationPatience {

  val appConfig: AppConfig = app.injector.instanceOf[AppConfig]
  val baseUrlCDS: String = appConfig.customsDataStore
  val uri = new URI(baseUrlCDS)
  val path = uri.getPath

  private def application: Application =
    new GuiceApplicationBuilder()
      .configure("microservice.services.customs-data-store.port" -> server.port)
      .build()

  "getVerifiedEmailForReport" - {

    val url = path ++ "/eori/verified-email"

    "Must return verified email when OK" in {

      val responseBody =
        s"""{
           |  "address": "example@test.com",
           |  "timestamp": "2025-05-19T16:11:16.825994979"
           |}""".stripMargin

      val app = application
      running(app) {
        val connector = app.injector.instanceOf[CustomsDataStoreConnector]
        server.stubFor(
          WireMock.get(urlEqualTo(url))
            .willReturn(ok(responseBody))
        )

        val result = connector.getVerifiedEmailForReport("eori").futureValue

        result mustBe NotificationEmail("example@test.com", LocalDateTime.parse("2025-05-19T16:11:16.825994979"))

      }

    }

    "Must return upstream error response when not OK" in {
      val app = application
      running(app) {
        val connector = app.injector.instanceOf[CustomsDataStoreConnector]
        server.stubFor(
          WireMock.get(urlEqualTo(url))
            .willReturn(aResponse.withStatus(500))
        )

        val result = connector.getVerifiedEmailForReport("eori").failed.futureValue

        result mustBe a[UpstreamErrorResponse]

      }

    }
  }

}
