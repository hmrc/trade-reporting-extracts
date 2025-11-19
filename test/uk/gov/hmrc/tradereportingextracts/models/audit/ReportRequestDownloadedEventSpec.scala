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

package uk.gov.hmrc.tradereportingextracts.models.audit

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.Json

class ReportRequestDownloadedEventSpec extends AnyFreeSpec with Matchers {

  "ReportRequestDownloadedEvent" - {

    "should serialize to JSON correctly" in {
      val audit = ReportRequestDownloadedEvent(
        requestId = "req-1",
        totalReportParts = 2,
        fileUrl = "http://example.com/file.csv",
        fileName = "file.csv",
        fileSizeBytes = 1234L,
        reportSubjectEori = "GB123456789000",
        reportTypeName = "IMPORTS_ITEM_REPORT",
        requesterEori = "GB987654321000",
        xCorrelationId = "CORR123"
      )

      val expectedJson = Json.parse("""
            |{
            |  "requestId": "req-1",
            |  "totalReportParts": 2,
            |  "fileUrl": "http://example.com/file.csv",
            |  "fileName": "file.csv",
            |  "fileSizeBytes": 1234,
            |  "reportSubjectEori": "GB123456789000",
            |  "reportTypeName": "IMPORTS_ITEM_REPORT",
            |  "requesterEori": "GB987654321000",
            |  "xCorrelationId": "CORR123"
            |}
            """.stripMargin)

      Json.toJson(audit) shouldBe expectedJson
    }

    "should have correct auditType" in {
      val audit = ReportRequestDownloadedEvent(
        requestId = "id",
        totalReportParts = 1,
        fileUrl = "url",
        fileName = "name",
        fileSizeBytes = 100L,
        reportSubjectEori = "eori1",
        reportTypeName = "type",
        requesterEori = "eori2",
        xCorrelationId = "CORR123"
      )
      audit.auditType shouldBe "ReportRequestDownloaded"
    }
  }

}
