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
import play.api.Logging
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tradereportingextracts.connectors.{CustomsDataStoreConnector, EmailConnector}
import uk.gov.hmrc.tradereportingextracts.models.*
import uk.gov.hmrc.tradereportingextracts.models.audit.ReportGenerationFailureEvent
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
  customsDataStoreConnector: CustomsDataStoreConnector,
  emailConnector: EmailConnector,
  auditService: AuditService
) extends Logging:

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
    for {
      eoriHistory                       <- customsDataStoreConnector.getEoriHistory(eori).map(_.eoriHistory.map(_.eori))
      eoriHistoryWithCurrentEori         = if (eoriHistory.contains(eori)) eoriHistory else eoriHistory :+ eori
      reportRequests                    <- reportRequestRepository.getRequestedReportsByHistory(eoriHistoryWithCurrentEori)
      /*
      The first group (userRequests) contains requests where at least one reportEORI matches the user's EORI history.
      The second group (thirdPartyRequests) contains the rest.
       */
      (userRequests, thirdPartyRequests) =
        reportRequests.partition(rr => rr.reportEORIs.exists(eoriHistoryWithCurrentEori.contains))
      userReports                        = userRequests.map(toUserReport)
      thirdPartyReportsFuture            = toThirdPartyReports(thirdPartyRequests)
      thirdPartyReports                 <- thirdPartyReportsFuture.map(_.toSeq)
    } yield GetReportRequestsResponse(
      userReports = if (userReports.nonEmpty) Some(userReports) else None,
      thirdPartyReports = if (thirdPartyReports.nonEmpty) Some(thirdPartyReports) else None
    )

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

  private def toThirdPartyReports(
    thirdPartyRequests: Seq[ReportRequest]
  )(implicit ec: ExecutionContext): Future[Seq[ThirdPartyReport]] =
    Future.traverse(thirdPartyRequests) { req =>
      customsDataStoreConnector
        .getCompanyInformation(req.reportEORIs.head)
        .map(companyInfo => toThirdPartyReport(req, companyInfo.name, companyInfo.consent))
    }

  private def toThirdPartyReport(req: ReportRequest, companyName: String, consent: String): ThirdPartyReport =
    ThirdPartyReport(
      referenceNumber = req.reportRequestId,
      reportName = req.reportName,
      requestedDate = req.createDate,
      reportType = req.reportTypeName,
      companyName = if (consent == "1") companyName else "Unknown",
      reportStatus = determineReportStatus(req),
      reportStartDate = req.reportStart,
      reportEndDate = req.reportEnd
    )

  def getAvailableReports(eori: String)(using ec: ExecutionContext): Future[Seq[ReportRequest]] =
    reportRequestRepository.getAvailableReports(eori)

  def getAvailableReportsByHistory(eoriHistory: Seq[String])(using ec: ExecutionContext): Future[Seq[ReportRequest]] =
    reportRequestRepository.getAvailableReportsByHistory(eoriHistory)

  def countAvailableReports(eori: String)(using ec: ExecutionContext): Future[Int] =
    reportRequestRepository.countAvailableReports(eori)

  def processReportStatus(
    headers: Headers,
    eisReportStatusRequest: EisReportStatusRequest
  )(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Unit] = {
    val correlationId = headers.get(XCorrelationID.toString).getOrElse("unknown-correlation-id")
    reportRequestRepository.findByCorrelationId(correlationId).flatMap {
      case Some(req) =>
        val maskedId             = req.reportRequestId.replaceFirst("^.{5}", "XXXXX")
        val updatedNotifications = req.notifications :+ eisReportStatusRequest
        val updatedReportRequest = req.copy(notifications = updatedNotifications)
        (
          req.notifications.exists(_.statusType == EisReportStatusRequest.StatusType.ERROR),
          eisReportStatusRequest.statusType
        ) match {
          case (false, StatusType.ERROR) =>
            for {
              _ <- reportRequestRepository.update(updatedReportRequest)
              _  = auditService.audit(
                     ReportGenerationFailureEvent(
                       xCorrelationId = updatedReportRequest.correlationId,
                       statusNotificationCode = eisReportStatusRequest.statusCode
                     )
                   )
              _  = req.userEmail match {
                     case Some(userEmail) =>
                       emailConnector.sendEmailRequest(
                         templateId = EmailTemplate.ReportFailed.id,
                         email = userEmail.decryptedValue,
                         params = Map("reportRequestId" -> maskedId)
                       )
                     case None            =>
                       logger.info(s"No userEmail found for reportRequestId: $maskedId")
                       Future.successful(())
                   }
              _  = Future.sequence(
                     req.recipientEmails.map { email =>
                       emailConnector.sendEmailRequest(
                         templateId = EmailTemplate.ReportAvailableNonVerified.id,
                         email = email.decryptedValue,
                         params = Map("reportRequestId" -> maskedId)
                       )
                     }
                   )
            } yield ()
          case _                         =>
            reportRequestRepository.update(updatedReportRequest).map(_ => ())
        }
      case None      =>
        Future.successful(())
    }
  }

  def determineReportStatus(reportRequest: ReportRequest): ReportStatus =
    reportRequest.notifications match
      case notifications
          if notifications
            .exists(n => n.statusType == StatusType.ERROR && n.statusCode == StatusCode.FILENOREC.toString) =>
        ReportStatus.NO_DATA_AVAILABLE
      case notifications if notifications.exists(_.statusType == StatusType.ERROR) => ReportStatus.ERROR
      case _                                                                       => ReportStatus.IN_PROGRESS

  def countReportSubmissionsForEoriOnDate(eori: String, limit: Int, date: LocalDate = LocalDate.now())(implicit
    ec: ExecutionContext
  ): Future[Boolean] =
    reportRequestRepository.countReportSubmissionsForEoriOnDate(eori, date).map(_ >= limit)
