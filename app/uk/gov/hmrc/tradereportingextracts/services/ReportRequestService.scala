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

import uk.gov.hmrc.tradereportingextracts.models.ReportRequest
import uk.gov.hmrc.tradereportingextracts.repositories.ReportRequestRepository

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ReportRequestService @Inject() (reportRequestRepository: ReportRequestRepository):

  def create(reportRequest: ReportRequest)(implicit ec: ExecutionContext): Future[Boolean] =
    reportRequestRepository.insert(reportRequest)

  def get(reportRequestId: String)(implicit ec: ExecutionContext): Future[Option[ReportRequest]] =
    reportRequestRepository.findByReportRequestId(reportRequestId)

  def update(reportRequest: ReportRequest): Future[Boolean] =
    reportRequestRepository.update(reportRequest)

  def delete(reportRequest: ReportRequest): Future[Boolean] =
    reportRequestRepository.delete(reportRequest)

  def getByRequesterEORI(requesterEORI: String)(using ec: ExecutionContext): Future[Seq[ReportRequest]] =
    reportRequestRepository.findByRequesterEORI(requesterEORI)
