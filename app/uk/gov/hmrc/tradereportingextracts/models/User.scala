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

package uk.gov.hmrc.tradereportingextracts.models

import play.api.libs.json.*
import uk.gov.hmrc.crypto.Sensitive.*
import uk.gov.hmrc.crypto.{Decrypter, Encrypter}
import uk.gov.hmrc.crypto.json.JsonEncryption
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.Instant

case class User(
  eori: String,
  additionalEmails: Seq[String] = Seq.empty,
  authorisedUsers: Seq[AuthorisedUser] = Seq.empty,
  accessDate: Instant = Instant.now()
)

case class AuthorisedUser(
  eori: String,
  accessStart: Instant,
  accessEnd: Instant,
  reportDataStart: Instant,
  reportDataEnd: Instant,
  accessType: AccessType
)

object User:
  given format: Format[User] = Json.format[User]

  def encryptedFormat(implicit crypto: Encrypter with Decrypter): OFormat[User] = {
    import play.api.libs.functional.syntax.*

    implicit val sensitiveFormat: Format[SensitiveString] =
      JsonEncryption.sensitiveEncrypterDecrypter(SensitiveString.apply)

    val encryptedReads: Reads[User] =
      (
        (__ \ "eori").read[SensitiveString] and
          (__ \ "additionalEmails").read[Seq[String]] and
          (__ \ "authorisedUsers").read[Seq[AuthorisedUser]] and
          (__ \ "accessDate").read(MongoJavatimeFormats.instantFormat)
      )((eori, additionalEmails, authorisedUsers, accessDate) =>
        User(eori.decryptedValue, additionalEmails, authorisedUsers, accessDate)
      )

    val encryptedWrites: OWrites[User] =
      (
        (__ \ "eori").write[SensitiveString] and
          (__ \ "additionalEmails").write[Seq[String]] and
          (__ \ "authorisedUsers").write[Seq[AuthorisedUser]] and
          (__ \ "accessDate").write(MongoJavatimeFormats.instantFormat)
      )(user => (SensitiveString(user.eori), user.additionalEmails, user.authorisedUsers, user.accessDate))

    OFormat(encryptedReads, encryptedWrites)
  }

object AuthorisedUser:
  given Format[AuthorisedUser] = Json.format[AuthorisedUser]
