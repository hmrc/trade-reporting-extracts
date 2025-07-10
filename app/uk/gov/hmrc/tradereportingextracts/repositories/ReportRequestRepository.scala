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
import uk.gov.hmrc.tradereportingextracts.models.{ReportRequest, StringFieldRegex}
import uk.gov.hmrc.tradereportingextracts.utils.ReportRequestUtil.isReportStatusComplete

import java.util.concurrent.TimeUnit
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
        IndexModel(Indexes.ascending("reportRequestId"), IndexOptions().name("reportRequestId-index").unique(true)),
        IndexModel(
          Indexes.ascending("createDate"),
          IndexOptions().name("createDate-ttl-index").expireAfter(appConfig.reportRequestTTL, TimeUnit.SECONDS)
        )
      ),
      replaceIndexes = true
    ):

  def insert(reportRequest: ReportRequest)(implicit ec: ExecutionContext): Future[Boolean] =
    collection
      .insertOne(reportRequest)
      .head()
      .map(_.wasAcknowledged())

  def insertAll(reportRequests: Seq[ReportRequest])(implicit ec: ExecutionContext): Future[Boolean] =
    if (reportRequests.isEmpty) Future.successful(true)
    else
      collection
        .insertMany(reportRequests)
        .head()
        .map(_.wasAcknowledged())

  def findByReportRequestId(reportRequestId: String)(implicit ec: ExecutionContext): Future[Option[ReportRequest]] =
    collection
      .find(Filters.equal("reportRequestId", reportRequestId))
      .headOption()

  def findByCorrelationId(correlationId: String)(implicit ec: ExecutionContext): Future[Option[ReportRequest]] =
    collection
      .find(Filters.equal("correlationId", correlationId))
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
      .find(Filters.equal("requesterEORI", eori))
      .toFuture()
      .map { reportRequests =>
        reportRequests.filter { reportRequest =>
          isReportStatusComplete(reportRequest)
        }
      }

  def countAvailableReports(eori: String)(using ec: ExecutionContext): Future[Long] =
    collection
      .find(Filters.equal("requesterEORI", eori))
      .toFuture()
      .map { reportRequests =>
        val notificationsWithParent = for {
          req   <- reportRequests
          notif <- req.fileNotifications.getOrElse(Seq.empty)
        } yield (req, notif)
        notificationsWithParent
          .flatMap { case (req, notif) =>
            notif.reportFilesParts match {
              case StringFieldRegex.filePartPattern(part, total) =>
                Some(((req.reportRequestId, total.toInt), part.toInt))
              case _                                             => None
            }
          }
          .groupBy(_._1)
          .count { case ((_, total), values) =>
            values.map(_._2).distinct.sorted == (1 to total)
          }
      }
