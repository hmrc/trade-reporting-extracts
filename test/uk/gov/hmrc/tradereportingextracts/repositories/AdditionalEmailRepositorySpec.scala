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

import org.mongodb.scala.SingleObservableFuture
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import uk.gov.hmrc.crypto.Sensitive.SensitiveString
import uk.gov.hmrc.crypto.{Decrypter, Encrypter}
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport
import uk.gov.hmrc.tradereportingextracts.config.{AppConfig, CryptoProvider}
import uk.gov.hmrc.tradereportingextracts.models.etmp.EoriUpdate
import uk.gov.hmrc.tradereportingextracts.models.{AdditionalEmailEntry, AdditionalEmailRecord}

import java.time.Instant
import java.time.temporal.ChronoUnit
import scala.concurrent.ExecutionContext.Implicits.global

class AdditionalEmailRepositorySpec
    extends PlaySpec
    with GuiceOneAppPerSuite
    with Matchers
    with ScalaFutures
    with IntegrationPatience
    with DefaultPlayMongoRepositorySupport[AdditionalEmailRecord] {

  lazy val appConfig: AppConfig                 = app.injector.instanceOf[AppConfig]
  lazy val cryptoProvider: CryptoProvider       = app.injector.instanceOf[CryptoProvider]
  implicit val crypto: Encrypter with Decrypter = cryptoProvider.get

  override protected val repository = new AdditionalEmailRepository(appConfig, mongoComponent)
  private val repo                  = repository.asInstanceOf[AdditionalEmailRepository]

  val testEori1  = "GB123456789000"
  val testEori2  = "GB987654321000"
  val testEmail1 = "test1@example.com"
  val testEmail2 = "test2@example.com"
  val testEmail3 = "test3@example.com"
  val now        = Instant.now()

  "AdditionalEmailRepository" should {

    "findByEori" should {

      "return None when no record exists for EORI" in {
        val result = repo.findByEori(testEori1).futureValue
        result mustBe None
      }

      "return the record when it exists for EORI" in {
        val emailEntry = AdditionalEmailEntry(SensitiveString(testEmail1), now)
        val record     = AdditionalEmailRecord(testEori1, Seq(emailEntry), now)

        repo.collection.insertOne(record).toFuture().futureValue

        val result = repo.findByEori(testEori1).futureValue
        result mustBe defined
        result.get.traderEori mustEqual testEori1
        result.get.additionalEmails must have size 1
        result.get.additionalEmails.head.email.decryptedValue mustEqual testEmail1
      }
    }

    "getEmailsForEori" should {

      "return empty sequence when no record exists" in {
        val result = repo.getEmailsForEori("nonexistent").futureValue
        result mustBe empty
      }

      "return decrypted emails for existing EORI" in {
        val emailEntry1 = AdditionalEmailEntry(SensitiveString(testEmail1), now)
        val emailEntry2 = AdditionalEmailEntry(SensitiveString(testEmail2), now)
        val record      = AdditionalEmailRecord(testEori1, Seq(emailEntry1, emailEntry2), now)

        repo.collection.insertOne(record).toFuture().futureValue

        val result = repo.getEmailsForEori(testEori1).futureValue
        result must contain theSameElementsAs Seq(testEmail1, testEmail2)
      }
    }

    "addEmail" should {

      "create new record when EORI doesn't exist" in {
        val result = repo.addEmail(testEori1, testEmail1).futureValue
        result mustBe true

        val storedRecord = repo.findByEori(testEori1).futureValue
        storedRecord mustBe defined
        storedRecord.get.traderEori mustEqual testEori1
        storedRecord.get.additionalEmails must have size 1
        storedRecord.get.additionalEmails.head.email.decryptedValue mustEqual testEmail1
      }

      "add email to existing record when EORI exists and email doesn't exist" in {
        repo.addEmail(testEori1, testEmail1).futureValue
        val result = repo.addEmail(testEori1, testEmail2).futureValue
        result mustBe true

        val storedRecord = repo.findByEori(testEori1).futureValue
        storedRecord mustBe defined
        storedRecord.get.additionalEmails must have size 2

        val emails = storedRecord.get.additionalEmails.map(_.email.decryptedValue)
        emails must contain theSameElementsAs Seq(testEmail1, testEmail2)
      }
    }

    "updateLastAccessed" should {

      "return false when EORI doesn't exist" in {
        val result = repo.updateLastAccessed("nonexistent").futureValue
        result mustBe false
      }

      "successfully update lastAccessed when EORI exists" in {
        repo.addEmail(testEori1, testEmail1).futureValue
        val originalRecord       = repo.findByEori(testEori1).futureValue.get
        val originalLastAccessed = originalRecord.lastAccessed

        val result = repo.updateLastAccessed(testEori1).futureValue
        result mustBe true

        val updatedRecord = repo.findByEori(testEori1).futureValue.get
        updatedRecord.lastAccessed must be > originalLastAccessed
      }
    }

    "deleteByEori" should {

      "return true when successfully deleting existing record" in {
        repo.addEmail(testEori1, testEmail1).futureValue

        val result = repo.deleteByEori(testEori1).futureValue
        result mustBe true

        val deletedRecord = repo.findByEori(testEori1).futureValue
        deletedRecord mustBe None
      }

      "return true even when EORI doesn't exist" in {
        val result = repo.deleteByEori("nonexistent").futureValue
        result mustBe true
      }
    }

    "handle encryption correctly" should {

      "store emails encrypted and retrieve them decrypted" in {
        repo.addEmail(testEori1, testEmail1).futureValue

        val record          = repo.findByEori(testEori1).futureValue.get
        val storedEncrypted = record.additionalEmails.head.email
        val decryptedValue  = storedEncrypted.decryptedValue
        decryptedValue mustEqual testEmail1
        storedEncrypted.toString must not equal testEmail1
      }

      "maintain encryption across database operations" in {
        repo.addEmail(testEori1, testEmail1).futureValue
        repo.updateLastAccessed(testEori1).futureValue

        val emails = repo.getEmailsForEori(testEori1).futureValue
        emails must contain only testEmail1
      }
    }

    "AC2: Individual Email TTL (365 days)" should {

      "automatically clean expired emails during addEmail operation" in {
        val now         = Instant.now()
        val expiredDate = now.minusSeconds(365 * 24 * 60 * 60 + 3600) // 365 days + 1 hour ago
        val validDate   = now.minusSeconds(364 * 24 * 60 * 60) // 364 days ago

        // Create a record with both expired and valid emails
        val expiredEmail  = AdditionalEmailEntry(SensitiveString(testEmail1), expiredDate)
        val validEmail    = AdditionalEmailEntry(SensitiveString(testEmail2), validDate)
        val initialRecord = AdditionalEmailRecord(testEori1, Seq(expiredEmail, validEmail), now)

        // Insert initial record directly
        repo.collection.insertOne(initialRecord).toFuture().futureValue

        // Adding a new email should trigger cleanup
        val newEmail = "new@example.com"
        repo.addEmail(testEori1, newEmail).futureValue mustBe true

        // Verify expired email was removed, valid email and new email remain
        val emails = repo.getEmailsForEori(testEori1).futureValue
        emails must have size 2
        emails must contain(testEmail2)
        emails must contain(newEmail)
        emails must not contain testEmail1
      }

      "automatically clean expired emails during updateEmailAccessDate operation" in {
        val now         = Instant.now()
        val expiredDate = now.minusSeconds(365 * 24 * 60 * 60 + 3600) // 365 days + 1 hour ago
        val validDate   = now.minusSeconds(364 * 24 * 60 * 60) // 364 days ago

        // Create a record with both expired and valid emails
        val expiredEmail  = AdditionalEmailEntry(SensitiveString(testEmail1), expiredDate)
        val validEmail    = AdditionalEmailEntry(SensitiveString(testEmail2), validDate)
        val initialRecord = AdditionalEmailRecord(testEori2, Seq(expiredEmail, validEmail), now)

        // Insert initial record directly
        repo.collection.insertOne(initialRecord).toFuture().futureValue

        // Updating access date should trigger cleanup
        repo.updateEmailAccessDate(testEori2, testEmail2).futureValue mustBe true

        // Verify expired email was removed, updated email remains
        val emails = repo.getEmailsForEori(testEori2).futureValue
        emails must have size 1
        emails must contain only testEmail2
        emails must not contain testEmail1
      }

      "preserve emails that are exactly 365 days old" in {
        val now            = Instant.now()
        val exactlyOldDate =
          now.minus(365, ChronoUnit.DAYS).plusSeconds(1) // Exactly 365 days ago + 1 second (should be preserved)

        // Create a record with an email exactly 365 days old
        val borderlineEmail = AdditionalEmailEntry(SensitiveString(testEmail1), exactlyOldDate)
        val initialRecord   = AdditionalEmailRecord(testEori1, Seq(borderlineEmail), now)

        // Insert initial record directly
        repo.collection.insertOne(initialRecord).toFuture().futureValue

        // Adding a new email should not remove the 365-day-old email (should preserve it)
        repo.addEmail(testEori1, testEmail2).futureValue mustBe true

        // Verify both emails are preserved
        val emails = repo.getEmailsForEori(testEori1).futureValue
        emails must have size 2
        emails must contain(testEmail1)
        emails must contain(testEmail2)
      }

      "clean multiple expired emails while preserving valid ones" in {
        val now          = Instant.now()
        val expired1Date = now.minusSeconds(366 * 24 * 60 * 60) // 366 days ago
        val expired2Date = now.minusSeconds(400 * 24 * 60 * 60) // 400 days ago
        val validDate1   = now.minusSeconds(100 * 24 * 60 * 60) // 100 days ago
        val validDate2   = now.minusSeconds(200 * 24 * 60 * 60) // 200 days ago

        // Create a record with multiple expired and valid emails
        val expired1 = AdditionalEmailEntry(SensitiveString("expired1@example.com"), expired1Date)
        val expired2 = AdditionalEmailEntry(SensitiveString("expired2@example.com"), expired2Date)
        val valid1   = AdditionalEmailEntry(SensitiveString("valid1@example.com"), validDate1)
        val valid2   = AdditionalEmailEntry(SensitiveString("valid2@example.com"), validDate2)

        val initialRecord = AdditionalEmailRecord(testEori2, Seq(expired1, expired2, valid1, valid2), now)

        // Insert initial record directly
        repo.collection.insertOne(initialRecord).toFuture().futureValue

        // Update access date should trigger cleanup
        repo.updateEmailAccessDate(testEori2, "valid1@example.com").futureValue mustBe true

        // Verify only valid emails remain
        val emails = repo.getEmailsForEori(testEori2).futureValue
        emails must have size 2
        emails must contain("valid1@example.com")
        emails must contain("valid2@example.com")
        emails must not contain "expired1@example.com"
        emails must not contain "expired2@example.com"
      }

      "handle edge case when all emails are expired" in {
        val now          = Instant.now()
        val expired1Date = now.minusSeconds(366 * 24 * 60 * 60) // 366 days ago
        val expired2Date = now.minusSeconds(400 * 24 * 60 * 60) // 400 days ago

        // Create a record with only expired emails
        val expired1      = AdditionalEmailEntry(SensitiveString("expired1@example.com"), expired1Date)
        val expired2      = AdditionalEmailEntry(SensitiveString("expired2@example.com"), expired2Date)
        val initialRecord = AdditionalEmailRecord(testEori1, Seq(expired1, expired2), now)

        // Insert initial record directly
        repo.collection.insertOne(initialRecord).toFuture().futureValue

        // Adding a new email should clean all expired emails
        repo.addEmail(testEori1, testEmail1).futureValue mustBe true

        // Verify only the newly added email remains
        val emails = repo.getEmailsForEori(testEori1).futureValue
        emails must have size 1
        emails must contain only testEmail1
        emails must not contain "expired1@example.com"
        emails must not contain "expired2@example.com"
      }

      "not affect operations when no emails are expired" in {
        val now         = Instant.now()
        val recentDate1 = now.minusSeconds(100 * 24 * 60 * 60) // 100 days ago
        val recentDate2 = now.minusSeconds(200 * 24 * 60 * 60) // 200 days ago

        // Create a record with only recent emails
        val recent1       = AdditionalEmailEntry(SensitiveString("recent1@example.com"), recentDate1)
        val recent2       = AdditionalEmailEntry(SensitiveString("recent2@example.com"), recentDate2)
        val initialRecord = AdditionalEmailRecord(testEori2, Seq(recent1, recent2), now)

        // Insert initial record directly
        repo.collection.insertOne(initialRecord).toFuture().futureValue

        // Update access date should not remove any emails
        repo.updateEmailAccessDate(testEori2, "recent1@example.com").futureValue mustBe true

        // Verify all emails are preserved
        val emails = repo.getEmailsForEori(testEori2).futureValue
        emails must have size 2
        emails must contain("recent1@example.com")
        emails must contain("recent2@example.com")
      }
    }

    "updateEori" should {

      "return true and update the traderEori when a record exists" in {
        val originalEori = testEori1
        val newEori      = "GB111111111000"

        val now        = Instant.now()
        val emailEntry = AdditionalEmailEntry(SensitiveString(testEmail1), now)
        val record     = AdditionalEmailRecord(originalEori, Seq(emailEntry), now)

        repo.collection.insertOne(record).toFuture().futureValue

        val result = repo.updateEori(EoriUpdate(oldEori = originalEori, newEori = newEori)).futureValue
        result mustBe true

        repo.findByEori(originalEori).futureValue mustBe None

        val updated = repo.findByEori(newEori).futureValue
        updated mustBe defined
        updated.get.traderEori mustEqual newEori
        updated.get.additionalEmails must have size 1
        updated.get.additionalEmails.head.email.decryptedValue mustEqual testEmail1
        updated.get.lastAccessed.truncatedTo(ChronoUnit.SECONDS) mustEqual now.truncatedTo(ChronoUnit.SECONDS)
      }

      "return true even when the old EORI does not exist (acknowledged but no-op)" in {
        val nonExistentEori = "GB000000000000"
        val newEori         = "GB222222222000"

        val result = repo.updateEori(EoriUpdate(oldEori = nonExistentEori, newEori = newEori)).futureValue
        result mustBe true

        repo.findByEori(nonExistentEori).futureValue mustBe None
        repo.findByEori(newEori).futureValue mustBe None
      }

      "not affect other records" in {
        val eoriToChange   = "GB555555555000"
        val unaffectedEori = "GB666666666000"
        val newEori        = "GB777777777000"

        val tNow = Instant.now()
        repo.collection
          .insertOne(
            AdditionalEmailRecord(eoriToChange, Seq(AdditionalEmailEntry(SensitiveString(testEmail1), tNow)), tNow)
          )
          .toFuture()
          .futureValue

        repo.collection
          .insertOne(
            AdditionalEmailRecord(unaffectedEori, Seq(AdditionalEmailEntry(SensitiveString(testEmail2), tNow)), tNow)
          )
          .toFuture()
          .futureValue

        repo.findByEori(eoriToChange).futureValue mustBe defined
        repo.findByEori(unaffectedEori).futureValue mustBe defined

        val updated = repo.updateEori(EoriUpdate(newEori, eoriToChange)).futureValue
        updated mustBe true

        repo.findByEori(eoriToChange).futureValue mustBe None
        repo.findByEori(newEori).futureValue.map(_.additionalEmails.map(_.email.decryptedValue)) mustBe Some(
          Seq(testEmail1)
        )
        repo.findByEori(unaffectedEori).futureValue.map(_.additionalEmails.map(_.email.decryptedValue)) mustBe Some(
          Seq(testEmail2)
        )
      }

      "keep encryption intact after EORI update" in {
        val originalEori = "GB888888888000"
        val newEori      = "GB999999999000"

        repo.addEmail(originalEori, testEmail1).futureValue mustBe true

        repo.updateEori(EoriUpdate(newEori, originalEori)).futureValue mustBe true

        val stored          = repo.findByEori(newEori).futureValue.get
        val storedEncrypted = stored.additionalEmails.head.email
        storedEncrypted.decryptedValue mustEqual testEmail1
        storedEncrypted.toString must not equal testEmail1
      }

    }

  }
}
