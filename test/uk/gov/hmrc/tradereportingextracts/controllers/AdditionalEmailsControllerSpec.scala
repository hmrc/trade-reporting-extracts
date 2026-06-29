/*
 * Copyright 2026 HM Revenue & Customs
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

import org.apache.pekko.actor.setup.Setup
import org.mockito.Mockito.when
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Result
import play.api.test.{FakeRequest, Helpers}
import play.api.test.Helpers.*
import uk.gov.hmrc.tradereportingextracts.services.AdditionalEmailService
import uk.gov.hmrc.tradereportingextracts.utils.SpecBase
import uk.gov.hmrc.tradereportingextracts.controllers.support.FakeAuth

import scala.concurrent.{ExecutionContext, Future}

class AdditionalEmailsControllerSpec extends SpecBase {

  implicit val ec: ExecutionContext                   = ExecutionContext.Implicits.global
  private val mockUserService: AdditionalEmailService = mock[AdditionalEmailService]
  private val fakeAuthAction                          = FakeAuth.Helpers.success(ec)

  val controller =
    new AdditionalEmailsController(
      mockUserService,
      Helpers.stubControllerComponents(),
      fakeAuthAction
    )(using ec)

  private val eori         = "GB123456789000"
  private val emailAddress = "test@example.com"

  "UserController.addAdditionalEmail" should {

    "return 200 OK when valid EORI and email address are provided" in new Setup {

      when(mockUserService.addAdditionalEmail(eori, emailAddress))
        .thenReturn(Future.successful(true))

      val request: FakeRequest[JsObject] = FakeRequest(POST, routes.AdditionalEmailsController.addAdditionalEmail().url)
        .withHeaders("Content-Type" -> "application/json", AUTHORIZATION -> "my-token")
        .withBody(Json.obj("eori" -> eori, "emailAddress" -> emailAddress))

      val result: Future[Result] = controller.addAdditionalEmail().apply(request)

      status(result) shouldBe OK
    }

    "return corresponding error code when call to auth fails" in new Setup {

      private val fakeFailingAuthAction = FakeAuth.Helpers.forbidden(ec)

      val controller =
        new AdditionalEmailsController(
          mockUserService,
          Helpers.stubControllerComponents(),
          fakeFailingAuthAction
        )(using ec)

      when(mockUserService.addAdditionalEmail(eori, emailAddress))
        .thenReturn(Future.successful(true))

      val request: FakeRequest[JsObject] = FakeRequest(POST, routes.AdditionalEmailsController.addAdditionalEmail().url)
        .withHeaders("Content-Type" -> "application/json", AUTHORIZATION -> "my-token")
        .withBody(Json.obj("eori" -> eori, "emailAddress" -> emailAddress))

      val result: Future[Result] = controller.addAdditionalEmail().apply(request)

      status(result) shouldBe FORBIDDEN
    }

    "return 500 InternalServerError when service fails to add additional email" in new Setup {

      when(mockUserService.addAdditionalEmail(eori, emailAddress))
        .thenReturn(Future.successful(false))

      val request: FakeRequest[JsObject] = FakeRequest(POST, routes.AdditionalEmailsController.addAdditionalEmail().url)
        .withHeaders("Content-Type" -> "application/json", AUTHORIZATION -> "my-token")
        .withBody(Json.obj("eori" -> eori, "emailAddress" -> emailAddress))

      val result: Future[Result] = controller.addAdditionalEmail().apply(request)

      status(result)        shouldBe INTERNAL_SERVER_ERROR
      contentAsString(result) should include("Failed to add additional email")
    }
  }
  "AdditionalEmailsController.removeAdditionalEmail" should {

    "return 204 NoContent when valid EORI and email address are provided and removed successfully" in new Setup {

      when(mockUserService.removeAdditionalEmail(eori, emailAddress))
        .thenReturn(Future.successful(true))

      val request: FakeRequest[JsObject] =
        FakeRequest(POST, routes.AdditionalEmailsController.removeAdditionalEmail().url)
          .withHeaders("Content-Type" -> "application/json", AUTHORIZATION -> "my-token")
          .withBody(Json.obj("eori" -> eori, "emailAddress" -> emailAddress))

      val result: Future[Result] = controller.removeAdditionalEmail().apply(request)

      status(result) shouldBe NO_CONTENT
    }

    "return 404 NotFound when the additional email address is not found" in new Setup {

      when(mockUserService.removeAdditionalEmail(eori, emailAddress))
        .thenReturn(Future.successful(false))

      val request: FakeRequest[JsObject] =
        FakeRequest(POST, routes.AdditionalEmailsController.removeAdditionalEmail().url)
          .withHeaders("Content-Type" -> "application/json", AUTHORIZATION -> "my-token")
          .withBody(Json.obj("eori" -> eori, "emailAddress" -> emailAddress))

      val result: Future[Result] = controller.removeAdditionalEmail().apply(request)

      status(result)        shouldBe NOT_FOUND
      contentAsString(result) should include("Additional email address not found")
    }

    "return 500 InternalServerError when service fails with an exception" in new Setup {

      when(mockUserService.removeAdditionalEmail(eori, emailAddress))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      val request: FakeRequest[JsObject] =
        FakeRequest(POST, routes.AdditionalEmailsController.removeAdditionalEmail().url)
          .withHeaders("Content-Type" -> "application/json", AUTHORIZATION -> "my-token")
          .withBody(Json.obj("eori" -> eori, "emailAddress" -> emailAddress))

      val result: Future[Result] = controller.removeAdditionalEmail().apply(request)

      status(result)        shouldBe INTERNAL_SERVER_ERROR
      contentAsString(result) should include("Failed to remove additional email")
    }

    "return 400 BadRequest when required fields are missing" in new Setup {
      val eori = "GB123456789000"

      val request: FakeRequest[JsObject] =
        FakeRequest(POST, routes.AdditionalEmailsController.removeAdditionalEmail().url)
          .withHeaders("Content-Type" -> "application/json", AUTHORIZATION -> "my-token")
          .withBody(Json.obj("eori" -> eori))

      val result: Future[Result] = controller.removeAdditionalEmail().apply(request)

      status(result) shouldBe BAD_REQUEST
    }
  }
}
