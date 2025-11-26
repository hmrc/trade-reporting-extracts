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
import uk.gov.hmrc.tradereportingextracts.models.eis.EisReportStatusRequest
import uk.gov.hmrc.tradereportingextracts.utils.SpecBase

class ReportStatusControllerSpec extends SpecBase {

  "ReportStatusController" should {
    "return 400 BadRequest" in new Setup {
      val request = FakeRequest(PUT, routes.ReportStatusController.notifyReportStatus().url)

      val result = route(app, request).value
      status(result) shouldBe BAD_REQUEST
    }
  }
  "return 403 Forbidden" in new Setup {
    val request = FakeRequest(PUT, routes.ReportStatusController.notifyReportStatus().url)
      .withHeaders(
        "content-type"          -> "application/json",
        "authorization"         -> "Invalid-auth-token",
        "date"                  -> "Mon, 02 Oct 2023 14:30:00 GMT",
        "x-correlation-id"      -> "asfd-asdf-asdf",
        "x-transmitting-system" -> "CDAP",
        "source-system"         -> "CDAP"
      )

    val result = route(app, request).value
    status(result) shouldBe FORBIDDEN

  }

  "return 201 Created" in new Setup {
    val eisReportStatusRequest = EisReportStatusRequest(
      applicationComponent = EisReportStatusRequest.ApplicationComponent.CDAP,
      statusCode = "200",
      statusMessage = "Report processed successfully",
      statusTimestamp = "2023-10-02T14:30:00Z",
      statusType = EisReportStatusRequest.StatusType.INFORMATION
    )
    val request                = FakeRequest(PUT, routes.ReportStatusController.notifyReportStatus().url)
      .withHeaders(
        "content-type"          -> "application/json",
        "authorization"         -> "Bearer EisAuthToken",
        "date"                  -> "Mon, 02 Oct 2023 14:30:00 GMT",
        "x-correlation-id"      -> "asfd-asdf-asdf",
        "x-transmitting-system" -> "CDAP",
        "source-system"         -> "CDAP"
      )
      .withBody(Json.toJson(eisReportStatusRequest))

    val result = route(app, request).value
    status(result) shouldBe CREATED

  }
  "return 404 MethodNotAllowed" in new Setup {
    val request = FakeRequest(GET, routes.ReportStatusController.notifyReportStatus().url)

    val result = route(app, request).value
    status(result) shouldBe 404

  }

  "return 400 when missing body" in new Setup {
    val request = FakeRequest(PUT, routes.ReportStatusController.notifyReportStatus().url)
      .withHeaders(
        "content-type"          -> "application/json",
        "authorization"         -> "Bearer EisAuthToken",
        "date"                  -> "Mon, 02 Oct 2023 14:30:00 GMT",
        "x-correlation-id"      -> "asfd-asdf-asdf",
        "x-transmitting-system" -> "CDAP",
        "source-system"         -> "CDAP"
      )

    val result = route(app, request).value
    status(result) shouldBe 400
  }

  "return 400 when invalid body" in new Setup {
    val request = FakeRequest(PUT, routes.ReportStatusController.notifyReportStatus().url)
      .withHeaders(
        "content-type"          -> "application/json",
        "authorization"         -> "Bearer EisAuthToken",
        "date"                  -> "Mon, 02 Oct 2023 14:30:00 GMT",
        "x-correlation-id"      -> "asfd-asdf-asdf",
        "x-transmitting-system" -> "CDAP",
        "source-system"         -> "CDAP"
      )
      .withBody(Json.obj("invalidField" -> "invalidValue"))

    val result = route(app, request).value
    status(result) shouldBe 400
  }

  trait Setup {
    val app: Application = application.build()
  }

}
