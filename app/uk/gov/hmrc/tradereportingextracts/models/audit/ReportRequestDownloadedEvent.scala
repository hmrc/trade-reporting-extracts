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

import play.api.libs.functional.syntax.*
import play.api.libs.json.{OWrites, __}

final case class ReportRequestDownloadedEvent(
  requestId: String,
  totalReportParts: Int,
  fileUrl: String,
  fileName: String,
  fileSizeBytes: Long,
  reportSubjectEori: String,
  reportTypeName: String,
  requesterEori: String,
  xCorrelationId: String
) extends AuditEvent {
  override val auditType: String = "ReportRequestDownloaded"
}

object ReportRequestDownloadedEvent {
  given OWrites[ReportRequestDownloadedEvent] = (
    (__ \ "requestId").write[String] and
      (__ \ "totalReportParts").write[Int] and
      (__ \ "fileUrl").write[String] and
      (__ \ "fileName").write[String] and
      (__ \ "fileSizeBytes").write[Long] and
      (__ \ "reportSubjectEori").write[String] and
      (__ \ "reportTypeName").write[String] and
      (__ \ "requesterEori").write[String] and
      (__ \ "xCorrelationId").write[String]
  )(report =>
    (
      report.requestId,
      report.totalReportParts,
      report.fileUrl,
      report.fileName,
      report.fileSizeBytes,
      report.reportSubjectEori,
      report.reportTypeName,
      report.requesterEori,
      report.xCorrelationId
    )
  )
}
