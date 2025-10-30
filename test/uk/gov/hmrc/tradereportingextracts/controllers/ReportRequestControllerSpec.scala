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

package uk.gov.hmrc.tradereportingextracts.controllers

import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.*
import org.scalatest.matchers.must.Matchers.{must, mustBe}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.*
import play.api.test.*
import play.api.test.Helpers.*
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.tradereportingextracts.config.AppConfig
import uk.gov.hmrc.tradereportingextracts.connectors.CustomsDataStoreConnector
import uk.gov.hmrc.tradereportingextracts.models.audit.ReportRequestSubmittedEvent
import uk.gov.hmrc.tradereportingextracts.models.{EoriHistory, EoriHistoryResponse, NotificationEmail, ReportRequest}
import uk.gov.hmrc.tradereportingextracts.services.{EisService, ReportRequestService, RequestReferenceService}
import uk.gov.hmrc.tradereportingextracts.utils.{SpecBase, WireMockHelper}

import java.time.LocalDateTime
import scala.concurrent.Future

class ReportRequestControllerSpec extends SpecBase with WireMockHelper {
  val mockCustomsDataStoreConnector: CustomsDataStoreConnector = mock[CustomsDataStoreConnector]
  val mockReportRequestService: ReportRequestService           = mock[ReportRequestService]
  val mockRequestReferenceService: RequestReferenceService     = mock[RequestReferenceService]
  val mockEisService: EisService                               = mock[EisService]
  val mockAuditConnector: AuditConnector                       = mock[AuditConnector]
  val mockAppConfig: AppConfig                                 = mock[AppConfig]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockReportRequestService)
    reset(mockCustomsDataStoreConnector)
    reset(mockRequestReferenceService)
    reset(mockEisService)
    reset(mockAppConfig)
  }

  private val app = new GuiceApplicationBuilder()
    .overrides(
      bind[CustomsDataStoreConnector].toInstance(mockCustomsDataStoreConnector),
      bind[ReportRequestService].toInstance(mockReportRequestService),
      bind[RequestReferenceService].toInstance(mockRequestReferenceService),
      bind[EisService].toInstance(mockEisService),
      bind[AppConfig].toInstance(mockAppConfig)
    )
    .build()

  "createReportRequest" should {

    "return OK and report ID for a single report when input is valid" in {
      val inputJson: JsValue = Json.parse(
        """
          {
            "eori": "GB123456789014",
            "reportStartDate": "2025-04-16",
            "reportEndDate": "2025-05-16",
            "whichEori": "GB123456789014",
            "reportName": "MyReport",
            "eoriRole": ["declarant"],
            "reportType": ["importHeader"],
            "dataType": "import",
            "additionalEmail": ["email1@gmail.com"]
          }
        """
      )
      when(mockCustomsDataStoreConnector.getNotificationEmail(any()))
        .thenReturn(Future.successful(NotificationEmail("email@example.com", LocalDateTime.now())))

      when(mockCustomsDataStoreConnector.getEoriHistory(any()))
        .thenReturn(
          Future.successful(
            EoriHistoryResponse(
              Seq(
                EoriHistory(
                  "eori",
                  Some("2023-02-01"),
                  Some("2023-03-01")
                )
              )
            )
          )
        )

      when(mockRequestReferenceService.generateUnique())
        .thenReturn(Future.successful("REF-00000001"))

      when(mockReportRequestService.createAll(any())(any()))
        .thenReturn(Future.successful(true))

      val reportRequestCaptor = ArgumentCaptor.forClass(classOf[ReportRequest])
      when(mockEisService.requestTraderReport(any(), reportRequestCaptor.capture())(any()))
        .thenAnswer(_ => Future.successful(reportRequestCaptor.getValue))

      doNothing()
        .when(mockAuditConnector)
        .sendExplicitAudit(any[String], any[ReportRequestSubmittedEvent])(any(), any(), any())

      val request = FakeRequest(POST, "/trade-reporting-extracts/create-report-request")
        .withHeaders("Content-Type" -> "application/json")
        .withJsonBody(inputJson)

      val result = route(app, request).value

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.arr(
        Json.obj(
          "reportName"      -> "MyReport",
          "reportType"      -> "importHeader",
          "reportReference" -> "REF-00000001"
        )
      )

      verify(mockReportRequestService).createAll(any())(any())

      val capturedRequest = reportRequestCaptor.getValue
      capturedRequest.reportRequestId mustBe "REF-00000001"
      capturedRequest.requesterEORI mustBe "GB123456789014"
    }

    "return OK and 2 report IDs when 2 report types are requested" in {
      val inputJson: JsValue = Json.parse(
        """
          {
            "eori": "GB123456789014",
            "reportStartDate": "2025-04-16",
            "reportEndDate": "2025-05-16",
            "whichEori": "GB123456789014",
            "reportName": "MyReport",
            "eoriRole": ["declarant"],
            "reportType": ["importHeader", "importItem"],
            "dataType": "import",
            "additionalEmail": ["email1@gmail.com"]
          }
    """
      )
      when(mockCustomsDataStoreConnector.getNotificationEmail(any()))
        .thenReturn(Future.successful(NotificationEmail("email@example.com", LocalDateTime.now())))

      when(mockCustomsDataStoreConnector.getEoriHistory(any()))
        .thenReturn(
          Future.successful(
            EoriHistoryResponse(
              Seq(
                EoriHistory(
                  "eori",
                  Some("2023-02-01"),
                  Some("2023-03-01")
                )
              )
            )
          )
        )

      when(mockRequestReferenceService.generateUnique())
        .thenReturn(Future.successful("REF-00000001"))
        .thenReturn(Future.successful("REF-00000002"))

      when(mockReportRequestService.createAll(any())(any()))
        .thenReturn(Future.successful(true))

      val reportRequestCaptor = ArgumentCaptor.forClass(classOf[ReportRequest])
      when(mockEisService.requestTraderReport(any(), reportRequestCaptor.capture())(any()))
        .thenAnswer(_ => Future.successful(reportRequestCaptor.getValue))

      doNothing()
        .when(mockAuditConnector)
        .sendExplicitAudit(any[String], any[ReportRequestSubmittedEvent])(any(), any(), any())

      val request = FakeRequest(POST, "/trade-reporting-extracts/create-report-request")
        .withHeaders("Content-Type" -> "application/json")
        .withJsonBody(inputJson)

      val result = route(app, request).value

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.arr(
        Json.obj(
          "reportName"      -> "MyReport",
          "reportType"      -> "importHeader",
          "reportReference" -> "REF-00000001"
        ),
        Json.obj(
          "reportName"      -> "MyReport",
          "reportType"      -> "importItem",
          "reportReference" -> "REF-00000002"
        )
      )

      val captor = ArgumentCaptor.forClass(classOf[Seq[ReportRequest]])
      verify(mockReportRequestService, times(1)).createAll(captor.capture())(any())

    }

    "return OK and 3 report IDs when 3 report types are requested" in {
      val inputJson: JsValue = Json.parse(
        """
      {
        "eori": "GB123456789014",
        "reportStartDate": "2025-04-16",
        "reportEndDate": "2025-05-16",
        "whichEori": "GB123456789014",
        "reportName": "MyReport",
        "eoriRole": ["declarant"],
        "reportType": ["importHeader", "importItem", "importTaxLine"],
        "dataType": "import",
        "additionalEmail": ["email1@gmail.com"]
      }
    """
      )
      when(mockCustomsDataStoreConnector.getNotificationEmail(any()))
        .thenReturn(Future.successful(NotificationEmail("email@example.com", LocalDateTime.now())))

      when(mockCustomsDataStoreConnector.getEoriHistory(any()))
        .thenReturn(
          Future.successful(
            EoriHistoryResponse(
              Seq(
                EoriHistory(
                  "eori",
                  Some("2023-02-01"),
                  Some("2023-03-01")
                )
              )
            )
          )
        )

      when(mockRequestReferenceService.generateUnique())
        .thenReturn(Future.successful("REF-00000001"))
        .thenReturn(Future.successful("REF-00000002"))
        .thenReturn(Future.successful("REF-00000003"))

      when(mockReportRequestService.createAll(any())(any()))
        .thenReturn(Future.successful(true))

      val reportRequestCaptor = ArgumentCaptor.forClass(classOf[ReportRequest])
      when(mockEisService.requestTraderReport(any(), reportRequestCaptor.capture())(any()))
        .thenAnswer(_ => Future.successful(reportRequestCaptor.getValue))

      doNothing()
        .when(mockAuditConnector)
        .sendExplicitAudit(any[String], any[ReportRequestSubmittedEvent])(any(), any(), any())

      val request = FakeRequest(POST, "/trade-reporting-extracts/create-report-request")
        .withHeaders("Content-Type" -> "application/json")
        .withJsonBody(inputJson)

      val result = route(app, request).value

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.arr(
        Json.obj(
          "reportName"      -> "MyReport",
          "reportType"      -> "importHeader",
          "reportReference" -> "REF-00000001"
        ),
        Json.obj(
          "reportName"      -> "MyReport",
          "reportType"      -> "importItem",
          "reportReference" -> "REF-00000002"
        ),
        Json.obj(
          "reportName"      -> "MyReport",
          "reportType"      -> "importTaxLine",
          "reportReference" -> "REF-00000003"
        )
      )

      val captor = ArgumentCaptor.forClass(classOf[Seq[ReportRequest]])
      verify(mockReportRequestService, times(1)).createAll(captor.capture())(any())

    }

    "return BadRequest if JSON is invalid" in {
      val invalidJson = Json.obj("foo" -> "bar")

      val request = FakeRequest(POST, "/trade-reporting-extracts/create-report-request")
        .withHeaders("Content-Type" -> "application/json")
        .withJsonBody(invalidJson)

      val result = route(app, request).value

      status(result) mustBe BAD_REQUEST
    }

    "fail when Customs Data Store is down" in {
      val inputJson: JsValue = Json.parse(
        """
          {
            "eori": "GB123456789014",
            "reportStartDate": "2025-04-16",
            "reportEndDate": "2025-05-16",
            "whichEori": "GB123456789014",
            "reportName": "MyReport",
            "eoriRole": ["declarant"],
            "reportType": ["importHeader"],
            "dataType": "import",
            "additionalEmail": ["email1@gmail.com"]
          }
        """
      )

      when(mockCustomsDataStoreConnector.getNotificationEmail(any()))
        .thenReturn(Future.failed(new RuntimeException("CDS Unavailable")))

      val request = FakeRequest(POST, "/trade-reporting-extracts/create-report-request")
        .withHeaders("Content-Type" -> "application/json")
        .withJsonBody(inputJson)

      val thrown = route(app, request).value.failed.futureValue
      thrown mustBe a[RuntimeException]
      thrown.getMessage must include("CDS Unavailable")
    }
  }

  "hasReachedSubmissionLimit" should {

    "return TooManyRequests when submission limit is reached" in {
      val eori = "GB123456789014"

      when(mockReportRequestService.countReportSubmissionsForEoriOnDate(eqTo(eori), any(), any())(any()))
        .thenReturn(Future.successful(true))

      val request = FakeRequest(GET, s"/trade-reporting-extracts/report-submission-limit/$eori").withHeaders(
        "Content-Type" -> "application/json"
      )

      val result = route(app, request).value

      status(result) mustBe TOO_MANY_REQUESTS
    }

    "return NoContent when submission limit is not reached" in {
      val eori = "GB123456789014"

      when(mockReportRequestService.countReportSubmissionsForEoriOnDate(eqTo(eori), any(), any())(any()))
        .thenReturn(Future.successful(false))

      val request = FakeRequest(GET, s"/trade-reporting-extracts/report-submission-limit/$eori")

      val result = route(app, request).value

      status(result) mustBe NO_CONTENT
    }
  }

  "getReportRequestLimitNumber" should {
    "return report request limit number" in {
      when(mockAppConfig.dailySubmissionLimit).thenReturn(25)

      val request = FakeRequest(GET, "/trade-reporting-extracts/report-request-limit-number")

      val result = route(app, request).value

      contentAsJson(result) mustBe Json.toJson("25")
    }
  }
}
