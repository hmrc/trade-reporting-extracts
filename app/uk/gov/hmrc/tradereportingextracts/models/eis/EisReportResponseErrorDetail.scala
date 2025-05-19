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

import play.api.libs.json.*

case class EisReportResponseErrorDetail(
  correlationId: String,
  errorCode: Option[String],
  errorMessage: Option[String],
  source: Option[String],
  sourceFaultDetail: Option[EisReportResponseErrorDetailSourceFaultDetail],
  timestamp: String
)

object EisReportResponseErrorDetail {
  implicit lazy val eisReportResponseErrorDetailJsonFormat: Format[EisReportResponseErrorDetail] =
    Json.format[EisReportResponseErrorDetail]
}
