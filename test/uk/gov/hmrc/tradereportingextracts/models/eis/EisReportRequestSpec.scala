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

package uk.gov.hmrc.tradereportingextracts.models.eis

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.*

class EisReportRequestSpec extends AnyFreeSpec with Matchers {

  "EisReportRequest" - {

    "must serialize and deserialize" in {
      val request = EisReportRequest(
        endDate = "2024-06-30",
        eori = List("GB123456789000"),
        eoriRole = EisReportRequest.EoriRole.TRADER,
        reportTypeName = EisReportRequest.ReportTypeName.IMPORTSITEMREPORT,
        requestID = "req-123",
        requestTimestamp = "2024-06-10T12:00:00Z",
        requesterEori = "GB123456789000",
        startDate = "2024-06-01"
      )
      val json    = Json.toJson(request)

      json.as[EisReportRequest] mustBe request
    }
  }
}
