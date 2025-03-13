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
import org.mongodb.scala.model.{Filters, IndexModel, IndexOptions, Indexes, Updates}
import play.api.Logging
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import org.mongodb.scala.*
import uk.gov.hmrc.tradereportingextracts.models.User

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UserRepository @Inject()(mongoComponent: MongoComponent)
                              (using ec: ExecutionContext) extends PlayMongoRepository[User](
  collectionName = "tre_user",
  mongoComponent = mongoComponent,
  domainFormat = User.mongoFormat,
  indexes = Seq(
    IndexModel(
      Indexes.ascending("userid"),
      IndexOptions().name("useridx").unique(true)
    )
  ),
  replaceIndexes = true
), Logging:

  def insertUser(user: User)
                (using ec: ExecutionContext): Future[Boolean] = {
    logger.info(s"Inserting a user in $collectionName table with userid: ${user.userid}")
    collection.insertOne(user)
      .head()
      .map(_ =>
        logger.info(s"Inserted a user in $collectionName table with userid: ${user.userid}")
        true
      )
      .recoverWith {
        case e =>
          logger.error(s"failed to insert user with userid: ${user.userid} into $collectionName table with ${e.getMessage}")
          Future.failed(Throwable(s"failed to insert user with userid: ${user.userid} into $collectionName table with ${e.getMessage}"))
      }
  }

  def findByUserId(userid: Long)(using ec: ExecutionContext): Future[Option[User]] =
    collection.find(Filters.equal("userid", userid))
      .headOption()
      .recoverWith {
        case e =>
          logger.error(s"failed to retrieve user with userid: $userid in $collectionName table with ${e.getMessage}")
          Future.failed(Throwable(s"failed to retrieve user with userid: $userid in $collectionName table with ${e.getMessage}"))
      }

  def updateByUserId(user: User): Future[Boolean] =
    collection.replaceOne(
      Filters.equal("userid", user.userid),
      user)
      .toFuture()
      .map(_.wasAcknowledged())
      .recoverWith {
        case e =>
          logger.info(s"failed to update user with userid: ${user.userid} in $collectionName table with ${e.getMessage}")
          Future.failed(Throwable(s"failed to update user with userid: ${user.userid} into $collectionName table with ${e.getMessage}"))
      }

  def deleteByUserId(userid: Long): Future[Boolean] =
    collection.deleteOne(Filters.equal("userid", userid))
      .head()
      .map(_ =>
        logger.info(s"Deleted a user in $collectionName table with userid: $userid")
        true
      )
      .recoverWith {
        case e =>
          logger.error(s"failed to delete user with userid: $userid into $collectionName table with ${e.getMessage}")
          Future.failed(Throwable(s"failed to delete user with userid: $userid into $collectionName table with ${e.getMessage}"))
      }
