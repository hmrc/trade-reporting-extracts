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
import uk.gov.hmrc.tradereportingextracts.models.sdes.*
import uk.gov.hmrc.tradereportingextracts.utils.SpecBase

class FileNotificationControllerSpec extends SpecBase {

  "FileNotificationController" should {
    "return 400 BadRequest" in new Setup {
      val request = FakeRequest(PUT, routes.FileNotificationController.fileNotification().url)

      val result = route(app, request).value
      status(result) shouldBe BAD_REQUEST
    }
  }
  "FileNotificationController" should {
    "return 403 Forbidden" in new Setup {
      val request = FakeRequest(PUT, routes.FileNotificationController.fileNotification().url)
        .withHeaders(
          "content-type"          -> "application/json",
          "authorization"         -> "Invalid-auth-token",
          "date"                  -> "Mon, 02 Oct 2023 14:30:00 GMT",
          "x-correlation-id"      -> "asfd-asdf-asdf",
          "source-system"         -> "SDES",
          "x-transmitting-system" -> "SDES"
        )

      val result = route(app, request).value
      println(contentAsString(result))
      status(result) shouldBe FORBIDDEN
    }
  }
  "FileNotificationController" should {
    "return 201 Created" in new Setup {
      val fileNotification = FileNotification(
        eori = "GB123456789012",
        fileName = "testFileName",
        fileSize = 12345,
        metadata = List(
          FileNotificationMetadata.RetentionDaysMetadataItem("30"),
          FileNotificationMetadata.FileTypeMetadataItem("CSV"),
          FileNotificationMetadata.EORIMetadataItem("GB123456789012"),
          FileNotificationMetadata.MDTPReportXCorrelationIDMetadataItem("asfd-asdf-asdf"),
          FileNotificationMetadata.MDTPReportRequestIDMetadataItem("TRE-19"),
          FileNotificationMetadata.MDTPReportTypeNameMetadataItem("IMPORT-HEADER"),
          FileNotificationMetadata.ReportFilesPartsMetadataItem("1of2")
        )
      )
      val request          = FakeRequest(PUT, routes.FileNotificationController.fileNotification().url)
        .withHeaders(
          "authorization"         -> "SdesAuthToken",
          "date"                  -> "Mon, 02 Oct 2023 14:30:00 GMT",
          "x-correlation-id"      -> "asfd-asdf-asdf",
          "source-system"         -> "SDES",
          "x-transmitting-system" -> "SDES"
        )
        .withBody(Json.toJson(fileNotification))

      val result = route(app, request).value
      status(result) shouldBe CREATED
    }
  }

  trait Setup {
    val app: Application = application.build()
  }

}
