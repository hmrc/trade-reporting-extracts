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
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.must.Matchers.mustBe
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.*
import uk.gov.hmrc.tradereportingextracts.config.AppConfig
import uk.gov.hmrc.tradereportingextracts.models.{CompanyInformation, EoriHistory, EoriHistoryResponse, NotificationEmail}
import uk.gov.hmrc.tradereportingextracts.utils.{SpecBase, WireMockHelper}

import java.time.LocalDateTime

class CustomsDataStoreConnectorSpec
    extends SpecBase
    with ScalaFutures
    with GuiceOneAppPerSuite
    with MockitoSugar
    with Matchers
    with WireMockHelper
    with IntegrationPatience {

  private def applicationWithPort(port: Int): Application =
    new GuiceApplicationBuilder()
      .configure("microservice.services.customs-data-store.port" -> port)
      .build()

  "CustomsDataStoreConnector" should {

    val eori = "GB123456789012"

    "getVerifiedEmailForReport" should {

      val responseBody =
        s"""{
           |  "address": "test@example.com",
           |  "timestamp": "2025-05-19T16:11:16.825994979"
           |}""".stripMargin

      "return NotificationEmail when response is OK" in {
        val app = applicationWithPort(server.port)
        running(app) {
          val connector = app.injector.instanceOf[CustomsDataStoreConnector]
          val appConfig = app.injector.instanceOf[AppConfig]

          server.stubFor(
            WireMock
              .post(WireMock.urlEqualTo(new java.net.URI(appConfig.verifiedEmailUrl).getPath))
              .willReturn(WireMock.ok(responseBody))
          )

          val result = connector.getNotificationEmail(eori).futureValue
          result mustBe NotificationEmail("test@example.com", LocalDateTime.parse("2025-05-19T16:11:16.825994979"))
        }
      }
    }

    "getCompanyInformation" should {

      val responseBody =
        s"""{
           |  "name": "Test Company"
           |}""".stripMargin

      "return CompanyInformation when response is OK" in {
        val app = applicationWithPort(server.port)
        running(app) {
          val connector = app.injector.instanceOf[CustomsDataStoreConnector]
          val appConfig = app.injector.instanceOf[AppConfig]

          server.stubFor(
            WireMock
              .post(WireMock.urlEqualTo(new java.net.URI(appConfig.companyInformationUrl).getPath))
              .willReturn(WireMock.ok(responseBody))
          )

          val result = connector.getCompanyInformation(eori).futureValue
          result mustBe CompanyInformation("Test Company")
        }
      }

      "return empty CompanyInformation when response is not OK" in {
        val app = applicationWithPort(server.port)
        running(app) {
          val connector = app.injector.instanceOf[CustomsDataStoreConnector]
          val appConfig = app.injector.instanceOf[AppConfig]

          server.stubFor(
            WireMock
              .post(WireMock.urlEqualTo(new java.net.URI(appConfig.companyInformationUrl).getPath))
              .willReturn(WireMock.aResponse().withStatus(404))
          )

          val result = connector.getCompanyInformation(eori).futureValue
          result mustBe CompanyInformation()
        }
      }
    }

    "getEoriHistory" should {

      val responseBody =
        s"""{
           |"eoriHistory": [
           |  {
           |    "eori": "GB123456789012",
           |    "validFrom": "2001-01-20",
           |    "validUntil": "2002-01-20"
           |  }
           |]
           |}""".stripMargin

      "return EoriHistoryResponse when response is OK" in {
        val app = applicationWithPort(server.port)
        running(app) {
          val connector = app.injector.instanceOf[CustomsDataStoreConnector]
          val appConfig = app.injector.instanceOf[AppConfig]

          server.stubFor(
            WireMock
              .post(WireMock.urlEqualTo(new java.net.URI(appConfig.eoriHistoryUrl).getPath))
              .willReturn(WireMock.ok(responseBody))
          )

          val result = connector.getEoriHistory(eori).futureValue
          result mustBe EoriHistoryResponse(
            Seq(
              EoriHistory(
                "GB123456789012",
                Some("2001-01-20"),
                Some("2002-01-20")
              )
            )
          )
        }
      }

      "return empty EoriHistoryResponse when response is not OK" in {
        val app = applicationWithPort(server.port)
        running(app) {
          val connector = app.injector.instanceOf[CustomsDataStoreConnector]
          val appConfig = app.injector.instanceOf[AppConfig]

          server.stubFor(
            WireMock
              .post(WireMock.urlEqualTo(new java.net.URI(appConfig.eoriHistoryUrl).getPath))
              .willReturn(WireMock.aResponse().withStatus(500))
          )

          val result = connector.getEoriHistory(eori).futureValue
          result mustBe EoriHistoryResponse(Seq.empty)
        }
      }
    }

    "getNotificationEmail" should {

      val responseBody =
        s"""{
           |  "address": "notify@example.com",
           |  "timestamp": "2025-05-19T16:11:16.825994979"
           |}""".stripMargin

      "return NotificationEmail when response is OK" in {
        val app = applicationWithPort(server.port)
        running(app) {
          val connector = app.injector.instanceOf[CustomsDataStoreConnector]
          val appConfig = app.injector.instanceOf[AppConfig]

          server.stubFor(
            WireMock
              .post(WireMock.urlEqualTo(new java.net.URI(appConfig.verifiedEmailUrl).getPath))
              .willReturn(WireMock.ok(responseBody))
          )

          val result = connector.getNotificationEmail(eori).futureValue
          result mustBe NotificationEmail("notify@example.com", LocalDateTime.parse("2025-05-19T16:11:16.825994979"))
        }
      }
    }
  }
}
