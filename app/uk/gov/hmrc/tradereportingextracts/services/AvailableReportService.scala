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
import play.api.mvc.Result
import play.api.mvc.Results.{BadRequest, NotFound}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tradereportingextracts.config.AppConfig
import uk.gov.hmrc.tradereportingextracts.connectors.{CustomsDataStoreConnector, SDESConnector}
import uk.gov.hmrc.tradereportingextracts.models.*
import uk.gov.hmrc.tradereportingextracts.models.audit.{AuditDownloadRequest, ReportRequestDownloadedEvent}
import uk.gov.hmrc.tradereportingextracts.models.sdes.{FileAvailableMetadataItem, FileAvailableResponse}

import java.time.Instant
import java.time.temporal.ChronoUnit.DAYS
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AvailableReportService @Inject() (
  reportRequestService: ReportRequestService,
  sdesConnector: SDESConnector,
  customsDataStoreConnector: CustomsDataStoreConnector,
  auditService: AuditService,
  appConfig: AppConfig
)(implicit
  ec: ExecutionContext
) {
  val logger: Logger = Logger(this.getClass)

  def getAvailableReports(eoriValue: String)(implicit
    hc: HeaderCarrier
  ): Future[AvailableReportResponse] =
    for {
      eoriHistory               <- customsDataStoreConnector.getEoriHistory(eoriValue).map(_.eoriHistory.map(_.eori))
      eoriHistoryWithCurrentEori = if (eoriHistory.contains(eoriValue)) eoriHistory else eoriHistory :+ eoriValue
      reportRequests            <- reportRequestService.getAvailableReportsByHistory(eoriHistoryWithCurrentEori)
      eoris                      = reportRequests.map(_.requesterEORI).distinct
      sdesResponse              <- if (reportRequests.isEmpty) Future.successful(Seq.empty[FileAvailableResponse])
                                   else Future.traverse(eoris)(sdesConnector.fetchAvailableReportFileUrl).map(_.flatten)
      response                  <- if (reportRequests.isEmpty)
                                     Future.successful(
                                       AvailableReportResponse(
                                         availableUserReports = Some(Seq.empty[AvailableUserReportResponse]),
                                         availableThirdPartyReports = Some(Seq.empty[AvailableThirdPartyReportResponse])
                                       )
                                     )
                                   else toAvailableReportResponses(eoriHistoryWithCurrentEori, reportRequests, sdesResponse)
    } yield response

  private def toAvailableReportResponses(
    eoriHistory: Seq[String],
    reportRequests: Seq[ReportRequest],
    sdesResponse: Seq[FileAvailableResponse]
  ): Future[AvailableReportResponse] = {
    /*
    The first group (userRequests) contains requests where at least one reportEORI matches the user's EORI history.
    The second group (thirdPartyRequests) contains the rest.
     */
    val (userRequests, thirdPartyRequests) =
      reportRequests.partition(req => req.reportEORIs.exists(eoriHistory.contains))

    def actionsFor(req: ReportRequest): Seq[AvailableReportAction] =
      sdesResponse
        .filter(_.metadata.exists {
          case FileAvailableMetadataItem.MDTPReportRequestIDMetadataItem(value) => value == req.reportRequestId
          case _                                                                => false
        })
        .map { sdesFile =>
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

    val availableUserReports = userRequests.map { req =>
      AvailableUserReportResponse(
        referenceNumber = req.reportRequestId,
        reportName = req.reportName,
        reportType = req.reportTypeName,
        expiryDate = req.updateDate.plus(appConfig.reportRequestTTLDays, DAYS),
        action = actionsFor(req)
      )
    }

    def toAvailableThirdPartyReportResponse(
      req: ReportRequest,
      companyName: String,
      consent: String
    ): AvailableThirdPartyReportResponse =
      AvailableThirdPartyReportResponse(
        referenceNumber = req.reportRequestId,
        reportName = req.reportName,
        reportType = req.reportTypeName,
        companyName = if (consent == "1") companyName else "Unknown",
        expiryDate = req.updateDate.plus(appConfig.reportRequestTTLDays, DAYS),
        action = actionsFor(req)
      )

    Future
      .traverse(thirdPartyRequests) { req =>
        customsDataStoreConnector
          .getCompanyInformation(req.reportEORIs.head)
          .map(companyInfo => toAvailableThirdPartyReportResponse(req, companyInfo.name, companyInfo.consent))
      }
      .map { availableThirdPartyReports =>
        AvailableReportResponse(Some(availableUserReports), Some(availableThirdPartyReports))
      }
  }

  def getAvailableReportsCount(eoriValue: String): Future[Long] =
    reportRequestService.countAvailableReports(eoriValue).recover { case ex: Exception =>
      logger.error(ex.getMessage, ex)
      0L
    }

  def findByReportRequestId(reportRequestId: String)(implicit hc: HeaderCarrier): Future[Option[ReportRequest]] =
    reportRequestService.get(reportRequestId)

  def processReportDownloadAudit(
    auditRequest: Option[AuditDownloadRequest]
  )(implicit hc: HeaderCarrier): Future[Either[Result, Unit]] =
    auditRequest match {
      case Some(auditRequest) =>
        findByReportRequestId(auditRequest.reportReference).map {
          case Some(report) =>
            report.fileNotifications
              .flatMap(_.find(_.fileName == auditRequest.fileName))
              .fold[Either[Result, Unit]](
                Left(BadRequest(s"File ${auditRequest.fileName} not found in report ${auditRequest.reportReference}"))
              ) { notification =>
                callReportDownloadedAudit(auditRequest, report, notification)
                Right(())
              }
          case None         =>
            Left(NotFound(s"Report with reference ${auditRequest.reportReference} not found"))
        }
      case None               =>
        Future.successful(Left(BadRequest("Missing or invalid request parameters")))
    }

  private def callReportDownloadedAudit(
    auditRequest: AuditDownloadRequest,
    report: ReportRequest,
    notification: FileNotification
  )(implicit hc: HeaderCarrier): Unit =
    auditService.audit(
      ReportRequestDownloadedEvent(
        requestId = auditRequest.reportReference,
        totalReportParts = report.fileNotifications.map(_.size).getOrElse(0),
        fileUrl = auditRequest.fileUrl,
        fileName = auditRequest.fileName,
        fileSizeBytes = notification.fileSize,
        reportSubjectEori = report.requesterEORI,
        reportTypeName = report.reportTypeName.toString,
        requesterEori = report.requesterEORI,
        xCorrelationId = report.correlationId
      )
    )
}
