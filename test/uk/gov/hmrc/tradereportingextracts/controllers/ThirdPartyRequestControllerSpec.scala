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
import play.api.libs.json.Json
import play.api.mvc.ControllerComponents
import play.api.test.Helpers.*
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.tradereportingextracts.models._
import uk.gov.hmrc.tradereportingextracts.models.thirdParty.ThirdPartyAddedConfirmation
import uk.gov.hmrc.tradereportingextracts.services.UserService

import scala.concurrent.{ExecutionContext, Future}

class ThirdPartyRequestControllerSpec extends AnyFreeSpec with Matchers with MockitoSugar with ScalaFutures {

  val cc: ControllerComponents      = Helpers.stubControllerComponents()
  val userService: UserService      = mock[UserService]
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  val controller                    = new ThirdPartyRequestController(cc, userService)

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

      when(userService.addAuthorisedUser(any(), any()))
        .thenReturn(Future.successful(confirmation))

      val result = controller.addThirdPartyRequest()(FakeRequest().withBody(requestBody))
      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(confirmation)
    }

    "should return 400 BadRequest for invalid JSON" in {
      val invalidJson = Json.parse("""{"foo": "bar"}""")
      val result      = controller.addThirdPartyRequest()(FakeRequest().withBody(invalidJson))
      status(result) mustBe BAD_REQUEST
      (contentAsJson(result) \ "error").as[String] must include("Invalid request format")
    }
  }
}
