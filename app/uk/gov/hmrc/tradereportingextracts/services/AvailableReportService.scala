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

import play.api.Logger
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tradereportingextracts.connectors.SDESConnector
import uk.gov.hmrc.tradereportingextracts.models.{AvailableReportAction, AvailableReportResponse, AvailableUserReportResponse, FileType, ReportRequest}
import uk.gov.hmrc.tradereportingextracts.models.sdes.{FileAvailableMetadataItem, FileAvailableResponse}

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}
import javax.inject.Inject

class AvailableReportService @Inject() (reportRequestService: ReportRequestService, sdesConnector: SDESConnector)(
  implicit ec: ExecutionContext
) {
  val logger: Logger = Logger(this.getClass)

  def getAvailableReports(eoriValue: String)(implicit
    hc: HeaderCarrier
  ): Future[AvailableReportResponse] =
    for {
      sdesResponse   <- sdesConnector.fetchAvailableReportFileUrl(eoriValue)
      reportRequests <- reportRequestService.getAvailableReports(eoriValue)
    } yield
      if (reportRequests.isEmpty) {
        logger.warn(s"No available reports found for EORI: $eoriValue")
        AvailableReportResponse(
          availableUserReports = Some(Seq.empty[AvailableUserReportResponse]),
          availableThirdPartyReports = None
        )
      } else {
        toAvailableReportResponses(reportRequests, sdesResponse)
      }

  private def toAvailableReportActions(
    sdesResponse: Seq[FileAvailableResponse]
  ): Seq[AvailableReportAction] =
    sdesResponse.map { sdesFile =>
      AvailableReportAction(
        fileURL = sdesFile.downloadURL,
        size = sdesFile.fileSize,
        fileType = sdesFile.metadata
          .collectFirst { case FileAvailableMetadataItem.FileTypeMetadataItem(value) =>
            FileType.valueOf(value)
          }
          .getOrElse(FileType.CSV),
        fileName = sdesFile.filename
      )
    }

  private def toAvailableReportResponses(
    reportRequests: Seq[ReportRequest],
    sdesResponse: Seq[FileAvailableResponse]
  ): AvailableReportResponse = {
    val availableUserReports = reportRequests.map { req =>
      val fileNotifyOpt = req.fileNotifications.flatMap(_.headOption)
      AvailableUserReportResponse(
        referenceNumber = req.reportRequestId,
        reportName = req.reportName,
        reportType = req.reportTypeName,
        expiryDate = req.linkAvailableTime
          .getOrElse(java.time.Instant.EPOCH)
          .plusSeconds(fileNotifyOpt.map(_.retentionDays.toLong).getOrElse(0L) * 86400),
        action = toAvailableReportActions(
          sdesResponse.filter(_.metadata.exists {
            case FileAvailableMetadataItem.MDTPReportRequestIDMetadataItem(value) =>
              fileNotifyOpt.exists(_.mDTPReportRequestID == value)
            case _                                                                => false
          })
        )
      )
    }
    AvailableReportResponse(Some(availableUserReports), None)
  }

  def getAvailableReportsCount(eoriValue: String): Future[Long] =
    reportRequestService.countAvailableReports(eoriValue).recover { case ex: Exception =>
      logger.error(ex.getMessage, ex)
      0L
    }
}
