package uk.gov.hmrc.tradereportingextracts.connectors

import com.github.tomakehurst.wiremock.client.WireMock.*
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import play.api.Application
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tradereportingextracts.utils.WireMockHelper

import scala.List

class SDESConnectorSpec
    extends AnyFreeSpec
    with Matchers
    with ScalaFutures
    with WireMockHelper
    with BeforeAndAfterEach
    with BeforeAndAfterAll
    with IntegrationPatience {

  implicit private lazy val hc: HeaderCarrier = HeaderCarrier()

  private lazy val app: Application = new GuiceApplicationBuilder()
    .configure(
      "microservice.services.sdes.port"        -> server.port,
      "microservice.services.sdes.host"        -> "localhost",
      "microservice.services.sdes.x-client-id" -> "TRE-CLIENT-ID"
    )
    .build()

  private lazy val connector: SDESConnector = app.injector.instanceOf[SDESConnector]

  private val sdesUrl = "/trade-reporting-extracts-stub/files-available/list/TRE"

  "SDESConnector" - {
    "should return file responses when SDES responds with 200" in {
      val eori         = "GB123456789000"
      val responseBody = Json
        .arr(
          Json.obj(
            "filename"    -> "pvat-2018-106.csv",
            "downloadURL" -> "https://some.sdes.domain?token=abc456",
            "fileSize"    -> 1324,
            "metadata"    -> Json.arr(
              Json.obj("metadata" -> "FileCreationTimestamp", "value"    -> "2025-06-30T12"),
              Json.obj("metadata" -> "FileType", "value"                 -> "CSV"),
              Json.obj("metadata" -> "EORI", "value"                     -> eori),
              Json.obj("metadata" -> "MdtpReportXCorrelationId", "value" -> "2409398b-ee8f-47cd-b873-ac7ac099c28b"),
              Json.obj("metadata" -> "MdtpReportRequestId", "value"      -> "RE1212212001"),
              Json.obj("metadata" -> "MdtpReportTypeName", "value"       -> "Imports-Header"),
              Json.obj("metadata" -> "ReportFileCounter", "value"        -> "2of2"),
              Json.obj("metadata" -> "ReportLastFile", "value"           -> "true")
            )
          )
        )
        .toString()

      server.stubFor(
        get(urlEqualTo(sdesUrl))
          .withHeader("X-Client-Id", equalTo("TRE-CLIENT-ID"))
          .withHeader("X-SDES-Key", equalTo(eori))
          .willReturn(aResponse().withStatus(OK).withBody(responseBody))
      )

      val result = connector.fetchAvailableReportFileUrl(eori).futureValue
      result.map(_.filename) should contain theSameElementsAs List("pvat-2018-106.csv")
    }

    "should fail when SDES responds with error" in {
      val eori = "GB123456789000"
      server.stubFor(
        get(urlEqualTo(sdesUrl))
          .withHeader("X-Client-Id", equalTo("TRE-CLIENT-ID"))
          .withHeader("X-SDES-Key", equalTo(eori))
          .willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR))
      )

      whenReady(connector.fetchAvailableReportFileUrl(eori).failed) { ex =>
        ex.getMessage should include("500")
      }
    }
  }
}
