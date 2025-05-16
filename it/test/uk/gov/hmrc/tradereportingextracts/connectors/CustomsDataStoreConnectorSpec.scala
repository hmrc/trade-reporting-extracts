package uk.gov.hmrc.tradereportingextracts.connectors

import com.github.tomakehurst.wiremock.client.WireMock
import uk.gov.hmrc.tradereportingextracts.utils.{SpecBase, WireMockHelper}
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, ok, post, urlEqualTo}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers.{a, mustBe}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.running
import play.api.{Application, inject}
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.tradereportingextracts.models.NotificationEmail

import java.time.LocalDateTime

class CustomsDataStoreConnectorSpec extends AnyFreeSpec with ScalaFutures
  with WireMockHelper {

  private def application: Application =
    new GuiceApplicationBuilder()
      .configure("microservice.services.customs-data-store.port" -> server.port)
      .build()

  "getVerifiedEmailForReport" - {

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
          WireMock.get(urlEqualTo("/trade-reporting-extracts-stub/eori/verified-email"))
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
          WireMock.get(urlEqualTo("/trade-reporting-extracts-stub/eori/verified-email"))
            .willReturn(aResponse.withStatus(500))
        )

        val result = connector.getVerifiedEmailForReport("eori").failed.futureValue

        result mustBe a[UpstreamErrorResponse]

      }

    }
  }

}
