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
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json
import play.api.test.*
import play.api.test.Helpers.*
import uk.gov.hmrc.tradereportingextracts.services.UserInformationService

import scala.concurrent.Future

class UserControllerSpec extends AnyFreeSpec with Matchers with MockitoSugar {
  "UserControllerSpec" - {
    "getAuthorisedEoris" - {

      val mockUserService = mock[UserInformationService]
      val cc              = Helpers.stubControllerComponents()
      val controller      = new UserController(mockUserService, cc)(using cc.executionContext)

      val testEori        = "EORI1234"
      val authorisedEoris = Seq("AUTH-EORI-1", "AUTH-EORI-2")

      "when service succeeds" - {
        "should return 200 OK with JSON body" in {
          when(mockUserService.getAuthorisedEoris(testEori)).thenReturn(Future.successful(authorisedEoris))

          val request  = FakeRequest(GET, s"/authorised-eoris/$testEori")
          val response = controller.getAuthorisedEoris(testEori).apply(request)

          status(response) mustBe OK
          contentAsJson(response) mustBe Json.toJson(authorisedEoris)
        }
      }

      "when service fails" - {
        "should return 500 InternalServerError with error message" in {
          val exception = new Exception("User not found")
          when(mockUserService.getAuthorisedEoris(testEori)).thenReturn(Future.failed(exception))

          val request  = FakeRequest(GET, s"/authorised-eoris/$testEori")
          val response = controller.getAuthorisedEoris(testEori).apply(request)

          status(response) mustBe INTERNAL_SERVER_ERROR
          contentAsString(response) must include("User not found")
        }
      }
    }
  }
}
