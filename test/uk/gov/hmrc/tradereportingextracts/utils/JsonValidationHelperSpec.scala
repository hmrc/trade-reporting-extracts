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

package uk.gov.hmrc.tradereportingextracts.utils

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsError, JsSuccess}
import play.api.test.Helpers.*

import scala.concurrent.Future

class JsonValidationHelperSpec extends AnyWordSpec with Matchers {

  import JsonValidationHelper._

  "JsonValidationHelper.validateFields" should {

    "return Right(Map) when all fields are valid and non-empty" in {
      val result =
        validateFields(
          "eori"         -> JsSuccess("GB123456789000"),
          "emailAddress" -> JsSuccess("test@example.com")
        )

      result shouldBe Right(
        Map(
          "eori"         -> "GB123456789000",
          "emailAddress" -> "test@example.com"
        )
      )
    }

    "return Left(BadRequest) with singular 'field' when exactly one field is invalid" in {
      val result =
        validateFields(
          "eori"         -> JsSuccess("GB123456789000"),
          "emailAddress" -> JsError("error")
        )

      result match {
        case Left(res) =>
          res.header.status                       shouldBe BAD_REQUEST
          contentAsString(Future.successful(res)) shouldBe
            "Missing or invalid 'emailAddress' field"

        case Right(_) =>
          fail("Expected Left(BadRequest), but got Right")
      }
    }

    "return Left(BadRequest) with plural 'fields' when multiple fields are invalid" in {
      val result =
        validateFields(
          "eori"         -> JsError("error"),
          "emailAddress" -> JsError("error")
        )

      result match {
        case Left(res) =>
          res.header.status                       shouldBe BAD_REQUEST
          contentAsString(Future.successful(res)) shouldBe
            "Missing or invalid 'eori', 'emailAddress' fields"

        case Right(_) =>
          fail("Expected Left(BadRequest), but got Right")
      }
    }

    "treat blank or whitespace-only values as invalid" in {
      val result =
        validateFields(
          "eori"         -> JsSuccess("   "),
          "emailAddress" -> JsSuccess("test@example.com")
        )

      result match {
        case Left(res) =>
          res.header.status                       shouldBe BAD_REQUEST
          contentAsString(Future.successful(res)) shouldBe
            "Missing or invalid 'eori' field"

        case Right(_) =>
          fail("Expected Left(BadRequest), but got Right")
      }
    }

    "collect both JsError and blank values together and report them as invalid fields" in {
      val result =
        validateFields(
          "eori"         -> JsSuccess(""),
          "emailAddress" -> JsError("error")
        )

      result match {
        case Left(res) =>
          res.header.status                       shouldBe BAD_REQUEST
          contentAsString(Future.successful(res)) shouldBe
            "Missing or invalid 'eori', 'emailAddress' fields"

        case Right(_) =>
          fail("Expected Left(BadRequest), but got Right")
      }
    }
  }
}
