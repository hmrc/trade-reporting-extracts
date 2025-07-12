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
import uk.gov.hmrc.tradereportingextracts.config.AppConfig
import uk.gov.hmrc.tradereportingextracts.connectors.SDESConnector
import uk.gov.hmrc.tradereportingextracts.models.sdes.{FileAvailableMetadataItem, FileAvailableResponse}
import uk.gov.hmrc.tradereportingextracts.models.*

import java.time.Instant
import java.time.temporal.ChronoUnit.DAYS
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AvailableReportService @Inject() (
  reportRequestService: ReportRequestService,
  sdesConnector: SDESConnector,
  appConfig: AppConfig
)(implicit
  ec: ExecutionContext
) {
  val logger: Logger = Logger(this.getClass)

  def getAvailableReports(eoriValue: String)(implicit
    hc: HeaderCarrier
  ): Future[AvailableReportResponse] =
    for {
      reportRequests <- reportRequestService.getAvailableReports(eoriValue)
      sdesResponse   <- if (reportRequests.isEmpty) Future.successful(Seq.empty[FileAvailableResponse])
                        else sdesConnector.fetchAvailableReportFileUrl(eoriValue)
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
      AvailableUserReportResponse(
        referenceNumber = req.reportRequestId,
        reportName = req.reportName,
        reportType = req.reportTypeName,
        expiryDate = req.updateDate.plus(appConfig.reportRequestTTLDays, DAYS),
        action = toAvailableReportActions(
          sdesResponse.filter(_.metadata.exists {
            case FileAvailableMetadataItem.MDTPReportRequestIDMetadataItem(value) =>
              value == req.reportRequestId
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
