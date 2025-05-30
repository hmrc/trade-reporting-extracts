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

import org.scalatestplus.play.*
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.Mockito.*
import org.mockito.ArgumentMatchers.*
import play.api.test.Helpers.*
import play.api.test.{FakeRequest, Helpers}
import play.api.libs.json.Json
import uk.gov.hmrc.tradereportingextracts.models.AvailableReportResponse
import uk.gov.hmrc.tradereportingextracts.services.AvailableReportService
import uk.gov.hmrc.tradereportingextracts.utils.ApplicationConstants.eori

import scala.concurrent.{ExecutionContext, Future}

class AvailableReportControllerSpec extends PlaySpec with MockitoSugar {

  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  val mockService                   = mock[AvailableReportService]
  val controller                    = new AvailableReportController(Helpers.stubControllerComponents(), mockService)(using ec)

  "getAvailableReports" should {
    "return Ok with reports when EORI is present" in {
      when(mockService.getAvailableReports(any[String])(any()))
        .thenReturn(
          Future.successful(
            AvailableReportResponse(
              availableUserReports = None,
              availableThirdPartyReports = None
            )
          )
        )
      val request = FakeRequest(GET, s"/api/available-reports")
        .withHeaders(CONTENT_TYPE -> JSON)
        .withJsonBody(Json.obj(eori -> "GB123456789000"))

      // val request = FakeRequest("GET", "/api/available-reports").withJsonBody(Json.obj(eori -> "GB123456789000"))
      val result = controller.getAvailableReports
        .apply(request)

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(
        AvailableReportResponse(
          availableUserReports = None,
          availableThirdPartyReports = None
        )
      )
    }

    "return BadRequest when EORI is missing" in {
      val request = FakeRequest().withJsonBody(Json.obj())
      val result  = controller.getAvailableReports()(request)

      status(result) mustBe BAD_REQUEST
    }
  }

  "getAvailableReportsCount" should {
    "return Ok with count when EORI is present" in {
      when(mockService.getAvailableReportsCount(any[String]))
        .thenReturn(Future.successful(5L))

      val request = FakeRequest().withJsonBody(Json.obj("eori" -> "GB123456789000"))
      val result  = controller.getAvailableReportsCount()(request)

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(5L)
    }

    "return BadRequest when EORI is missing" in {
      val request = FakeRequest().withJsonBody(Json.obj())
      val result  = controller.getAvailableReportsCount()(request)

      status(result) mustBe BAD_REQUEST
    }
  }
}
