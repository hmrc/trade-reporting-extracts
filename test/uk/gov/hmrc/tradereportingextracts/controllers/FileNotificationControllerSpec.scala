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
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json
import play.api.test.Helpers.*
import play.api.test.{FakeRequest, Helpers}
import play.api.{Application, inject}
import uk.gov.hmrc.tradereportingextracts.models.sdes.*
import uk.gov.hmrc.tradereportingextracts.services.FileNotificationService
import uk.gov.hmrc.tradereportingextracts.utils.SpecBase

class FileNotificationControllerSpec extends SpecBase with MockitoSugar {

  "FileNotificationController" should {

    "return 400 BadRequest when body is not JSON" in new Setup {
      val request = FakeRequest(POST, routes.FileNotificationController.fileNotification().url)
        .withHeaders(
          "authorization"         -> "Bearer SdesAuthToken",
          "date"                  -> "Mon, 02 Oct 2023 14:30:00 GMT",
          "x-correlation-id"      -> "asfd-asdf-asdf",
          "source-system"         -> "SDES",
          "x-transmitting-system" -> "SDES"
        )
        .withBody("not-json")
      val result  = route(app, request).value
      status(result)        shouldBe BAD_REQUEST
      contentAsString(result) should include("Expected application/json request body")
    }

    "return 400 BadRequest when JSON is invalid" in new Setup {
      val invalidJson = Json.obj("foo" -> "bar")
      val request     = FakeRequest(POST, routes.FileNotificationController.fileNotification().url)
        .withHeaders(
          "authorization"         -> "Bearer SdesAuthToken",
          "date"                  -> "Mon, 02 Oct 2023 14:30:00 GMT",
          "x-correlation-id"      -> "asfd-asdf-asdf",
          "source-system"         -> "SDES",
          "x-transmitting-system" -> "SDES"
        )
        .withBody(invalidJson)
      val result      = route(app, request).value
      status(result)        shouldBe BAD_REQUEST
      contentAsString(result) should include("Invalid value at path")
    }

    "still return 201 even when FileNotificationService returns error as external caller is not concerned with our errors" in new Setup {
      val fileNotification = FileNotificationResponse(
        eori = "GB123456789012",
        fileName = "testFileName",
        fileSize = 12345,
        metadata = List(
          FileNotificationMetadata.RetentionDaysMetadataItem("30"),
          FileNotificationMetadata.FileTypeMetadataItem("CSV")
        )
      )

      when(mockFileNotificationService.processFileNotification(fileNotification))
        .thenReturn(
          scala.concurrent.Future.successful((BAD_REQUEST, "report-requestID not found in FileNotification metadata"))
        )

      val request = FakeRequest(POST, routes.FileNotificationController.fileNotification().url)
        .withHeaders(
          "authorization"         -> "Bearer SdesAuthToken",
          "date"                  -> "Mon, 02 Oct 2023 14:30:00 GMT",
          "x-correlation-id"      -> "asfd-asdf-asdf",
          "source-system"         -> "SDES",
          "x-transmitting-system" -> "SDES"
        )
        .withBody(Json.toJson(fileNotification))
      val result  = route(app, request).value
      status(result) shouldBe CREATED
    }

    "return 201 Created" in new Setup {
      val fileNotification = FileNotificationResponse(
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
      when(mockFileNotificationService.processFileNotification(fileNotification))
        .thenReturn(scala.concurrent.Future.successful((CREATED, "Created")))

      val request = FakeRequest(POST, routes.FileNotificationController.fileNotification().url)
        .withHeaders(
          "authorization"         -> "Bearer SdesAuthToken",
          "date"                  -> "Mon, 02 Oct 2023 14:30:00 GMT",
          "x-correlation-id"      -> "asfd-asdf-asdf",
          "source-system"         -> "SDES",
          "x-transmitting-system" -> "SDES"
        )
        .withBody(Json.toJson(fileNotification))
      val result  = route(app, request).value
      status(result) shouldBe CREATED
    }

    "return 404 MethodNotAllowed" in new Setup {
      val request = FakeRequest(GET, routes.FileNotificationController.fileNotification().url)
      val result  = route(app, request).value
      status(result) shouldBe 404
    }
  }

  trait Setup {
    val mockFileNotificationService: FileNotificationService = mock[FileNotificationService]
    val app: Application                                     = application
      .overrides(
        inject.bind[FileNotificationService].toInstance(mockFileNotificationService)
      )
      .build()
  }
}
