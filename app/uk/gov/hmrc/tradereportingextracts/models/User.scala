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

import play.api.libs.json.{Format, Json}

case class User(userid: String, eori: String, thirdpartyEmails: Array[String], nudgeEmails: Array[String]):
  override def equals(that: Any): Boolean = that match
    case a: User =>
      this.userid == a.userid
      &&
      this.eori == a.eori
    case _ =>
      false

object User:
  given mongoFormat: Format[User] = Json.format[User]
  given CanEqual[User, User] = CanEqual.derived
