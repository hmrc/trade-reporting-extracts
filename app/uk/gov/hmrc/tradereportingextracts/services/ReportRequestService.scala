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
import uk.gov.hmrc.tradereportingextracts.models.Report
import uk.gov.hmrc.tradereportingextracts.repositories.ReportRequestRepository

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ReportRequestService @Inject() (
  reportRequestRepository: ReportRequestRepository
)(using ec: ExecutionContext)
    extends Logging:

  def create(report: Report)(using ec: ExecutionContext): Future[Boolean] =
    // Business Logic
    reportRequestRepository.insertReportRequest(report)

  def get(reportId: String)(using ec: ExecutionContext): Future[Option[Report]] =
    reportRequestRepository.findByReportId(reportId)

  def update(report: Report): Future[Boolean] =
    reportRequestRepository.updateByReportId(report)

  def delete(reportId: String): Future[Boolean] =
    reportRequestRepository.deleteByReportId(reportId)
