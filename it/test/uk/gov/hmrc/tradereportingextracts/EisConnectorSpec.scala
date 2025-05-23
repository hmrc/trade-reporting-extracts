package uk.gov.hmrc.tradereportingextracts

import com.github.tomakehurst.wiremock.client.WireMock.*
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import play.api.Application
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, NO_CONTENT, OK}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import uk.gov.hmrc.tradereportingextracts.models.eis.EisReportRequest
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.tradereportingextracts.connectors.EisConnector
import uk.gov.hmrc.tradereportingextracts.it.utils.WireMockHelper

import java.time.{ZoneOffset, ZonedDateTime}
import java.time.format.DateTimeFormatter
import java.util.UUID

class EisConnectorSpec
  extends AnyFreeSpec
    with MockitoSugar
    with ScalaFutures
    with Matchers
    with WireMockHelper
    with BeforeAndAfterEach
    with BeforeAndAfterAll {

  override def beforeAll(): Unit = {
    super.beforeAll()
    startWireMock()
  }

  override def afterAll(): Unit = {
    stopWireMock()
    super.afterAll()
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    resetWireMock()
  }

  implicit private lazy val hc: HeaderCarrier = HeaderCarrier()

  private lazy val app: Application = new GuiceApplicationBuilder()
    .configure(
      "microservice.services.eis.port" -> wireMockServer.port
    )
    .build()

  private lazy val connector: EisConnector = app.injector.instanceOf[EisConnector]

  private val requestUrl = "/gbe/requesttraderreport/v1"

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

      wireMockServer.stubFor(
        put(urlEqualTo(requestUrl))
          .willReturn(aResponse().withStatus(OK).withBody(responseBody))
      )

      val result = connector.requestTraderReport(sampleRequest, correlationId).futureValue
      result.status shouldBe OK
      result.body should include ("success")
    }

    "should return HttpResponse with NO_CONTENT when EIS responds with 204" in {
      val correlationId = UUID.randomUUID().toString

      wireMockServer.stubFor(
        put(urlEqualTo(requestUrl))
          .willReturn(aResponse().withStatus(NO_CONTENT))
      )

      val result = connector.requestTraderReport(sampleRequest, correlationId).futureValue
      result.status shouldBe NO_CONTENT
    }

    "should return INTERNAL_SERVER_ERROR when EIS responds with error" in {
      val correlationId = UUID.randomUUID().toString

      wireMockServer.stubFor(
        put(urlEqualTo(requestUrl))
          .willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR))
      )

      val result = connector.requestTraderReport(sampleRequest, correlationId).futureValue
      result.status shouldBe INTERNAL_SERVER_ERROR
    }
  }
}