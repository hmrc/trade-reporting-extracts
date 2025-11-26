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

import play.api.Application
import play.api.libs.json.Json
import play.api.test.Helpers.*
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.tradereportingextracts.models.etmp.EoriUpdate
import uk.gov.hmrc.tradereportingextracts.utils.SpecBase

class EoriUpdateControllerSpec extends SpecBase {

  "EoriUpdateController" should {
    "return 201 Created" in new Setup {
      val eoriUpdate = EoriUpdate(newEori = "GB987654321098", oldEori = "GB123456789012")
      val request    = FakeRequest(PUT, routes.EoriUpdateController.eoriUpdate().url)
        .withHeaders(
          "authorization"         -> "Bearer EtmpAuthToken",
          "content-type"          -> "application/json",
          "date"                  -> "Mon, 02 Oct 2023 14:30:00 GMT",
          "x-correlation-id"      -> "asfd-asdf-asdf",
          "x-transmitting-system" -> "CDAP",
          "source-system"         -> "CDAP"
        )
        .withBody(Json.toJson(eoriUpdate))
      val result     = route(app, request).value
      status(result) shouldBe CREATED
    }
  }
  "return 400 BadRequest when missing headers" in new Setup {
    val request = FakeRequest(PUT, routes.EoriUpdateController.eoriUpdate().url)
      .withHeaders(
        "authorization"    -> "Bearer EtmpAuthToken",
        "date"             -> "Mon, 02 Oct 2023 14:30:00 GMT",
        "x-correlation-id" -> "asfd-asdf-asdf",
        "x-forwarded-host" -> "localhost:9000",
        "content-type"     -> "application/json"
      )
    val result  = route(app, request).value
    status(result) shouldBe BAD_REQUEST
  }

  "return 403 forbidden when not authorised" in new Setup {
    val eoriUpdate = EoriUpdate(newEori = "GB987654321098", oldEori = "GB123456789012")
    val request    = FakeRequest(PUT, routes.EoriUpdateController.eoriUpdate().url)
      .withHeaders(
        "authorization"         -> "wrongToken",
        "content-type"          -> "application/json",
        "date"                  -> "Mon, 02 Oct 2023 14:30:00 GMT",
        "x-correlation-id"      -> "asfd-asdf-asdf",
        "x-transmitting-system" -> "CDAP",
        "source-system"         -> "CDAP"
      )
      .withBody(Json.toJson(eoriUpdate))
    val result     = route(app, request).value
    status(result) shouldBe FORBIDDEN
  }

  "return 400 BadRequest when missing body" in new Setup {
    val eoriUpdate = EoriUpdate(newEori = "GB987654321098", oldEori = "GB123456789012")
    val request    = FakeRequest(PUT, routes.EoriUpdateController.eoriUpdate().url)
      .withHeaders(
        "authorization"         -> "Bearer EtmpAuthToken",
        "content-type"          -> "application/json",
        "date"                  -> "Mon, 02 Oct 2023 14:30:00 GMT",
        "x-correlation-id"      -> "asfd-asdf-asdf",
        "x-transmitting-system" -> "CDAP",
        "source-system"         -> "CDAP"
      )
    val result     = route(app, request).value
    status(result) shouldBe BAD_REQUEST
  }

  "return 400 BadRequest when invalid body" in new Setup {
    val eoriUpdate = EoriUpdate(newEori = "GB987654321098", oldEori = "GB123456789012")
    val request    = FakeRequest(PUT, routes.EoriUpdateController.eoriUpdate().url)
      .withHeaders(
        "authorization"         -> "Bearer EtmpAuthToken",
        "content-type"          -> "application/json",
        "date"                  -> "Mon, 02 Oct 2023 14:30:00 GMT",
        "x-correlation-id"      -> "asfd-asdf-asdf",
        "x-transmitting-system" -> "CDAP",
        "source-system"         -> "CDAP"
      )
      .withBody(Json.obj("invalidField" -> "invalidValue"))
    val result     = route(app, request).value
    status(result) shouldBe BAD_REQUEST
  }

  trait Setup {
    val app: Application = application.build()
  }

}
