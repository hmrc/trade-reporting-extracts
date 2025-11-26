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
import play.api.libs.json._

class AuditDownloadRequestSpec extends AnyFreeSpec with Matchers {

  "AuditDownloadRequest JSON format" - {

    "should serialise to JSON correctly" in {
      val req = AuditDownloadRequest(
        reportReference = "RE1",
        fileName = "report.csv",
        fileUrl = "url"
      )

      val json = Json.toJson(req)

      (json \ "reportReference").as[String] shouldBe "RE1"
      (json \ "fileName").as[String]        shouldBe "report.csv"
      (json \ "fileUrl").as[String]         shouldBe "url"
    }

    "should deserialise from JSON correctly" in {
      val json = Json.parse(
        """
          |{
          |  "reportReference": "RE1",
          |  "fileName": "report.csv",
          |  "fileUrl": "url"
          |}
          |""".stripMargin
      )

      val result = json.as[AuditDownloadRequest]

      result.reportReference shouldBe "RE1"
      result.fileName        shouldBe "report.csv"
      result.fileUrl         shouldBe "url"
    }

  }
}
