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

import play.api.mvc.Headers
import uk.gov.hmrc.tradereportingextracts.connectors.CustomsDataStoreConnector
import uk.gov.hmrc.tradereportingextracts.models.*
import uk.gov.hmrc.tradereportingextracts.models.eis.EisReportStatusHeaders.XCorrelationID
import uk.gov.hmrc.tradereportingextracts.models.eis.EisReportStatusRequest
import uk.gov.hmrc.tradereportingextracts.models.eis.EisReportStatusRequest.StatusType
import uk.gov.hmrc.tradereportingextracts.repositories.ReportRequestRepository

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ReportRequestService @Inject() (
  reportRequestRepository: ReportRequestRepository,
  customsDataStoreConnector: CustomsDataStoreConnector
):

  def create(reportRequest: ReportRequest)(implicit ec: ExecutionContext): Future[Boolean] =
    reportRequestRepository.insert(reportRequest)

  def createAll(reportRequests: Seq[ReportRequest])(implicit ec: ExecutionContext): Future[Boolean] =
    reportRequestRepository.insertAll(reportRequests)

  def get(reportRequestId: String)(implicit ec: ExecutionContext): Future[Option[ReportRequest]] =
    reportRequestRepository.findByReportRequestId(reportRequestId)

  def update(reportRequest: ReportRequest)(implicit ec: ExecutionContext): Future[Boolean] =
    reportRequestRepository.update(reportRequest)

  def delete(reportRequest: ReportRequest)(implicit ec: ExecutionContext): Future[Boolean] =
    reportRequestRepository.delete(reportRequest)

  def getByRequesterEORI(requesterEORI: String)(using ec: ExecutionContext): Future[Seq[ReportRequest]] =
    reportRequestRepository.findByRequesterEORI(requesterEORI)

  def getReportRequestsForUser(eori: String)(using ec: ExecutionContext): Future[GetReportRequestsResponse] =
    reportRequestRepository.findByRequesterEORI(eori).flatMap { reportRequests =>
      val (userRequests, thirdPartyRequests) =
        reportRequests.partition(r => r.requesterEORI == eori && r.reportEORIs.contains(eori))

      val userReports = userRequests.map(toUserReport)

      val thirdPartyReportsFutures: Seq[Future[ThirdPartyReport]] = thirdPartyRequests.map { req =>
        customsDataStoreConnector
          .getCompanyInformation(req.requesterEORI)
          .map { companyInfo =>
            toThirdPartyReport(req, companyInfo.name)
          }
          .recover { case _ =>
            toThirdPartyReport(req, s"Unknown company (${req.requesterEORI})")
          }
      }

      Future.sequence(thirdPartyReportsFutures).map { thirdPartyReports =>
        GetReportRequestsResponse(
          userReports = if (userReports.nonEmpty) Some(userReports) else None,
          thirdPartyReports = if (thirdPartyReports.nonEmpty) Some(thirdPartyReports) else None
        )
      }
    }

  private def toUserReport(req: ReportRequest): UserReport =
    UserReport(
      referenceNumber = req.reportRequestId,
      reportName = req.reportName,
      requestedDate = req.createDate,
      reportType = req.reportTypeName,
      reportStatus = determineReportStatus(req),
      reportStartDate = req.reportStart,
      reportEndDate = req.reportEnd
    )

  private def toThirdPartyReport(req: ReportRequest, companyName: String): ThirdPartyReport =
    ThirdPartyReport(
      referenceNumber = req.reportRequestId,
      reportName = req.reportName,
      requestedDate = req.createDate,
      reportType = req.reportTypeName,
      companyName = companyName,
      reportStatus = determineReportStatus(req),
      reportStartDate = req.reportStart,
      reportEndDate = req.reportEnd
    )

  def getAvailableReports(eori: String)(using ec: ExecutionContext): Future[Seq[ReportRequest]] =
    reportRequestRepository.getAvailableReports(eori)

  def countAvailableReports(eori: String)(using ec: ExecutionContext): Future[Long] =
    reportRequestRepository.countAvailableReports(eori)

  def processReportStatus(headers: Headers, eisReportStatusRequest: EisReportStatusRequest)(using
    ec: ExecutionContext
  ): Unit = {
    val correlationId = headers.get(XCorrelationID.toString).getOrElse("unknown-correlation-id")
    val reportRequest = reportRequestRepository.findByCorrelationId(correlationId)
    reportRequest.map {
      case Some(req) =>
        val updatedNotifications = req.notifications :+ eisReportStatusRequest
        val updatedReportRequest = req.copy(notifications = updatedNotifications)
        reportRequestRepository.update(updatedReportRequest)
      case None      =>
    }

  }

  def determineReportStatus(reportRequest: ReportRequest): ReportStatus =
    (reportRequest.isReportStatusComplete(), reportRequest.notifications) match
      case (true, _)                                                                    => ReportStatus.COMPLETE
      case (_, notifications)
          if notifications
            .exists(n => n.statusType == StatusType.ERROR && n.statusCode == StatusCode.FILENOREC.toString) =>
        ReportStatus.NO_DATA_AVAILABLE
      case (_, notifications) if notifications.exists(_.statusType == StatusType.ERROR) => ReportStatus.ERROR
      case _                                                                            => ReportStatus.IN_PROGRESS

  def countReportSubmissionsForEoriOnDate(eori: String, limit: Int, date: LocalDate = LocalDate.now())(implicit
    ec: ExecutionContext
  ): Future[Boolean] =
    reportRequestRepository.countReportSubmissionsForEoriOnDate(eori, date).map(_ >= limit)
