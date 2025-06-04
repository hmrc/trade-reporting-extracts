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
import play.api.{Application, inject}
import play.api.libs.json.{JsObject, Json}
import play.api.test.Helpers.*
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.tradereportingextracts.models.FileNotification
import uk.gov.hmrc.tradereportingextracts.models.sdes.*
import uk.gov.hmrc.tradereportingextracts.services.FileNotificationService
import uk.gov.hmrc.tradereportingextracts.utils.SpecBase

class FileNotificationControllerSpec extends SpecBase with MockitoSugar {

  "FileNotificationController" should {
    "return 400 BadRequest when headers are missing" in new Setup {
      val request = FakeRequest(PUT, routes.FileNotificationController.fileNotification().url)
      val result  = route(app, request).value
      status(result)        shouldBe BAD_REQUEST
      contentAsString(result) should include("Failed header validation")
    }

    "return 400 BadRequest when body is not JSON" in new Setup {
      val request = FakeRequest(PUT, routes.FileNotificationController.fileNotification().url)
        .withHeaders(
          "authorization"         -> "SdesAuthToken",
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
      val request     = FakeRequest(PUT, routes.FileNotificationController.fileNotification().url)
        .withHeaders(
          "authorization"         -> "SdesAuthToken",
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

    "return 400 BadRequest when report-requestID is missing in metadata" in new Setup {
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

      val request = FakeRequest(PUT, routes.FileNotificationController.fileNotification().url)
        .withHeaders(
          "authorization"         -> "SdesAuthToken",
          "date"                  -> "Mon, 02 Oct 2023 14:30:00 GMT",
          "x-correlation-id"      -> "asfd-asdf-asdf",
          "source-system"         -> "SDES",
          "x-transmitting-system" -> "SDES"
        )
        .withBody(Json.toJson(fileNotification))
      val result  = route(app, request).value
      status(result)        shouldBe BAD_REQUEST
      contentAsString(result) should include("report-requestID not found")
    }

    "return 404 NotFound when reportRequest is not found" in new Setup {
      val fileNotification = FileNotificationResponse(
        eori = "GB123456789012",
        fileName = "testFileName",
        fileSize = 12345,
        metadata = List(
          FileNotificationMetadata.RetentionDaysMetadataItem("30"),
          FileNotificationMetadata.FileTypeMetadataItem("CSV"),
          FileNotificationMetadata.MDTPReportRequestIDMetadataItem("NOT-FOUND")
        )
      )

      when(mockFileNotificationService.processFileNotification(fileNotification))
        .thenReturn(
          scala.concurrent.Future.successful((NOT_FOUND, "ReportRequest not found for reportRequestId: NOT-FOUND"))
        )

      val request = FakeRequest(PUT, routes.FileNotificationController.fileNotification().url)
        .withHeaders(
          "authorization"         -> "SdesAuthToken",
          "date"                  -> "Mon, 02 Oct 2023 14:30:00 GMT",
          "x-correlation-id"      -> "asfd-asdf-asdf",
          "source-system"         -> "SDES",
          "x-transmitting-system" -> "SDES"
        )
        .withBody(Json.toJson(fileNotification))
      val result  = route(app, request).value
      status(result)        shouldBe NOT_FOUND
      contentAsString(result) should include("ReportRequest not found")
    }

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
      val result  = route(app, request).value
      status(result) shouldBe FORBIDDEN
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

      val request = FakeRequest(PUT, routes.FileNotificationController.fileNotification().url)
        .withHeaders(
          "authorization"         -> "SdesAuthToken",
          "date"                  -> "Mon, 02 Oct 2023 14:30:00 GMT",
          "x-correlation-id"      -> "asfd-asdf-asdf",
          "source-system"         -> "SDES",
          "x-transmitting-system" -> "SDES"
        )
        .withBody(Json.toJson(fileNotification))
      val result  = route(app, request).value
      status(result) shouldBe CREATED
    }

    "return 405 MethodNotAllowed" in new Setup {
      val request = FakeRequest(GET, routes.FileNotificationController.fileNotification().url)
      val result  = route(app, request).value
      status(result) shouldBe METHOD_NOT_ALLOWED
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
