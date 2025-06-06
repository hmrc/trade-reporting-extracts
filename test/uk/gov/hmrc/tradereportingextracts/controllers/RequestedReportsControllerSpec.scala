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
import play.api.test.Helpers.{GET, contentAsJson, status}
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.tradereportingextracts.models.ReportStatus.IN_PROGRESS
import uk.gov.hmrc.tradereportingextracts.models.ReportTypeName.EXPORTS_ITEM_REPORT
import uk.gov.hmrc.tradereportingextracts.models.{GetReportRequestsResponse, UserReport}
import uk.gov.hmrc.tradereportingextracts.services.ReportRequestService
import uk.gov.hmrc.tradereportingextracts.utils.SpecBase

import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RequestedReportsControllerSpec extends SpecBase:

  private val mockReportRequestService: ReportRequestService =
    mock[ReportRequestService]

  private val controller =
    new RequestedReportsController(Helpers.stubControllerComponents(), mockReportRequestService)

  private val expectedResponse = GetReportRequestsResponse(
    userReports = Some(
      Seq(
        UserReport(
          referenceNumber = "REQ123",
          reportName = "Monthly Report",
          requestedDate = Instant.parse("2024-07-01T10:00:00Z"),
          reportType = EXPORTS_ITEM_REPORT,
          reportStatus = IN_PROGRESS
        )
      )
    ),
    thirdPartyReports = None
  )

  "POST /requested-reports" should {

    "return 200 OK with reports when EORI is provided" in {
      val eori = "GB123456789000"

      when(mockReportRequestService.getReportRequestsForUser(eori))
        .thenReturn(Future.successful(expectedResponse))

      val request: Request[JsValue] = FakeRequest(GET, "/requested-reports")
        .withBody(Json.obj("eori" -> eori))

      val result: Future[Result] = controller.getRequestedReports()(request)

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(expectedResponse)
    }

    "return 204 NoContent when no reports are found for the given EORI" in {
      val eori = "GB000000000000"

      when(mockReportRequestService.getReportRequestsForUser(eori))
        .thenReturn(Future.successful(GetReportRequestsResponse(None, None)))

      val request: Request[JsValue] = FakeRequest(GET, "/requested-reports")
        .withBody(Json.obj("eori" -> eori))

      val result: Future[Result] = controller.getRequestedReports()(request)

      status(result) mustBe NO_CONTENT
    }

    "return 400 BadRequest when EORI is missing" in {
      val request: Request[JsValue] = FakeRequest(GET, "/requested-reports")
        .withBody(Json.obj())

      val result: Future[Result] = controller.getRequestedReports()(request)

      status(result) mustBe BAD_REQUEST
    }
  }
