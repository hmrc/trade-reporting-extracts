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

import javax.inject.{Inject, Singleton}
import org.mongodb.scala.model.{Filters, IndexModel, IndexOptions, Indexes}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.tradereportingextracts.config.AppConfig
import uk.gov.hmrc.tradereportingextracts.models.ReportRequest
import scala.concurrent.{ExecutionContext, Future}
import org.mongodb.scala.*
import play.api.Logging

@Singleton
class ReportRequestRepository @Inject() (
  mongoComponent: MongoComponent,
  config: AppConfig
)(implicit ec: ExecutionContext)
    extends PlayMongoRepository[ReportRequest](
      mongoComponent,
      collectionName = "tre_report",
      domainFormat = ReportRequest.mongoFormat,
      indexes = Seq(
        IndexModel(Indexes.ascending("reportId"), IndexOptions().name("reportidx").unique(true))
      ),
      replaceIndexes = true
    ),
      Logging {

  def insertReportRequest(report: ReportRequest)(using ec: ExecutionContext): Future[Boolean] = {
    logger.info(s"Inserting a report in $collectionName table with reportId: ${report.reportId}")
    collection
      .insertOne(report)
      .head()
      .map(_ =>
        logger.info(s"Inserted a report in $collectionName table with reportId: ${report.reportId}")
        true
      )
      .recoverWith { case e =>
        logger.error(
          s"failed to insert report with reportId: ${report.reportId} into $collectionName table with ${e.getMessage}"
        )
        Future.failed(
          Throwable(
            s"failed to insert report with reportId: ${report.reportId} into $collectionName table with ${e.getMessage}"
          )
        )
      }
  }

  def findByReportId(reportId: String)(using ec: ExecutionContext): Future[Option[ReportRequest]] =
    collection
      .find(Filters.equal("reportId", reportId))
      .headOption()
      .recoverWith { case e =>
        logger.error(s"failed to retrieve user with userid: $reportId in $collectionName table with ${e.getMessage}")
        Future.failed(
          Throwable(s"failed to retrieve user with userid: $reportId in $collectionName table with ${e.getMessage}")
        )
      }

  def updateByReportId(report: ReportRequest): Future[Boolean] =
    collection
      .replaceOne(Filters.equal("reportId", report.reportId), report)
      .toFuture()
      .map(_.wasAcknowledged())
      .recoverWith { case e =>
        logger.error(
          s"failed to update report with reportId: ${report.reportId} in $collectionName table with ${e.getMessage}"
        )
        Future.failed(
          Throwable(
            s"failed to update report with reportId: ${report.reportId} in $collectionName table with ${e.getMessage}"
          )
        )
      }

  def deleteByReportId(reportId: String): Future[Boolean] =
    collection
      .deleteOne(Filters.equal("reportId", reportId))
      .toFuture()
      .map(_.wasAcknowledged())
      .recoverWith { case e =>
        logger.error(s"failed to delete report with reportId: $reportId in $collectionName table with ${e.getMessage}")
        Future.failed(
          Throwable(s"failed to delete report with reportId: $reportId in $collectionName table with ${e.getMessage}")
        )
      }
}
