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

package uk.gov.hmrc.tradereportingextracts.models.sdes

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json._

class FileNotificationMetadataSpec extends AnyFreeSpec with Matchers {

  import FileNotificationMetadata._

  private val samples: Seq[FileNotificationMetadata] = Seq(
    RetentionDaysMetadataItem("30"),
    FileTypeMetadataItem("csv"),
    EORIMetadataItem("eori1"),
    MDTPReportXCorrelationIDMetadataItem("corr-123"),
    MDTPReportRequestIDMetadataItem("re123"),
    MDTPReportTypeNameMetadataItem("SomeReportType"),
    ReportFilesPartsMetadataItem("1"),
    ReportLastFileMetadataItem("true"),
    FileCreationTimestampMetadataItem("2026-03-25T11:00:00Z")
  )

  private val expectedJson: Map[FileNotificationMetadata, JsObject] = Map(
    RetentionDaysMetadataItem("30")                           ->
      Json.obj("metadata" -> "RETENTION_DAYS", "value" -> "30"),
    FileTypeMetadataItem("csv")                               ->
      Json.obj("metadata" -> "FileType", "value" -> "csv"),
    EORIMetadataItem("eori1")                                 ->
      Json.obj("metadata" -> "EORI", "value" -> "eori1"),
    MDTPReportXCorrelationIDMetadataItem("corr-123")          ->
      Json.obj("metadata" -> "MdtpReportXCorrelationId", "value" -> "corr-123"),
    MDTPReportRequestIDMetadataItem("re123")                  ->
      Json.obj("metadata" -> "MdtpReportRequestId", "value" -> "re123"),
    MDTPReportTypeNameMetadataItem("SomeReportType")          ->
      Json.obj("metadata" -> "MdtpReportTypeName", "value" -> "SomeReportType"),
    ReportFilesPartsMetadataItem("1")                         ->
      Json.obj("metadata" -> "ReportFileCounter", "value" -> "1"),
    ReportLastFileMetadataItem("true")                        ->
      Json.obj("metadata" -> "ReportLastFile", "value" -> "true"),
    FileCreationTimestampMetadataItem("2026-03-25T11:00:00Z") ->
      Json.obj("metadata" -> "FileCreationTimestamp", "value" -> "2026-03-25T11:00:00Z")
  )

  "FileNotificationMetadata " - {

    "must write to Json" - {
      samples.foreach { dataItem =>
        s"must write ${dataItem.getClass.getSimpleName} to expected JSON" in {
          Json.toJson(dataItem) mustBe expectedJson(dataItem)
        }
      }
    }

    "must read from Json" - {
      expectedJson.foreach { case (model, json) =>
        s"must read ${model.getClass.getSimpleName} from expected JSON" in {
          json.validate[FileNotificationMetadata] mustBe JsSuccess(model)
        }
      }
    }
  }
}
