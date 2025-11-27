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

import play.api.Logging
import play.api.http.Status
import play.api.http.Status.{BAD_REQUEST, CREATED, NOT_FOUND}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tradereportingextracts.connectors.EmailConnector
import uk.gov.hmrc.tradereportingextracts.models.audit.ReportAvailableEvent
import uk.gov.hmrc.tradereportingextracts.models.sdes.{FileNotificationMetadata, FileNotificationResponse}
import uk.gov.hmrc.tradereportingextracts.models.{EmailTemplate, FileNotification as TreFileNotification, ReportTypeName}

import java.time.Instant
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class FileNotificationService @Inject() (
  reportRequestService: ReportRequestService,
  emailConnector: EmailConnector,
  auditService: AuditService
)(implicit
  ec: ExecutionContext
) extends Logging {

  def processFileNotification(fileNotification: FileNotificationResponse): Future[(Int, String)] = {
    val maybeReportRequestId = fileNotification.metadata.collectFirst {
      case FileNotificationMetadata.MDTPReportRequestIDMetadataItem(value) => value
    }

    implicit val hc: HeaderCarrier = HeaderCarrier()

    maybeReportRequestId match {
      case Some(reportRequestId) =>
        reportRequestService.get(reportRequestId).flatMap {
          case Some(reportRequest) =>
            val updatedFileNotifications = reportRequest.fileNotifications match {
              case Some(existing) => Some(existing :+ convertToTreFileNotification(fileNotification))
              case None           => Some(Seq(convertToTreFileNotification(fileNotification)))
            }
            val updatedReportRequest     = reportRequest
              .copy(fileNotifications = updatedFileNotifications, updateDate = Instant.now())
            val maskedId                 = updatedReportRequest.reportRequestId.replaceFirst("^.{5}", "XXXXX")
            if (updatedReportRequest.isReportStatusComplete) {
              for {
                _ <- reportRequestService.update(updatedReportRequest)
                _  = auditService.audit(ReportAvailableEvent(xCorrelationId = updatedReportRequest.correlationId))
                _ <- updatedReportRequest.userEmail match {
                       case Some(userEmail) =>
                         emailConnector.sendEmailRequest(
                           templateId = EmailTemplate.ReportAvailable.id,
                           email = userEmail.decryptedValue,
                           params = Map("reportRequestId" -> maskedId)
                         )
                       case None            =>
                         logger.info(s"No userEmail found for reportRequestId: $maskedId")
                         Future.successful(())
                     }
                _ <- Future.sequence(
                       updatedReportRequest.recipientEmails.map { email =>
                         emailConnector.sendEmailRequest(
                           templateId = EmailTemplate.ReportAvailableNonVerified.id,
                           email = email.decryptedValue,
                           params = Map("reportRequestId" -> maskedId)
                         )
                       }
                     )
              } yield (CREATED, "Created")
            } else {
              reportRequestService.update(updatedReportRequest).map(_ => (CREATED, "Created"))
            }

          case None =>
            Future.successful((NOT_FOUND, s"ReportRequest not found for reportRequestId: $reportRequestId"))
        }
      case None                  =>
        Future.successful((BAD_REQUEST, "report-requestID not found in FileNotification metadata"))
    }
  }

  private def convertToTreFileNotification(sdes: FileNotificationResponse): TreFileNotification = {
    def getValue[A <: FileNotificationMetadata](pf: PartialFunction[FileNotificationMetadata, String]): String =
      sdes.metadata.collectFirst(pf).getOrElse("")

    TreFileNotification(
      fileName = sdes.fileName,
      fileSize = sdes.fileSize,
      retentionDays =
        getValue { case FileNotificationMetadata.RetentionDaysMetadataItem(v: String) => v }.toIntOption.getOrElse(0),
      fileType = getValue { case FileNotificationMetadata.FileTypeMetadataItem(v: String) => v },
      mDTPReportXCorrelationID = getValue {
        case FileNotificationMetadata.MDTPReportXCorrelationIDMetadataItem(v: String) => v
      },
      mDTPReportRequestID = getValue { case FileNotificationMetadata.MDTPReportRequestIDMetadataItem(v: String) => v },
      mDTPReportTypeName = getValue { case FileNotificationMetadata.MDTPReportTypeNameMetadataItem(v: String) => v },
      reportFilesParts = getValue { case FileNotificationMetadata.ReportFilesPartsMetadataItem(v: String) => v },
      reportLastFile = getValue { case FileNotificationMetadata.ReportLastFileMetadataItem(v: String) => v },
      fileCreationTimestamp = getValue { case FileNotificationMetadata.FileCreationTimestampMetadataItem(v: String) =>
        v
      }
    )
  }
}
