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
  accessEnd: Option[Instant],
  reportDataStart: Option[Instant],
  reportDataEnd: Option[Instant],
  accessType: Set[AccessType],
  referenceName: Option[String] = None
)

object MongoInstantFormat:
  private val instantReads: Reads[Instant]    = Reads { js =>
    (js \ "$date" \ "$numberLong").validate[String].map(str => Instant.ofEpochMilli(str.toLong))
  }
  private val instantWrites: Writes[Instant]  =
    (instant: Instant) => Json.obj("$date" -> Json.obj("$numberLong" -> instant.toEpochMilli.toString))
  implicit val instantFormat: Format[Instant] = Format(instantReads, instantWrites)

object User:
  import MongoInstantFormat._
  given format: Format[User] = Json.format[User]

object AuthorisedUser:
  import MongoInstantFormat._
  given Format[AuthorisedUser] = Json.format[AuthorisedUser]
