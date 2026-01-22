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

package uk.gov.hmrc.tradereportingextracts.models

import org.scalatest.matchers.must.Matchers
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.*
import uk.gov.hmrc.crypto.Sensitive.SensitiveString
import uk.gov.hmrc.crypto.{Decrypter, Encrypter}
import uk.gov.hmrc.tradereportingextracts.config.CryptoProvider

import java.time.Instant

class AdditionalEmailRecordSpec extends PlaySpec with GuiceOneAppPerSuite with Matchers {

  lazy val cryptoProvider: CryptoProvider       = app.injector.instanceOf[CryptoProvider]
  implicit val crypto: Encrypter with Decrypter = cryptoProvider.get

  val now        = Instant.parse("2026-01-20T10:30:00Z")
  val testEmail1 = "test1@example.com"
  val testEmail2 = "test2@example.com"
  val testEori   = "GB123456789000"

  val emailEntry1 = AdditionalEmailEntry(
    email = SensitiveString(testEmail1),
    accessDate = now
  )

  val emailEntry2 = AdditionalEmailEntry(
    email = SensitiveString(testEmail2),
    accessDate = now.plusSeconds(3600)
  )

  val emailRecord = AdditionalEmailRecord(
    traderEori = testEori,
    additionalEmails = Seq(emailEntry1, emailEntry2),
    lastAccessed = now
  )

  "AdditionalEmailEntry" should {
    "be created with correct default values" in {
      val entry = AdditionalEmailEntry(SensitiveString(testEmail1))
      entry.email.decryptedValue mustEqual testEmail1
      entry.accessDate must not be null
    }

    "serialize and deserialize to/from JSON correctly" in {
      val entryFormat       = AdditionalEmailFormats.formats._1
      val json              = Json.toJson(emailEntry1)(entryFormat)
      val deserializedEntry = json.as[AdditionalEmailEntry](entryFormat)

      deserializedEntry.email.decryptedValue mustEqual emailEntry1.email.decryptedValue
      deserializedEntry.accessDate mustEqual emailEntry1.accessDate
    }

    "compare equality correctly" in {
      val entry1 = AdditionalEmailEntry(SensitiveString(testEmail1), now)
      val entry2 = AdditionalEmailEntry(SensitiveString(testEmail1), now)
      val entry3 = AdditionalEmailEntry(SensitiveString(testEmail2), now)

      entry1 mustEqual entry2
      entry1 must not equal entry3
    }
  }

  "AdditionalEmailRecord" should {
    "be created with correct default values" in {
      val record = AdditionalEmailRecord(testEori)
      record.traderEori mustEqual testEori
      record.additionalEmails mustBe empty
      record.lastAccessed must not be null
    }

    "be created with custom values" in {
      emailRecord.traderEori mustEqual testEori
      emailRecord.additionalEmails must have size 2
      emailRecord.additionalEmails.head mustEqual emailEntry1
      emailRecord.additionalEmails(1) mustEqual emailEntry2
      emailRecord.lastAccessed mustEqual now
    }

    "serialize and deserialize to/from JSON correctly" in {
      val recordFormat       = AdditionalEmailFormats.formats._2
      val json               = Json.toJson(emailRecord)(recordFormat)
      val deserializedRecord = json.as[AdditionalEmailRecord](recordFormat)

      deserializedRecord.traderEori mustEqual emailRecord.traderEori
      deserializedRecord.additionalEmails must have size 2
      deserializedRecord.additionalEmails.head.email.decryptedValue mustEqual testEmail1
      deserializedRecord.additionalEmails(1).email.decryptedValue mustEqual testEmail2
      deserializedRecord.lastAccessed mustEqual emailRecord.lastAccessed
    }

    "handle empty email list" in {
      val emptyRecord  = AdditionalEmailRecord(testEori, Seq.empty, now)
      val recordFormat = AdditionalEmailFormats.formats._2
      val json         = Json.toJson(emptyRecord)(recordFormat)
      val deserialized = json.as[AdditionalEmailRecord](recordFormat)

      deserialized.traderEori mustEqual testEori
      deserialized.additionalEmails mustBe empty
      deserialized.lastAccessed mustEqual now
    }
  }

  "AdditionalEmailFormats" should {
    "provide correct format tuples" in {
      val formats                     = AdditionalEmailFormats.formats
      val (entryFormat, recordFormat) = formats

      entryFormat  must not be null
      recordFormat must not be null

      val entryJson  = Json.toJson(emailEntry1)(entryFormat)
      val recordJson = Json.toJson(emailRecord)(recordFormat)

      entryJson.as[AdditionalEmailEntry](entryFormat) mustEqual emailEntry1
      recordJson.as[AdditionalEmailRecord](recordFormat) mustEqual emailRecord
    }

    "handle multiple calls consistently" in {
      val formats1 = AdditionalEmailFormats.formats
      val formats2 = AdditionalEmailFormats.formats

      val json1 = Json.toJson(emailRecord)(formats1._2)
      val json2 = Json.toJson(emailRecord)(formats2._2)

      // The encrypted values will be different, but both should deserialize to same original data
      val deserialized1 = json1.as[AdditionalEmailRecord](formats1._2)
      val deserialized2 = json2.as[AdditionalEmailRecord](formats2._2)

      deserialized1 mustEqual emailRecord
      deserialized2 mustEqual emailRecord
      deserialized1 mustEqual deserialized2
    }
  }
}
