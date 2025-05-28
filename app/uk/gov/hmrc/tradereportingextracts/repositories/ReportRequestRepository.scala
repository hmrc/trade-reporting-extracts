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

package uk.gov.hmrc.tradereportingextracts.repositories

import org.mongodb.scala.*
import org.mongodb.scala.model.{Filters, IndexModel, IndexOptions, Indexes}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.tradereportingextracts.config.AppConfig
import uk.gov.hmrc.tradereportingextracts.models.ReportRequest

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ReportRequestRepository @Inject() (appConfig: AppConfig, mongoComponent: MongoComponent)(implicit
  ec: ExecutionContext
) extends PlayMongoRepository[ReportRequest](
      mongoComponent,
      collectionName = "tre-report-request",
      domainFormat = ReportRequest.format,
      indexes = Seq(
        IndexModel(Indexes.ascending("reportRequestId"), IndexOptions().name("reportRequestId-index").unique(true))
      ),
      replaceIndexes = true
    ):

  def insert(reportRequest: ReportRequest)(implicit ec: ExecutionContext): Future[Boolean] =
    collection
      .insertOne(reportRequest)
      .head()
      .map(_.wasAcknowledged())

  def findByReportRequestId(reportRequestId: String)(implicit ec: ExecutionContext): Future[Option[ReportRequest]] =
    collection
      .find(Filters.equal("reportRequestId", reportRequestId))
      .headOption()

  def update(reportRequest: ReportRequest)(implicit ec: ExecutionContext): Future[Boolean] =
    collection
      .replaceOne(Filters.equal("reportRequestId", reportRequest.reportRequestId), reportRequest)
      .toFuture()
      .map(_.wasAcknowledged())

  def delete(reportRequest: ReportRequest)(implicit ec: ExecutionContext): Future[Boolean] =
    collection
      .deleteOne(Filters.equal("reportRequestId", reportRequest.reportRequestId))
      .toFuture()
      .map(_.wasAcknowledged())

  def findByRequesterEORI(requesterEORI: String)(using ec: ExecutionContext): Future[Seq[ReportRequest]] =
    collection
      .find(Filters.equal("requesterEORI", requesterEORI))
      .toFuture()

  def getAvailableReports(eori: String)(using ec: ExecutionContext): Future[Seq[ReportRequest]] =
    collection
      .find(
        Filters.and(
          Filters.equal("requesterEORI", eori),
          Filters.elemMatch("fileNotifications", Filters.equal("reportFilesParts", appConfig.reportFilesPart))
        )
      )
      .toFuture()

  def countAvailableReports(eori: String): Future[Long] =
    collection
      .countDocuments(
        Filters.and(
          Filters.equal("requesterEORI", eori),
          Filters.elemMatch("fileNotifications", Filters.equal("reportFilesParts", appConfig.reportFilesPart))
        )
      )
      .toFuture()
