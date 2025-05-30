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

package uk.gov.hmrc.tradereportingextracts.services

import play.api.http.Status
import play.api.http.Status.{BAD_REQUEST, CREATED, NOT_FOUND}
import uk.gov.hmrc.tradereportingextracts.models.sdes.{FileNotification, FileNotificationMetadata}
import uk.gov.hmrc.tradereportingextracts.models.{FileNotification as TreFileNotification, FileType, ReportTypeName}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class FileNotificationService @Inject() (reportRequestService: ReportRequestService)(implicit ec: ExecutionContext) {

  def processFileNotification(fileNotification: FileNotification): Future[(Int, String)] = {
    val maybeReportRequestId = fileNotification.metadata.collectFirst {
      case FileNotificationMetadata.MDTPReportRequestIDMetadataItem(value) => value
    }

    maybeReportRequestId match {
      case Some(reportRequestId) =>
        reportRequestService.get(reportRequestId).flatMap {
          case Some(reportRequest) =>
            val updatedFileNotifications = reportRequest.fileNotifications match {
              case Some(existing) => Some(existing :+ convertToTreFileNotification(fileNotification))
              case None           => Some(Seq(convertToTreFileNotification(fileNotification)))
            }
            val updatedReportRequest     = reportRequest.copy(fileNotifications = updatedFileNotifications)
            reportRequestService.update(updatedReportRequest).map(_ => (CREATED, "Created"))
          case None                =>
            Future.successful((NOT_FOUND, s"ReportRequest not found for reportRequestId: $reportRequestId"))
        }
      case None                  =>
        Future.successful((BAD_REQUEST, "report-requestID not found in FileNotification metadata"))
    }
  }

  private def convertToTreFileNotification(sdes: FileNotification): TreFileNotification = {
    def getValue[A <: FileNotificationMetadata](pf: PartialFunction[FileNotificationMetadata, String]): String =
      sdes.metadata.collectFirst(pf).getOrElse("")

    TreFileNotification(
      fileName = sdes.fileName,
      fileSize = sdes.fileSize,
      retentionDays =
        getValue { case FileNotificationMetadata.RetentionDaysMetadataItem(v: String) => v }.toIntOption.getOrElse(0),
      fileType = FileType.valueOf(
        getValue { case FileNotificationMetadata.FileTypeMetadataItem(v: String) => v }
      ),
      mDTPReportXCorrelationID = getValue {
        case FileNotificationMetadata.MDTPReportXCorrelationIDMetadataItem(v: String) => v
      },
      mDTPReportRequestID = getValue { case FileNotificationMetadata.MDTPReportRequestIDMetadataItem(v: String) => v },
      mDTPReportTypeName = ReportTypeName.valueOf(
        getValue { case FileNotificationMetadata.MDTPReportTypeNameMetadataItem(v: String) => v }
      ),
      reportFilesParts = getValue { case FileNotificationMetadata.ReportFilesPartsMetadataItem(v: String) => v }
    )
  }
}
