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

import org.mockito.ArgumentMatchers.*
import org.mockito.Mockito.*
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.*
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.mvc.Results.BadRequest
import play.api.test.Helpers.*
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.internalauth.client.*
import uk.gov.hmrc.internalauth.client.Retrieval.EmptyRetrieval
import uk.gov.hmrc.internalauth.client.test.{BackendAuthComponentsStub, StubBehaviour}
import uk.gov.hmrc.tradereportingextracts.config.AppConfig
import uk.gov.hmrc.tradereportingextracts.models.AvailableReportResponse
import uk.gov.hmrc.tradereportingextracts.services.AvailableReportService
import uk.gov.hmrc.tradereportingextracts.utils.ApplicationConstants.eori

import scala.concurrent.{ExecutionContext, Future}

class AvailableReportControllerSpec extends PlaySpec with MockitoSugar {

  implicit val ec: ExecutionContext                        = ExecutionContext.Implicits.global
  implicit val hc: HeaderCarrier                           = HeaderCarrier()
  val mockService: AvailableReportService                  = mock[AvailableReportService]
  val appConfig: AppConfig                                 = mock[AppConfig]
  private val mockStubBehaviour                            = mock[StubBehaviour]
  private val backendAuthComponents: BackendAuthComponents =
    BackendAuthComponentsStub(mockStubBehaviour)(Helpers.stubControllerComponents())
  val controller                                           =
    new AvailableReportController(Helpers.stubControllerComponents(), backendAuthComponents, mockService)(using
      ec
    )
  val permission                                           = Predicate.Permission(
    Resource(ResourceType("trade-reporting-extracts"), ResourceLocation("trade-reporting-extracts/*")),
    IAAction("READ")
  )

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
      when(mockStubBehaviour.stubAuth(Some(permission), EmptyRetrieval))
        .thenReturn(Future.successful(EmptyRetrieval))
      val request = FakeRequest(GET, s"/api/available-reports")
        .withHeaders(CONTENT_TYPE -> JSON, AUTHORIZATION -> "my-token")
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
      when(mockStubBehaviour.stubAuth(Some(permission), EmptyRetrieval))
        .thenReturn(Future.successful(EmptyRetrieval))
      val request = FakeRequest().withJsonBody(Json.obj()).withHeaders(AUTHORIZATION -> "my-token")
      val result  = controller.getAvailableReports()(request)

      status(result) mustBe BAD_REQUEST
    }
  }

  "getAvailableReportsCount" should {
    "return Ok with count when EORI is present" in {
      when(mockService.getAvailableReportsCount(any[String]))
        .thenReturn(Future.successful(25))
      when(mockStubBehaviour.stubAuth(Some(permission), EmptyRetrieval))
        .thenReturn(Future.successful(EmptyRetrieval))
      val request =
        FakeRequest().withJsonBody(Json.obj("eori" -> "GB123456789000")).withHeaders(AUTHORIZATION -> "my-token")
      val result  = controller.getAvailableReportsCount()(request)

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(25)
    }

    "return BadRequest when EORI is missing" in {
      when(mockStubBehaviour.stubAuth(Some(permission), EmptyRetrieval))
        .thenReturn(Future.successful(EmptyRetrieval))
      val request = FakeRequest().withJsonBody(Json.obj()).withHeaders(AUTHORIZATION -> "my-token")
      val result  = controller.getAvailableReportsCount()(request)

      status(result) mustBe BAD_REQUEST
    }
  }

  "auditReportDownload" should {
    "return NoContent when audit succeeds" in {
      when(mockStubBehaviour.stubAuth(Some(permission), EmptyRetrieval))
        .thenReturn(Future.successful(EmptyRetrieval))
      when(mockService.processReportDownloadAudit(any())(any())).thenReturn(Future.successful(Right(())))

      val request                = FakeRequest().withHeaders(AUTHORIZATION -> "my-token").withJsonBody(Json.obj("foo" -> "bar"))
      val result: Future[Result] = controller.auditReportDownload(request)

      status(result) mustBe NO_CONTENT
    }

    "return error result when audit fails" in {
      val errorResult = BadRequest("error")
      when(mockStubBehaviour.stubAuth(Some(permission), EmptyRetrieval))
        .thenReturn(Future.successful(EmptyRetrieval))
      when(mockService.processReportDownloadAudit(any())(any())).thenReturn(Future.successful(Left(errorResult)))

      val request                = FakeRequest().withHeaders(AUTHORIZATION -> "my-token").withJsonBody(Json.obj("foo" -> "bar"))
      val result: Future[Result] = controller.auditReportDownload(request)

      status(result) mustBe BAD_REQUEST
      contentAsString(result) must include("error")
    }
  }
}
