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

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json

class FileAvailableResponseSpec extends AnyWordSpec with Matchers {

  "FileAvailableResponse JSON serialization" should {
    "serialize and deserialize correctly for all metadata types" in {
      val response = FileAvailableResponse(
        filename = "file.csv",
        downloadURL = "http://example.com/file.csv",
        fileSize = 12345L,
        metadata = Seq(
          FileAvailableMetadataItem.RetentionDaysMetadataItem("30"),
          FileAvailableMetadataItem.FileTypeMetadataItem("CSV"),
          FileAvailableMetadataItem.EORIMetadataItem("GB123456789000"),
          FileAvailableMetadataItem.MDTPReportXCorrelationIDMetadataItem("corr-id"),
          FileAvailableMetadataItem.MDTPReportRequestIDMetadataItem("req-id"),
          FileAvailableMetadataItem.MDTPReportTypeNameMetadataItem("type"),
          FileAvailableMetadataItem.ReportFileCounterMetadataItem("1"),
          FileAvailableMetadataItem.ReportLastFileMetadataItem("true")
        )
      )
      val json = Json.toJson(response)
      val parsed = json.as[FileAvailableResponse]
      parsed shouldBe response
    }

    "fail to deserialize unknown metadata type" in {
      val json = Json.parse(
        """
          |{
          |  "filename": "file.csv",
          |  "downloadURL": "http://example.com/file.csv",
          |  "fileSize": 12345,
          |  "metadata": [
          |    { "metadata": "UNKNOWN_TYPE", "value": "foo" }
          |  ]
          |}
          |""".stripMargin)
      val result = json.validate[FileAvailableResponse]
      result.isError shouldBe true
    }
  }

  "FileAvailableMetadataItem" should {
    "correctly extract metadata and value for each subtype" in {
      FileAvailableMetadataItem.RetentionDaysMetadataItem("10").metadata shouldBe "RETENTION_DAYS"
      FileAvailableMetadataItem.FileTypeMetadataItem("PDF").metadata shouldBe "FileType"
      FileAvailableMetadataItem.EORIMetadataItem("GB123").metadata shouldBe "EORI"
      FileAvailableMetadataItem.MDTPReportXCorrelationIDMetadataItem("xid").metadata shouldBe "MdtpReportXCorrelationId"
      FileAvailableMetadataItem.MDTPReportRequestIDMetadataItem("rid").metadata shouldBe "MdtpReportRequestId"
      FileAvailableMetadataItem.MDTPReportTypeNameMetadataItem("type").metadata shouldBe "MdtpReportTypeName"
      FileAvailableMetadataItem.ReportFileCounterMetadataItem("2").metadata shouldBe "ReportFileCounter"
      FileAvailableMetadataItem.ReportLastFileMetadataItem("false").metadata shouldBe "ReportLastFile"
    }
  }

  "FileAvailableMetadataItem JSON format" should {
    "serialize and deserialize each subtype correctly" in {
      val items = Seq(
        FileAvailableMetadataItem.RetentionDaysMetadataItem("10"),
        FileAvailableMetadataItem.FileTypeMetadataItem("PDF"),
        FileAvailableMetadataItem.EORIMetadataItem("GB123"),
        FileAvailableMetadataItem.MDTPReportXCorrelationIDMetadataItem("xid"),
        FileAvailableMetadataItem.MDTPReportRequestIDMetadataItem("rid"),
        FileAvailableMetadataItem.MDTPReportTypeNameMetadataItem("type"),
        FileAvailableMetadataItem.ReportFileCounterMetadataItem("2"),
        FileAvailableMetadataItem.ReportLastFileMetadataItem("false")
      )
      items.foreach { item =>
        val json = Json.toJson(item)
        val parsed = json.as[FileAvailableMetadataItem]
        parsed shouldBe item
      }
    }
  }
}