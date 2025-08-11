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

class EisReportStatusResponseErrorSpec extends AnyFreeSpec with Matchers {

  "EisReportStatusResponseError" - {

    "must serialize and deserialize correctly with ErrorDetailSourceFaultDetail" in {
      val sourceFaultDetail = EisReportStatusResponseErrorDetailSourceFaultDetail(
        detail = List("Status fault", "Another status fault"),
        restFault = Some(Json.obj("error" -> "REST status error")),
        soapFault = Some(Json.obj("error" -> "SOAP status error"))
      )
      val errorDetail       = EisReportStatusResponseErrorDetail(
        correlationId = "stat-corr-123",
        errorCode = Some("STAT_ERR_CODE"),
        errorMessage = Some("Status error message"),
        source = Some("EIS"),
        sourceFaultDetail = Some(sourceFaultDetail),
        timestamp = "2024-06-10T12:00:00Z"
      )
      val error             = EisReportStatusResponseError(errorDetail)
      val json              = Json.toJson(error)

      json mustBe Json.obj(
        "errorDetail" -> Json.obj(
          "correlationId"     -> "stat-corr-123",
          "errorCode"         -> "STAT_ERR_CODE",
          "errorMessage"      -> "Status error message",
          "source"            -> "EIS",
          "sourceFaultDetail" -> Json.obj(
            "detail"    -> Json.arr("Status fault", "Another status fault"),
            "restFault" -> Json.obj("error" -> "REST status error"),
            "soapFault" -> Json.obj("error" -> "SOAP status error")
          ),
          "timestamp"         -> "2024-06-10T12:00:00Z"
        )
      )

      json.as[EisReportStatusResponseError] mustBe error
    }
  }
}
