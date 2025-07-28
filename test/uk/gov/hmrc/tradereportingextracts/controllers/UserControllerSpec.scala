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
import play.api.{Application, inject}
import play.api.libs.json.{JsArray, JsObject, JsString, Json}
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.Helpers.*
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.tradereportingextracts.models.{AddressInformation, CompanyInformation, NotificationEmail, UserDetails}
import uk.gov.hmrc.tradereportingextracts.services.UserService
import uk.gov.hmrc.tradereportingextracts.utils.SpecBase

import java.time.LocalDateTime
import scala.concurrent.Future

class UserControllerSpec extends SpecBase {

  "UserController.getNotificationEmail" should {

    "return 200 OK with NotificationEmail when valid EORI is provided" in new Setup {
      val eori                     = "GB123456789000"
      val email: NotificationEmail = NotificationEmail("user@example.com", LocalDateTime.now())

      when(mockUserService.getNotificationEmail(eori))
        .thenReturn(Future.successful(email))

      val request: FakeRequest[JsObject] = FakeRequest(POST, routes.UserController.getNotificationEmail.url)
        .withHeaders("Content-Type" -> "application/json")
        .withBody(Json.obj("eori" -> eori))

      val result: Future[Result] = route(app, request).value

      status(result)        shouldBe OK
      contentAsJson(result) shouldBe Json.toJson(email)
    }

    "return 400 BadRequest when EORI is missing" in new Setup {
      val request: FakeRequest[JsObject] = FakeRequest(POST, routes.UserController.getNotificationEmail.url)
        .withHeaders("Content-Type" -> "application/json")
        .withBody(Json.obj("invalidField" -> "value"))

      val result: Future[Result] = route(app, request).value

      status(result)        shouldBe BAD_REQUEST
      contentAsString(result) should include("Missing or invalid 'eori' field")
    }

    "return 500 InternalServerError when service fails" in new Setup {
      val eori = "GB123456789000"

      when(mockUserService.getNotificationEmail(eori))
        .thenReturn(Future.failed(new RuntimeException("Service failure")))

      val request: FakeRequest[JsObject] = FakeRequest(POST, routes.UserController.getNotificationEmail.url)
        .withHeaders("Content-Type" -> "application/json")
        .withBody(Json.obj("eori" -> eori))

      val result: Future[Result] = route(app, request).value

      status(result)        shouldBe INTERNAL_SERVER_ERROR
      contentAsString(result) should include("Service failure")
    }
  }

  "UserController.setupUser" should {

    "return 201 Created with user details when valid EORI is provided" in new Setup {
      val eori                     = "GB123456789000"
      val userDetails: UserDetails = UserDetails(
        eori = eori,
        additionalEmails = Seq.empty,
        authorisedUsers = Seq.empty,
        companyInformation = CompanyInformation(), // assuming default constructor exists
        notificationEmail = NotificationEmail("user@example.com", LocalDateTime.now())
      )

      when(mockUserService.getOrCreateUser(eori))
        .thenReturn(Future.successful(userDetails))

      val request: FakeRequest[JsObject] = FakeRequest(GET, routes.UserController.setupUser().url)
        .withHeaders("Content-Type" -> "application/json")
        .withBody(Json.obj("eori" -> eori))

      val result: Future[Result] = route(app, request).value

      status(result)        shouldBe CREATED
      contentAsJson(result) shouldBe Json.toJson(userDetails)
    }

    "return 400 BadRequest when EORI is missing" in new Setup {
      val request: FakeRequest[JsObject] = FakeRequest(GET, routes.UserController.setupUser().url)
        .withHeaders("Content-Type" -> "application/json")
        .withBody(Json.obj("wrongField" -> "value"))

      val result: Future[Result] = route(app, request).value

      status(result)        shouldBe BAD_REQUEST
      contentAsString(result) should include("Missing or invalid 'eori' field")
    }
  }

  "UserController.getAuthorisedEoris" should {

    "return 200 OK with list of authorised EORIs" in new Setup {
      val eori                         = "GB123456789000"
      val authorisedEoris: Seq[String] = Seq("GB111111111111", "GB222222222222")

      when(mockUserService.getAuthorisedEoris(eori))
        .thenReturn(Future.successful(authorisedEoris))

      val request: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest(GET, routes.UserController.getAuthorisedEoris(eori).url)

      val result: Future[Result] = route(app, request).value

      status(result)        shouldBe OK
      contentAsJson(result) shouldBe JsArray(authorisedEoris.map(JsString(_)))
    }

    "return 500 InternalServerError when service fails" in new Setup {
      val eori = "GB123456789000"

      when(mockUserService.getAuthorisedEoris(eori))
        .thenReturn(Future.failed(new RuntimeException("Service failure")))

      val request: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest(GET, routes.UserController.getAuthorisedEoris(eori).url)

      val result: Future[Result] = route(app, request).value

      status(result)        shouldBe INTERNAL_SERVER_ERROR
      contentAsString(result) should include("Service failure")
    }
  }

  "UserController.getUserDetails" should {

    "return 201 OK with user details when valid EORI is provided" in new Setup {
      val eori                     = "GB123456789000"
      val userDetails: UserDetails = UserDetails(
        eori = eori,
        additionalEmails = Seq.empty,
        authorisedUsers = Seq.empty,
        companyInformation = CompanyInformation(
          name = "Test Company",
          consent = "1",
          address = AddressInformation(
            streetAndNumber = "123 Test Street",
            city = "Test City",
            postalCode = Some("12345"),
            countryCode = "GB"
          )
        ),
        notificationEmail = NotificationEmail("test@test.com", LocalDateTime.now())
      )
      when(mockUserService.getUserAndEmailDetails(eori)).thenReturn(Future.successful(userDetails))

      val request: FakeRequest[JsObject] = FakeRequest(GET, routes.UserController.getUserAndEmail.url)
        .withHeaders("Content-Type" -> "application/json")
        .withBody(Json.obj("eori" -> eori))
      val result: Future[Result]         = route(app, request).value
      status(result)        shouldBe CREATED
      contentAsJson(result) shouldBe Json.toJson(userDetails)
    }
  }

  trait Setup {
    val mockUserService: UserService = mock[UserService]

    val app: Application = application
      .overrides(inject.bind[UserService].toInstance(mockUserService))
      .build()
  }
}
