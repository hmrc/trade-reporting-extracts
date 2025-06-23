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
import org.scalatest.matchers.must.Matchers.*
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.*
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.tradereportingextracts.config.AppConfig
import uk.gov.hmrc.tradereportingextracts.models.NotificationEmail
import uk.gov.hmrc.tradereportingextracts.utils.WireMockHelper

import java.net.URI
import java.time.LocalDateTime

class CustomsDataStoreConnectorSpec
    extends AnyFreeSpec
    with ScalaFutures
    with GuiceOneAppPerSuite
    with MockitoSugar
    with Matchers
    with WireMockHelper
    with IntegrationPatience {

  private def application: Application =
    new GuiceApplicationBuilder()
      .configure("microservice.services.customs-data-store.port" -> server.port)
      .build()

  "CustomsDataStoreConnector" - {

    "getVerifiedEmailForReport" - {

      "must return NotificationEmail when response is OK" in {
        val responseBody =
          s"""{
             |  "address": "example@test.com",
             |  "timestamp": "2025-05-19T16:11:16.825994979"
             |}""".stripMargin

        val app = application
        running(app) {
          val connector = app.injector.instanceOf[CustomsDataStoreConnector]
          val appConfig = app.injector.instanceOf[AppConfig]
          val path      = new URI(appConfig.verifiedEmailUrl).getPath

          server.stubFor(
            post(urlEqualTo(path))
              .willReturn(ok(responseBody))
          )

          val result = connector.getNotificationEmail("eori").futureValue

          result mustBe NotificationEmail("example@test.com", LocalDateTime.parse("2025-05-19T16:11:16.825994979"))
        }
      }
    }
  }
}
