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

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.*
import uk.gov.hmrc.tradereportingextracts.models.AccessType.IMPORTS

import java.time.Instant

class UserDetailsSpec extends AnyFreeSpec with Matchers {

  "UserDetails" - {

    "must serialize and deserialize correctly" in {
      val authorisedUser    = AuthorisedUser(
        eori = "GB123456789000",
        accessStart = Instant.parse("2024-06-01T00:00:00Z"),
        accessEnd = Some(Instant.parse("2024-12-31T23:59:59Z")),
        reportDataStart = Some(Instant.parse("2024-06-01T00:00:00Z")),
        reportDataEnd = Some(Instant.parse("2024-12-31T23:59:59Z")),
        accessType = Set(IMPORTS)
      )
      val companyInfo       = CompanyInformation()
      val notificationEmail = NotificationEmail()

      val userDetails = UserDetails(
        eori = "GB123456789000",
        additionalEmails = Seq("test@example.com"),
        authorisedUsers = Seq(authorisedUser),
        companyInformation = companyInfo,
        notificationEmail = notificationEmail
      )

      val json = Json.toJson(userDetails)
      json.as[UserDetails] mustBe userDetails
    }
  }
}
