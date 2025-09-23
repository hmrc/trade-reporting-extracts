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
import org.mongodb.scala.*
import org.mongodb.scala.model.*
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.play.http.logging.Mdc
import uk.gov.hmrc.tradereportingextracts.config.AppConfig
import uk.gov.hmrc.tradereportingextracts.models.etmp.EoriUpdate
import uk.gov.hmrc.tradereportingextracts.models.thirdParty.ThirdPartyAddedConfirmation
import uk.gov.hmrc.tradereportingextracts.models.{AuthorisedUser, User}

import java.util.concurrent.TimeUnit
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UserRepository @Inject() (appConfig: AppConfig, mongoComponent: MongoComponent)(using ec: ExecutionContext)
    extends PlayMongoRepository[User](
      collectionName = "tre-user",
      mongoComponent = mongoComponent,
      domainFormat = User.format,
      indexes = Seq(
        IndexModel(
          Indexes.ascending("eori"),
          IndexOptions().name("eori-index").unique(true)
        ),
        IndexModel(
          Indexes.ascending("accessDate"),
          IndexOptions().name("accessDate-ttl-index").expireAfter(appConfig.userTTLDays, TimeUnit.DAYS)
        )
      ),
      replaceIndexes = true
    ):

  def insert(user: User)(using ec: ExecutionContext): Future[Boolean] = Mdc.preservingMdc {
    collection
      .insertOne(user)
      .head()
      .map(_.wasAcknowledged())
  }

  def findByEori(eori: String)(using ec: ExecutionContext): Future[Option[User]] = Mdc.preservingMdc {
    collection
      .find(Filters.equal("eori", eori))
      .headOption()
  }

  def getOrCreateUser(eori: String): Future[User] = Mdc.preservingMdc {
    findByEori(eori).flatMap {
      case Some(existingUser) =>
        val updatedUser = existingUser.copy(accessDate = java.time.Instant.now())
        update(updatedUser).map(_ => updatedUser)
      case None               =>
        val newUser = User(eori)
        insert(newUser).map(_ => newUser)
    }
  }

  def update(user: User): Future[Boolean] = Mdc.preservingMdc {
    collection
      .replaceOne(Filters.equal("eori", user.eori), user)
      .toFuture()
      .map(_.wasAcknowledged())
  }

  def updateEori(eoriUpdate: EoriUpdate): Future[Boolean] = Mdc.preservingMdc {
    val updateQuery  = Filters.equal("eori", eoriUpdate.oldEori)
    val updateAction = Updates.combine(
      Updates.set("eori", eoriUpdate.newEori)
    )
    collection
      .updateOne(
        filter = updateQuery,
        update = updateAction
      )
      .toFuture()
      .map(_.wasAcknowledged())
  }

  def deleteByEori(eori: String): Future[Boolean] = Mdc.preservingMdc {
    collection
      .deleteOne(Filters.equal("eori", eori))
      .toFuture()
      .map(_.wasAcknowledged())
  }

  def getAuthorisedEoris(eori: String): Future[Seq[String]] = Mdc.preservingMdc {
    findByEori(eori).flatMap {
      case Some(user) =>
        val authorisedEoris = user.authorisedUsers.map(_.eori)
        Future.successful(authorisedEoris)
      case None       =>
        Future.failed(new Exception(s"User with EORI $eori not found"))
    }
  }

  def addAuthorisedUser(eori: String, authorisedUser: AuthorisedUser): Future[ThirdPartyAddedConfirmation] =
    Mdc.preservingMdc {
      findByEori(eori)
        .flatMap {
          case Some(existingUser) =>
            val updatedAuthorisedUsers =
              existingUser.authorisedUsers :+ authorisedUser
            val updatedUser            = existingUser.copy(authorisedUsers = updatedAuthorisedUsers)
            update(updatedUser).map(_ => ThirdPartyAddedConfirmation(authorisedUser.eori))
          case None               =>
            Future.failed(new Exception(s"User with EORI $eori not found"))
        }
        .recoverWith { case ex: Exception =>
          Future.failed(new Exception(s"Failed to add authorised user for EORI $eori: ${ex.getMessage}", ex))
        }
    }

  def deleteAuthorisedUser(eori: String, authorisedEori: String): Future[Boolean] =
    Mdc.preservingMdc {
      findByEori(eori)
        .flatMap {
          case Some(existingUser) =>
            val updatedAuthorisedUsers = existingUser.authorisedUsers.filterNot(_.eori == authorisedEori)
            val updatedUser            = existingUser.copy(authorisedUsers = updatedAuthorisedUsers)
            update(updatedUser)
          case None               =>
            Future.failed(new Exception(s"User with EORI $eori not found"))
        }
        .recoverWith { case ex: Exception =>
          Future.failed(new Exception(s"Failed to delete authorised user for EORI $eori: ${ex.getMessage}", ex))
        }
    }

  def getAuthorisedUser(eori: String, authorisedEori: String): Future[Option[AuthorisedUser]] = Mdc.preservingMdc {
    findByEori(eori).map {
      case Some(user) =>
        user.authorisedUsers.find(_.eori == authorisedEori)
      case None       =>
        None
    }
  }

  def getUsersByAuthorisedEori(authorisedEori: String): Future[Seq[User]] = Mdc.preservingMdc {
    collection
      .find(Filters.elemMatch("authorisedUsers", Filters.equal("eori", authorisedEori)))
      .toFuture()
  }
