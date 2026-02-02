/*
 * Copyright 2026 HM Revenue & Customs
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
import uk.gov.hmrc.crypto.{Decrypter, Encrypter}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mdc.Mdc
import uk.gov.hmrc.tradereportingextracts.config.AppConfig
import uk.gov.hmrc.tradereportingextracts.models.{AdditionalEmailEntry, AdditionalEmailRecord}
import uk.gov.hmrc.crypto.Sensitive.*
import uk.gov.hmrc.tradereportingextracts.models.etmp.EoriUpdate

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AdditionalEmailRepository @Inject() (
  appConfig: AppConfig,
  mongoComponent: MongoComponent
)(using
  ec: ExecutionContext,
  crypto: Encrypter with Decrypter
) extends PlayMongoRepository[AdditionalEmailRecord](
      collectionName = "tre-additional-emails",
      mongoComponent = mongoComponent,
      domainFormat = AdditionalEmailRecord.encryptedFormat,
      indexes = Seq(
        IndexModel(
          Indexes.ascending("traderEori"),
          IndexOptions().name("traderEori-index").unique(true)
        ),
        IndexModel(
          Indexes.ascending("lastAccessed"),
          IndexOptions().name("lastAccessed-ttl-index").expireAfter(appConfig.emailTTLDays, TimeUnit.DAYS)
        ),
        IndexModel(
          Indexes.ascending("additionalEmails.accessDate"),
          IndexOptions().name("email-accessDate-index")
        )
      ),
      replaceIndexes = true
    ):

  def findByEori(eori: String): Future[Option[AdditionalEmailRecord]] = Mdc.preservingMdc {
    collection
      .find(Filters.equal("traderEori", eori))
      .headOption()
  }

  private def cleanExpiredEmails(eori: String): Future[Boolean] = Mdc.preservingMdc {
    val cutoffDate = Instant.now().minus(appConfig.emailTTLDays, ChronoUnit.DAYS)

    collection
      .updateOne(
        filter = Filters.equal("traderEori", eori),
        update = Updates.combine(
          Updates.pull("additionalEmails", Filters.lt("accessDate", cutoffDate)),
          Updates.set("lastAccessed", Instant.now())
        )
      )
      .toFuture()
      .map(_.wasAcknowledged())
  }

  def getEmailsForEori(eori: String): Future[Seq[String]] = Mdc.preservingMdc {
    for {
      _      <- cleanExpiredEmails(eori)
      result <- findByEori(eori).map {
                  case Some(record) =>
                    record.additionalEmails.map(_.email.decryptedValue)
                  case None         => Seq.empty
                }
    } yield result
  }

  private def upsert(record: AdditionalEmailRecord): Future[Boolean] = Mdc.preservingMdc {
    collection
      .replaceOne(
        filter = Filters.equal("traderEori", record.traderEori),
        replacement = record,
        options = ReplaceOptions().upsert(true)
      )
      .toFuture()
      .map(_.wasAcknowledged())
  }

  def addEmail(eori: String, email: String): Future[Boolean] = Mdc.preservingMdc {
    val now = Instant.now()
    for {
      _      <- cleanExpiredEmails(eori)
      result <- findByEori(eori).flatMap {
                  case Some(existingRecord) =>
                    val existingEmailIndex = existingRecord.additionalEmails.indexWhere(_.email.decryptedValue == email)
                    if (existingEmailIndex >= 0) {
                      val updatedEmails = existingRecord.additionalEmails.updated(
                        existingEmailIndex,
                        existingRecord.additionalEmails(existingEmailIndex).copy(accessDate = now)
                      )
                      val updatedRecord = existingRecord.copy(
                        additionalEmails = updatedEmails,
                        lastAccessed = now
                      )
                      upsert(updatedRecord)
                    } else {
                      val encryptedEmail = SensitiveString(email)
                      val newEmailEntry  = AdditionalEmailEntry(encryptedEmail, now)
                      val updatedRecord  = existingRecord.copy(
                        additionalEmails = existingRecord.additionalEmails :+ newEmailEntry,
                        lastAccessed = now
                      )
                      upsert(updatedRecord)
                    }
                  case None                 =>
                    val encryptedEmail = SensitiveString(email)
                    val emailEntry     = AdditionalEmailEntry(encryptedEmail, now)
                    val newRecord      = AdditionalEmailRecord(
                      traderEori = eori,
                      additionalEmails = Seq(emailEntry),
                      lastAccessed = now
                    )
                    upsert(newRecord)
                }
    } yield result
  }

  def removeEmail(eori: String, email: String): Future[Boolean] = Mdc.preservingMdc {
    val now = Instant.now()

    collection
      .updateOne(
        filter = Filters.equal("traderEori", eori),
        update = Updates.combine(
          Updates.pull("additionalEmails", Filters.equal("email", email)),
          Updates.set("lastAccessed", now)
        )
      )
      .toFuture()
      .map(_.getModifiedCount > 0)
  }

  def updateEmailAccessDate(eori: String, email: String): Future[Boolean] = Mdc.preservingMdc {
    val now = Instant.now()

    for {
      _      <- cleanExpiredEmails(eori)
      result <- findByEori(eori).flatMap {
                  case Some(existingRecord) =>
                    val existingEmailIndex = existingRecord.additionalEmails.indexWhere(_.email.decryptedValue == email)
                    if (existingEmailIndex >= 0) {
                      val updatedEmails = existingRecord.additionalEmails.updated(
                        existingEmailIndex,
                        existingRecord.additionalEmails(existingEmailIndex).copy(accessDate = now)
                      )
                      val updatedRecord = existingRecord.copy(
                        additionalEmails = updatedEmails,
                        lastAccessed = now
                      )
                      upsert(updatedRecord)
                    } else {
                      Future.successful(false)
                    }
                  case None                 => Future.successful(false)
                }
    } yield result
  }

  def updateLastAccessed(eori: String): Future[Boolean] = Mdc.preservingMdc {
    val now = Instant.now()

    collection
      .updateOne(
        filter = Filters.equal("traderEori", eori),
        update = Updates.set("lastAccessed", now)
      )
      .toFuture()
      .map(_.getModifiedCount > 0)
  }

  def deleteByEori(eori: String): Future[Boolean] = Mdc.preservingMdc {
    collection
      .deleteOne(Filters.equal("traderEori", eori))
      .toFuture()
      .map(_.wasAcknowledged())
  }

  def updateEori(eoriUpdate: EoriUpdate): Future[Boolean] = Mdc.preservingMdc {
    val updateQuery  = Filters.equal("traderEori", eoriUpdate.oldEori)
    val updateAction = Updates.combine(
      Updates.set("traderEori", eoriUpdate.newEori)
    )
    collection
      .updateOne(
        filter = updateQuery,
        update = updateAction
      )
      .toFuture()
      .map(_.wasAcknowledged())
  }
