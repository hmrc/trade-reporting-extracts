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

package uk.gov.hmrc.tradereportingextracts.models

import play.api.libs.json.{Format, Json}

case class Report(userid: Long,
                  reportId: String,
                  templateId: String,
                  recipientEmails: Array[String],
                  reportEORIs: Array[String],
                  reportType: String,
                  reportStart: String,
                  reportEnd: String,
                  status: String,
                  statusDetails: String
                 ) {
  override def equals(obj: Any): Boolean = obj match {
    case that: Report =>
      this.userid == that.userid &&
        this.reportId == that.reportId &&
        this.templateId == that.templateId
    case _ => false
  }
}

object Report:
  given mongoFormat: Format[Report] = Json.format[Report]