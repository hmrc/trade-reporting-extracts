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
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.tradereportingextracts.config.AppConfig
import uk.gov.hmrc.tradereportingextracts.models.{CompanyInformation, EoriHistory, EoriHistoryResponse, NotificationEmail}
import uk.gov.hmrc.tradereportingextracts.utils.{SpecBase, WireMockHelper}

import java.time.{Instant, LocalDate, LocalDateTime}

class CustomsDataStoreConnectorSpec
    extends SpecBase
    with ScalaFutures
    with GuiceOneAppPerSuite
    with MockitoSugar
    with Matchers
    with WireMockHelper
    with IntegrationPatience {

  val appConfig: AppConfig = app.injector.instanceOf[AppConfig]
  val baseUrlCDS: String   = appConfig.customsDataStore
  val uri                  = new java.net.URI(baseUrlCDS)
  val path                 = uri.getPath

  private def application1: Application =
    new GuiceApplicationBuilder()
      .configure("microservice.services.customs-data-store.port" -> server.port)
      .build()

  "CustomsDataStoreConnector" should {

    val url  = path ++ "/eori/verified-email"
    val eori = "GB123456789012"

    "should return NotificationEmail when response is OK" in {
      val responseBody =
        s"""{
             |  "address": "test@example.com",
             |  "timestamp": "2025-05-19T16:11:16.825994979"
             |}""".stripMargin

      val app = application1
      running(app) {
        val connector = app.injector.instanceOf[CustomsDataStoreConnector]
        server.stubFor(
          WireMock
            .get(WireMock.urlEqualTo(url))
            .willReturn(WireMock.ok(responseBody))
        )

        val result = connector.getVerifiedEmailForReport(eori).futureValue
        result mustBe NotificationEmail("test@example.com", LocalDateTime.parse("2025-05-19T16:11:16.825994979"))
      }
    }

    "should fail with UpstreamErrorResponse when response is not OK" in {
      val app = application1
      running(app) {
        val connector = app.injector.instanceOf[CustomsDataStoreConnector]
        server.stubFor(
          WireMock
            .get(WireMock.urlEqualTo(url))
            .willReturn(WireMock.aResponse().withStatus(500))
        )

        val result = connector.getVerifiedEmailForReport(eori).failed.futureValue
        result mustBe a[UpstreamErrorResponse]
      }
    }

    "getCompanyInformation" should {
      val url  = path ++ "/eori/company-information"
      val eori = "GB123456789012"

      "should return CompanyInformation when response is OK" in {
        val responseBody =
          s"""{
             |  "name": "Test Company"
             |}""".stripMargin

        val app = application1
        running(app) {
          val connector = app.injector.instanceOf[CustomsDataStoreConnector]
          server.stubFor(
            WireMock
              .get(WireMock.urlEqualTo(url))
              .willReturn(WireMock.ok(responseBody))
          )

          val result = connector.getCompanyInformation(eori).futureValue
          result mustBe CompanyInformation("Test Company")
        }
      }

      "should return empty CompanyInformation when response is not OK" in {
        val app = application1
        running(app) {
          val connector = app.injector.instanceOf[CustomsDataStoreConnector]
          server.stubFor(
            WireMock
              .get(WireMock.urlEqualTo(url))
              .willReturn(WireMock.aResponse().withStatus(404))
          )

          val result = connector.getCompanyInformation(eori).futureValue
          result mustBe CompanyInformation()
        }
      }
    }

    "getEoriHistory" should {
      val url  = path ++ "/eori/eori-history"
      val eori = "GB123456789012"

      "should return EoriHistoryResponse when response is OK" in {
        val responseBody =
          s"""{
             |"eoriHistory": [
             |  {
             |    "eori": "GB123456789012",
             |    "validFrom": "2001-01-20T00:00:00Z", 
             |    "validUntil": "2002-01-20T00:00:00Z"
             |  }
             |]
             |}""".stripMargin

        val app = application1
        running(app) {
          val connector = app.injector.instanceOf[CustomsDataStoreConnector]
          server.stubFor(
            WireMock
              .get(WireMock.urlEqualTo(url))
              .willReturn(WireMock.ok(responseBody))
          )

          val result = connector.getEoriHistory(eori).futureValue
          result mustBe EoriHistoryResponse(
            Seq(
              EoriHistory(
                "GB123456789012",
                Some(Instant.parse("2001-01-20T00:00:00Z")),
                Some(Instant.parse("2002-01-20T00:00:00Z"))
              )
            )
          )
        }
      }

      "should return empty EoriHistoryResponse when response is not OK" in {
        val app = application1
        running(app) {
          val connector = app.injector.instanceOf[CustomsDataStoreConnector]
          server.stubFor(
            WireMock
              .get(WireMock.urlEqualTo(url))
              .willReturn(WireMock.aResponse().withStatus(500))
          )

          val result = connector.getEoriHistory(eori).futureValue
        }
      }
    }

    "getNotificationEmail" should {
      val url  = path ++ "/eori/verified-email"
      val eori = "GB123456789012"

      "should return NotificationEmail when response is OK" in {
        val responseBody =
          s"""{
             |  "address": "notify@example.com",
             |  "timestamp": "2025-05-19T16:11:16.825994979"
             |}""".stripMargin

        val app = application1
        running(app) {
          val connector = app.injector.instanceOf[CustomsDataStoreConnector]
          server.stubFor(
            WireMock
              .get(WireMock.urlEqualTo(url))
              .willReturn(WireMock.ok(responseBody))
          )

          val result = connector.getNotificationEmail(eori).futureValue
          result mustBe NotificationEmail("notify@example.com", LocalDateTime.parse("2025-05-19T16:11:16.825994979"))
        }
      }
    }
  }
}
