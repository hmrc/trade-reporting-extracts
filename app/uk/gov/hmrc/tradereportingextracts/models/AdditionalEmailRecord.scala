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

import play.api.libs.json.*
import uk.gov.hmrc.crypto.Sensitive.*
import uk.gov.hmrc.crypto.json.JsonEncryption
import uk.gov.hmrc.crypto.{Decrypter, Encrypter}

import java.time.Instant

case class AdditionalEmailRecord(
  traderEori: String,
  additionalEmails: Seq[AdditionalEmailEntry] = Seq.empty,
  lastAccessed: Instant = Instant.now()
)

case class AdditionalEmailEntry(
  email: SensitiveString,
  accessDate: Instant = Instant.now()
)

object AdditionalEmailFormats:
  def formats(using crypto: Encrypter & Decrypter) = {
    import MongoInstantFormat.*
    given sensitiveFormat: Format[SensitiveString]    =
      JsonEncryption.sensitiveEncrypterDecrypter(SensitiveString.apply)
    given entryFormat: Format[AdditionalEmailEntry]   = Json.format[AdditionalEmailEntry]
    given recordFormat: Format[AdditionalEmailRecord] = Json.format[AdditionalEmailRecord]
    (entryFormat, recordFormat)
  }

object AdditionalEmailRecord:
  def encryptedFormat(using crypto: Encrypter & Decrypter): Format[AdditionalEmailRecord] =
    AdditionalEmailFormats.formats._2

object AdditionalEmailEntry:
  def encryptedFormat(using crypto: Encrypter & Decrypter): Format[AdditionalEmailEntry] =
    AdditionalEmailFormats.formats._1
