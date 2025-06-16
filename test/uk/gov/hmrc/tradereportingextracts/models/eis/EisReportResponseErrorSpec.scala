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

class EisReportResponseErrorSpec extends AnyFreeSpec with Matchers {

  "EisReportResponseError" - {

    "must serialize and deserialize correctly with sourceFaultDetail" in {
      val sourceFaultDetail = EisReportResponseErrorDetailSourceFaultDetail(
        detail = List("Some fault", "Another fault"),
        restFault = Some(Json.obj("error" -> "REST error")),
        soapFault = Some(Json.obj("error" -> "SOAP error"))
      )
      val detail            = EisReportResponseErrorDetail(
        correlationId = "corr-123",
        errorCode = Some("ERR_CODE"),
        errorMessage = Some("Some error message"),
        source = Some("EIS"),
        sourceFaultDetail = Some(sourceFaultDetail),
        timestamp = "2024-06-01T12:00:00Z"
      )
      val error             = EisReportResponseError(detail)
      val json              = Json.toJson(error)

      json mustBe Json.obj(
        "errorDetail" -> Json.obj(
          "correlationId"     -> "corr-123",
          "errorCode"         -> "ERR_CODE",
          "errorMessage"      -> "Some error message",
          "source"            -> "EIS",
          "sourceFaultDetail" -> Json.obj(
            "detail"    -> Json.arr("Some fault", "Another fault"),
            "restFault" -> Json.obj("error" -> "REST error"),
            "soapFault" -> Json.obj("error" -> "SOAP error")
          ),
          "timestamp"         -> "2024-06-01T12:00:00Z"
        )
      )
      json.as[EisReportResponseError] mustBe error
    }
  }
}
