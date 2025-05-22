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
import org.mockito.ArgumentMatchers.any
import play.api.test.Helpers.*
import play.api.test.*
import play.api.libs.json.*
import play.api.inject.bind
import org.mockito.Mockito.*
import org.scalatest.matchers.must.Matchers.{must, mustBe}
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.tradereportingextracts.connectors.CustomsDataStoreConnector
import uk.gov.hmrc.tradereportingextracts.models.{EoriHistory, EoriHistoryResponse, NotificationEmail, ReportRequest}
import uk.gov.hmrc.tradereportingextracts.services.{ReportRequestService, RequestReferenceService}
import uk.gov.hmrc.tradereportingextracts.utils.SpecBase

import scala.concurrent.{ExecutionContext, Future}
import java.time.{LocalDate, LocalDateTime}

class ReportRequestControllerSpec extends SpecBase {

  val ec: ExecutionContext                                     = ExecutionContext.global
  val mockCustomsDataStoreConnector: CustomsDataStoreConnector = mock[CustomsDataStoreConnector]
  val mockReportRequestService: ReportRequestService           = mock[ReportRequestService]
  val mockRequestReferenceService: RequestReferenceService     = mock[RequestReferenceService]

  private val app = new GuiceApplicationBuilder()
    .overrides(
      bind[CustomsDataStoreConnector].toInstance(mockCustomsDataStoreConnector),
      bind[ReportRequestService].toInstance(mockReportRequestService),
      bind[RequestReferenceService].toInstance(mockRequestReferenceService)
    )
    .build()

  "createReportRequest" should {
    "return OK and report ID when input is valid" in {
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
      when(mockCustomsDataStoreConnector.getVerifiedEmailForReport(any()))
        .thenReturn(Future.successful(NotificationEmail("email@example.com", LocalDateTime.now())))

      when(mockCustomsDataStoreConnector.getEoriHistory(any()))
        .thenReturn(
          Future.successful(
            EoriHistoryResponse(
              Seq(EoriHistory("eori", Some(LocalDate.of(2023, 2, 1)), Some(LocalDate.of(2023, 3, 1))))
            )
          )
        )

      when(mockRequestReferenceService.random()).thenReturn("RE-00000001")

      when(mockReportRequestService.create(any())(any()))
        .thenReturn(Future.successful(true))

      val request = FakeRequest(POST, "/trade-reporting-extracts/create-report-request")
        .withHeaders("Content-Type" -> "application/json")
        .withJsonBody(inputJson)

      val result = route(app, request).value

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.obj("references" -> Seq("RE-00000001"))

      val captor = ArgumentCaptor.forClass(classOf[ReportRequest])
      verify(mockReportRequestService).create(captor.capture())(any())

      val persistedRequest: ReportRequest = captor.getValue

      persistedRequest.requesterEORI mustBe "GB123456789014"
      persistedRequest.reportName mustBe "MyReport"
    }

    "return OK and report ID when input is sdfsdfsdf" in {
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

      when(mockCustomsDataStoreConnector.getVerifiedEmailForReport(any()))
        .thenReturn(Future.successful(NotificationEmail("email@example.com", LocalDateTime.now())))

      when(mockCustomsDataStoreConnector.getEoriHistory(any()))
        .thenReturn(Future.failed(Throwable("error")))

      when(mockRequestReferenceService.random()).thenReturn("RE-00000001")

      when(mockReportRequestService.create(any())(any()))
        .thenReturn(Future.successful(true))

      val request = FakeRequest(POST, "/trade-reporting-extracts/create-report-request")
        .withHeaders("Content-Type" -> "application/json")
        .withJsonBody(inputJson)

      val result = route(app, request).value.failed.futureValue
      result mustBe a[Throwable]
    }

    "return BadRequest if JSON is invalid" in {
      val invalidJson = Json.obj("foo" -> "bar")

      val request = FakeRequest(POST, "/trade-reporting-extracts/create-report-request")
        .withHeaders("Content-Type" -> "application/json")
        .withJsonBody(invalidJson)

      val result = route(app, request).value

      status(result) mustBe BAD_REQUEST
    }
  }
}
