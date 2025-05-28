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

import org.mockito.Mockito.when
import org.scalatest.matchers.must.Matchers.mustBe
import play.api.http.Status.*
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Request, Result}
import play.api.test.Helpers.{POST, contentAsJson, status}
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.tradereportingextracts.models.EoriRole.TRADER
import uk.gov.hmrc.tradereportingextracts.models.ReportRequest
import uk.gov.hmrc.tradereportingextracts.models.ReportTypeName.EXPORTS_ITEM_REPORT
import uk.gov.hmrc.tradereportingextracts.services.ReportRequestService
import uk.gov.hmrc.tradereportingextracts.utils.SpecBase

import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RequestedReportsControllerSpec extends SpecBase:

  private val mockReportRequestService: ReportRequestService = mock[ReportRequestService]
  private val controller                                     = new RequestedReportsController(Helpers.stubControllerComponents(), mockReportRequestService)

  private val sampleReport: ReportRequest = ReportRequest(
    reportRequestId = "REQ123",
    correlationId = "CORR001",
    reportName = "Monthly Report",
    requesterEORI = "GB123456789000",
    eoriRole = TRADER,
    reportEORIs = Seq("GB123456789000"),
    recipientEmails = Seq("user@example.com"),
    reportTypeName = EXPORTS_ITEM_REPORT,
    reportStart = Instant.parse("2024-06-01T00:00:00Z"),
    reportEnd = Instant.parse("2024-06-30T23:59:59Z"),
    createDate = Instant.parse("2024-07-01T10:00:00Z"),
    notifications = Seq.empty,
    fileNotifications = None,
    linkAvailableTime = Some(Instant.parse("2024-07-01T10:15:00Z"))
  )

  "POST /requested-reports" should {

    "return 200 OK with reports when EORI is provided" in {
      when(mockReportRequestService.getByRequesterEORI("GB123456789000"))
        .thenReturn(Future.successful(Seq(sampleReport)))

      val request: Request[JsValue] = FakeRequest(POST, "/requested-reports")
        .withBody(Json.obj("eori" -> "GB123456789000"))

      val result: Future[Result] = controller.getRequestedReports()(request)

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(Seq(sampleReport))
    }

    "return 204 NoContent when no reports are found for the given EORI" in {
      when(mockReportRequestService.getByRequesterEORI("GB000000000000"))
        .thenReturn(Future.successful(Seq.empty))

      val request: Request[JsValue] = FakeRequest(POST, "/requested-reports")
        .withBody(Json.obj("eori" -> "GB000000000000"))

      val result: Future[Result] = controller.getRequestedReports()(request)

      status(result) mustBe NO_CONTENT
    }

    "return 400 BadRequest when EORI is missing" in {
      val request: Request[JsValue] = FakeRequest(POST, "/requested-reports")
        .withBody(Json.obj())

      val result: Future[Result] = controller.getRequestedReports()(request)

      status(result) mustBe BAD_REQUEST
    }

  }
