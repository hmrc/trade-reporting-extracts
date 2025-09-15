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

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.ControllerComponents
import play.api.test.Helpers.*
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.internalauth.client.Retrieval.EmptyRetrieval
import uk.gov.hmrc.internalauth.client.{BackendAuthComponents, IAAction, Predicate, Resource, ResourceLocation, ResourceType}
import uk.gov.hmrc.internalauth.client.test.{BackendAuthComponentsStub, StubBehaviour}
import uk.gov.hmrc.tradereportingextracts.models.*
import uk.gov.hmrc.tradereportingextracts.models.thirdParty.ThirdPartyAddedConfirmation
import uk.gov.hmrc.tradereportingextracts.services.UserService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ThirdPartyRequestControllerSpec extends AnyFreeSpec with Matchers with MockitoSugar with ScalaFutures {

  private val cc: ControllerComponents                     = Helpers.stubControllerComponents()
  private val userService: UserService                     = mock[UserService]
  private val mockStubBehaviour                            = mock[StubBehaviour]
  private val backendAuthComponents: BackendAuthComponents =
    BackendAuthComponentsStub(mockStubBehaviour)(cc)
  private val controller                                   = new ThirdPartyRequestController(cc, userService, backendAuthComponents)
  private val permission: Predicate.Permission             = Predicate.Permission(
    Resource(ResourceType("trade-reporting-extracts"), ResourceLocation("trade-reporting-extracts/*")),
    IAAction("READ")
  )

  "addThirdPartyRequest" - {

    "should return 200 OK with confirmation for valid request" in {
      val requestBody = Json.parse("""
                                     |{
                                     |  "userEORI":"GB987654321098",
                                     |  "thirdPartyEORI":"GB123456123456",
                                     |  "accessStart":"2025-09-09T00:00:00Z",
                                     |  "accessEnd":"2025-09-09T10:59:38.334682780Z",
                                     |  "reportDateStart":"2025-09-10T00:00:00Z",
                                     |  "reportDateEnd":"2025-09-09T10:59:38.334716742Z",
                                     |  "accessType":["IMPORT","EXPORT"],
                                     |  "referenceName":"TestReport"
                                     |}
        """.stripMargin)

      val confirmation = ThirdPartyAddedConfirmation(
        thirdPartyEori = "GB123456123456"
      )
      when(mockStubBehaviour.stubAuth(Some(permission), EmptyRetrieval))
        .thenReturn(Future.successful(EmptyRetrieval))
      when(userService.addAuthorisedUser(any(), any()))
        .thenReturn(Future.successful(confirmation))

      val result =
        controller.addThirdPartyRequest()(FakeRequest().withHeaders(AUTHORIZATION -> "my-token").withBody(requestBody))
      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(confirmation)
    }

    "should return 400 BadRequest for invalid JSON" in {
      val invalidJson = Json.parse("""{"foo": "bar"}""")
      val result      =
        controller.addThirdPartyRequest()(FakeRequest().withHeaders(AUTHORIZATION -> "my-token").withBody(invalidJson))
      status(result) mustBe BAD_REQUEST
      (contentAsJson(result) \ "error").as[String] must include("Invalid request format")
    }
  }

  "deleteThirdPartyDetails" - {

    "should return 204 NoContent when authorised user is removed" in {
      val requestBody = Json.parse("""
                                     |{
                                     |  "eori":"GB987654321098",
                                     |  "thirdPartyEori":"GB123456123456"
                                     |}
        """.stripMargin)

      when(userService.deleteAuthorisedUser(any(), any()))
        .thenReturn(Future.successful(true))

      val result = controller.deleteThirdPartyDetails()(
        FakeRequest().withHeaders(AUTHORIZATION -> "my-token").withBody(requestBody)
      )
      status(result) mustBe NO_CONTENT
    }

    "should return 404 NotFound when authorised user is not found" in {
      val requestBody = Json.parse("""
                                     |{
                                     |  "eori":"GB987654321098",
                                     |  "thirdPartyEori":"GB000000000000"
                                     |}
        """.stripMargin)

      when(userService.deleteAuthorisedUser(any(), any()))
        .thenReturn(Future.successful(false))

      val result = controller.deleteThirdPartyDetails()(
        FakeRequest().withHeaders(AUTHORIZATION -> "my-token").withBody(requestBody)
      )
      status(result) mustBe NOT_FOUND
      contentAsString(result) must include("No authorised user found for third party EORI")
    }

    "should return 400 BadRequest for invalid JSON" in {
      val invalidJson = Json.parse("""{"foo": "bar"}""")
      val result      = controller.deleteThirdPartyDetails()(
        FakeRequest().withHeaders(AUTHORIZATION -> "my-token").withBody(invalidJson)
      )
      status(result) mustBe BAD_REQUEST
      contentAsString(result) must include("Missing or invalid")
    }
  }
}
