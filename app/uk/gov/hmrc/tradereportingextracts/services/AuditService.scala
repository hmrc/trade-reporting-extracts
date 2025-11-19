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

import play.api.libs.json.OWrites
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.tradereportingextracts.models.audit.{AuditEvent, ReportDetail, ReportRequestSubmittedEvent}
import uk.gov.hmrc.tradereportingextracts.models.{ReportRequest, ReportSubmissionStatus, StatusCode}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AuditService @Inject() (
  auditConnector: AuditConnector
)(using ExecutionContext) {

  def audit[A <: AuditEvent](event: A)(using OWrites[A], HeaderCarrier): Unit =
    auditConnector.sendExplicitAudit(event.auditType, event)

  def auditReportRequestSubmitted(
    reportRequests: Seq[ReportRequest],
    eoriRoles: Set[String]
  )(using HeaderCarrier): Future[Unit] =
    reportRequests.headOption match {
      case Some(baseRequest) =>
        val reportDetails: Seq[ReportDetail] = reportRequests.map { request =>
          val notif = request.notifications.headOption
          ReportDetail(
            requestId = request.reportRequestId,
            xCorrelationId = request.correlationId,
            reportTypeName = request.reportTypeName.toString,
            outcomeIsSuccessful = notif.exists(_.statusCode == StatusCode.INITIATED.toString)
          )
        }
        val submissionStatus                 =
          if (reportDetails.forall(_.outcomeIsSuccessful)) ReportSubmissionStatus.Complete.value
          else ReportSubmissionStatus.Incomplete.value
        val event                            = ReportRequestSubmittedEvent(
          submissionStatus = submissionStatus,
          numberOfReports = reportDetails.size,
          requesterEori = baseRequest.requesterEORI,
          reportSubjectEori = baseRequest.reportEORIs.mkString(", "),
          reportSubjectRole = eoriRoles.mkString(", "),
          reportAlias = baseRequest.reportName,
          reportStart = baseRequest.reportStart,
          reportEnd = baseRequest.reportEnd,
          submittedAt = baseRequest.createDate,
          reports = reportDetails
        )

        Future(audit(event))

      case None =>
        Future.unit
    }

}
