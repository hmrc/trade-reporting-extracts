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
import uk.gov.hmrc.crypto.{Decrypter, Encrypter}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mdc.Mdc
import uk.gov.hmrc.tradereportingextracts.config.AppConfig
import uk.gov.hmrc.tradereportingextracts.models.ReportRequest

import java.time.{LocalDate, ZoneOffset}
import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ReportRequestRepository @Inject() (appConfig: AppConfig, mongoComponent: MongoComponent)(implicit
  ec: ExecutionContext,
  crypto: Encrypter with Decrypter
) extends PlayMongoRepository[ReportRequest](
      mongoComponent,
      collectionName = "tre-report-request",
      domainFormat = ReportRequest.encryptedFormat,
      indexes = Seq(
        IndexModel(Indexes.ascending("reportRequestId"), IndexOptions().name("reportRequestId-index").unique(true)),
        IndexModel(
          Indexes.ascending("updateDate"),
          IndexOptions().name("updateDate-ttl-index").expireAfter(appConfig.reportRequestTTLDays, TimeUnit.DAYS)
        )
      ),
      replaceIndexes = true
    ):

  def insert(reportRequest: ReportRequest)(implicit ec: ExecutionContext): Future[Boolean] = Mdc.preservingMdc {
    collection
      .insertOne(reportRequest)
      .head()
      .map(_.wasAcknowledged())
  }

  def insertAll(reportRequests: Seq[ReportRequest])(implicit ec: ExecutionContext): Future[Boolean] =
    Mdc.preservingMdc {
      if (reportRequests.isEmpty) Future.successful(true)
      else
        collection
          .insertMany(reportRequests)
          .head()
          .map(_.wasAcknowledged())
    }

  def findByReportRequestId(reportRequestId: String)(implicit ec: ExecutionContext): Future[Option[ReportRequest]] =
    Mdc.preservingMdc {
      collection
        .find(Filters.equal("reportRequestId", reportRequestId))
        .headOption()
    }

  def findByCorrelationId(correlationId: String)(implicit ec: ExecutionContext): Future[Option[ReportRequest]] =
    Mdc.preservingMdc {
      collection
        .find(Filters.equal("correlationId", correlationId))
        .headOption()
    }

  def update(reportRequest: ReportRequest)(implicit ec: ExecutionContext): Future[Boolean] = Mdc.preservingMdc {
    collection
      .replaceOne(Filters.equal("reportRequestId", reportRequest.reportRequestId), reportRequest)
      .toFuture()
      .map(_.wasAcknowledged())
  }

  def delete(reportRequest: ReportRequest)(implicit ec: ExecutionContext): Future[Boolean] = Mdc.preservingMdc {
    collection
      .deleteOne(Filters.equal("reportRequestId", reportRequest.reportRequestId))
      .toFuture()
      .map(_.wasAcknowledged())
  }

  def findByRequesterEORI(requesterEORI: String)(using ec: ExecutionContext): Future[Seq[ReportRequest]] =
    Mdc.preservingMdc {
      collection
        .find(Filters.equal("requesterEORI", requesterEORI))
        .toFuture()
    }

  def findByRequesterEoriHistory(eoriHistory: Seq[String])(using ec: ExecutionContext): Future[Seq[ReportRequest]] =
    Mdc.preservingMdc {
      collection
        .find(Filters.in("requesterEORI", eoriHistory*))
        .toFuture()
    }

  def getRequestedReportsByHistory(eoriHistory: Seq[String])(using ec: ExecutionContext): Future[Seq[ReportRequest]] =
    Mdc.preservingMdc {
      collection
        .find(Filters.in("requesterEORI", eoriHistory*))
        .toFuture()
        .map(_.filter(!_.isReportStatusComplete))
    }

  def getAvailableReports(eori: String)(using ec: ExecutionContext): Future[Seq[ReportRequest]] = Mdc.preservingMdc {
    collection
      .find(Filters.equal("requesterEORI", eori))
      .toFuture()
      .map(_.filter(_.isReportStatusComplete))
  }

  def getAvailableReportsByHistory(eoriHistory: Seq[String])(using ec: ExecutionContext): Future[Seq[ReportRequest]] =
    Mdc.preservingMdc {
      collection
        .find(Filters.in("requesterEORI", eoriHistory*))
        .toFuture()
        .map(_.filter(_.isReportStatusComplete))
    }

  def countAvailableReports(eori: String)(using ec: ExecutionContext): Future[Long] = Mdc.preservingMdc {
    collection
      .find(Filters.equal("requesterEORI", eori))
      .toFuture()
      .map { reportRequests =>
        reportRequests.count(_.isReportStatusComplete)
      }
  }

  def deleteReportsForThirdPartyRemoval(traderEori: String, thirdPartyEori: String)(implicit
    ec: ExecutionContext
  ): Future[Boolean] = Mdc.preservingMdc {
    collection
      .deleteMany(
        Filters.and(
          Filters.equal("requesterEORI", thirdPartyEori),
          Filters.in("reportEORIs", traderEori)
        )
      )
      .toFuture()
      .map(_ => true)
  }

  def countReportSubmissionsForEoriOnDate(eori: String, date: LocalDate)(implicit ec: ExecutionContext): Future[Int] =
    Mdc.preservingMdc {
      val startOfDay = date.atStartOfDay().toInstant(ZoneOffset.UTC)
      val endOfDay   = date.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)
      collection
        .countDocuments(
          Filters.and(
            Filters.equal("requesterEORI", eori),
            Filters.gte("createDate", startOfDay),
            Filters.lt("createDate", endOfDay)
          )
        )
        .toFuture()
        .map(_.toInt)
    }
