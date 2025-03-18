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

import com.google.inject.{Inject, Singleton}
import org.mongodb.scala.model.{Filters, IndexModel, IndexOptions, Indexes}
import play.api.Logging
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import org.mongodb.scala.*
import uk.gov.hmrc.tradereportingextracts.models.ThirdParty

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ThirdPartyRepository @Inject()(mongoComponent: MongoComponent)
                                    (using ec: ExecutionContext) extends PlayMongoRepository[ThirdParty](
  collectionName = "tre_thirdParty",
  mongoComponent = mongoComponent,
  domainFormat = ThirdParty.mongoFormat,
  indexes = Seq(
    IndexModel(
      Indexes.ascending("userId"),
      IndexOptions().name("userIdx").unique(true)
      )
    ),
  replaceIndexes = true
  ), Logging:
  def insertThirdParty(thirdParty: ThirdParty)
                      (using ec: ExecutionContext): Future[Boolean] = {
    logger.info(s"Inserting a third party in $collectionName table with userId: ${thirdParty.userId}")
    collection.insertOne(thirdParty)
      .head()
      .map(_ =>
             logger.info(s"Inserted a third party in $collectionName table with userId: ${thirdParty.userId}")
             true
           )
      .recoverWith {
        case e =>
          logger.error(s"failed to insert third party with userId: ${thirdParty.userId} into $collectionName table with ${e.getMessage}")
          Future.failed(Throwable(s"failed to insert third party with userId: ${thirdParty.userId} into $collectionName table with ${e.getMessage}"))
      }
  }

  def findByUserId(userId: Long)(using ec: ExecutionContext): Future[Option[ThirdParty]] =
    collection.find(Filters.equal("userId", userId))
      .headOption()
      .recoverWith {
        case e =>
          logger.error(s"failed to retrieve third party with userId: $userId in $collectionName table with ${e.getMessage}")
          Future.failed(Throwable(s"failed to retrieve third party with userId: $userId in $collectionName table with ${e.getMessage}"))
      }

  def updateByUserId(thirdParty: ThirdParty): Future[Boolean] =
    collection.replaceOne(
        Filters.equal("userId", thirdParty.userId),
        thirdParty)
      .toFuture()
      .map(_.wasAcknowledged())
      .recoverWith {
        case e =>
          logger.info(s"failed to update third party with userId: ${thirdParty.userId} in $collectionName table with ${e.getMessage}")
          Future.failed(Throwable(s"failed to update third party with userId: ${thirdParty.userId} into $collectionName table with ${e.getMessage}"))
      }

  def deleteByUserId(userId: Long): Future[Boolean] =
    collection.deleteOne(Filters.equal("userId", userId))
      .head()
      .map(_ =>
             logger.info(s"Deleted a third party in $collectionName table with userId: $userId")
             true
           )
      .recoverWith {
        case e =>
          logger.error(s"failed to delete third party with userId: $userId into $collectionName table with ${e.getMessage}")
          Future.failed(Throwable(s"failed to delete third party with userId: $userId into $collectionName table with ${e.getMessage}"))
      }