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

import play.api.libs.json.*

case class ReportFilesPartsMetadataItem(
  key: ReportFilesPartsMetadataItem.Key.Value,
  value: String
)

object ReportFilesPartsMetadataItem {
  implicit lazy val reportFilesPartsMetadataItemJsonFormat: Format[ReportFilesPartsMetadataItem] =
    Json.format[ReportFilesPartsMetadataItem]

  object Key extends Enumeration {
    val ReportFilesParts = Value("Report-files-parts")

    type Key = Value
    implicit lazy val KeyJsonFormat: Format[Value] =
      Format(Reads.enumNameReads(this), Writes.enumNameWrites[ReportFilesPartsMetadataItem.Key.type])
  }
}
