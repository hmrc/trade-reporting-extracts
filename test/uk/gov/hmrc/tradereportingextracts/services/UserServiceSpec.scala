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

package uk.gov.hmrc.tradereportingextracts.services

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{OptionValues, TryValues}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.mockito.MockitoSugar.mock
import uk.gov.hmrc.tradereportingextracts.connectors.CustomsDataStoreConnector
import uk.gov.hmrc.tradereportingextracts.models.{AddressInformation, AuthorisedUser, CompanyInformation, NotificationEmail, User, UserDetails}
import uk.gov.hmrc.tradereportingextracts.models.etmp.EoriUpdate
import uk.gov.hmrc.tradereportingextracts.models.thirdParty.ThirdPartyAddedConfirmation
import uk.gov.hmrc.tradereportingextracts.repositories.UserRepository

import java.time.{Instant, LocalDateTime}
import scala.concurrent.{ExecutionContext, Future}

class UserServiceSpec
    extends AnyFreeSpec
    with Matchers
    with TryValues
    with OptionValues
    with ScalaFutures
    with IntegrationPatience {

  implicit val ec: ExecutionContext = ExecutionContext.global

  "UserService" - {

    val mockRepository                = mock[UserRepository]
    val mockCustomsDataStoreConnector = mock[CustomsDataStoreConnector]

    val service = new UserService(mockRepository, mockCustomsDataStoreConnector)

    val eori            = "EORI1234"
    val authorisedEoris = Seq("AUTH-EORI-1", "AUTH-EORI-2")

    "insert" - {

      "must insert user when repository returns true" in {
        val user = User(eori, additionalEmails = Seq(), authorisedUsers = Seq.empty)
        when(mockRepository.insert(user)).thenReturn(Future.successful(true))

        val result = service.insert(user)

        result.futureValue mustEqual true
        verify(mockRepository).insert(user)
      }

      "must fail when repository returns false" in {
        val user = User(eori, additionalEmails = Seq(), authorisedUsers = Seq())
        when(mockRepository.insert(user)).thenReturn(Future.successful(false))

        val result = service.insert(user)

        result.futureValue mustEqual false
      }
    }

    "update" - {

      "must update user when repository returns true" in {
        val user = User(eori, additionalEmails = Seq(), authorisedUsers = Seq.empty)
        when(mockRepository.update(user)).thenReturn(Future.successful(true))

        val result = service.update(user)

        result.futureValue mustEqual true
        verify(mockRepository).update(user)
      }

      "must fail when repository returns false" in {
        val user = User(eori, additionalEmails = Seq(), authorisedUsers = Seq())
        when(mockRepository.update(user)).thenReturn(Future.successful(false))

        val result = service.update(user)

        result.futureValue mustEqual false
      }
    }

    "updateEori" - {

      "must update EORI when repository returns true" in {
        val eoriUpdate = EoriUpdate("EORI1234", "EORI5678")
        when(mockRepository.updateEori(eoriUpdate)).thenReturn(Future.successful(true))

        val result = service.updateEori(eoriUpdate)

        result.futureValue mustEqual true
        verify(mockRepository).updateEori(eoriUpdate)
      }

      "must fail when repository returns false" in {
        val eoriUpdate = EoriUpdate("EORI1234", "EORI5678")
        when(mockRepository.updateEori(eoriUpdate)).thenReturn(Future.successful(false))

        val result = service.updateEori(eoriUpdate)

        result.futureValue mustEqual false
      }
    }

    "deleteByEori" - {

      "must delete user when repository returns true" in {
        when(mockRepository.deleteByEori(eori)).thenReturn(Future.successful(true))

        val result = service.deleteByEori(eori)

        result.futureValue mustEqual true
        verify(mockRepository).deleteByEori(eori)
      }

      "must fail when repository returns false" in {
        when(mockRepository.deleteByEori(eori)).thenReturn(Future.successful(false))

        val result = service.deleteByEori(eori)

        result.futureValue mustEqual false
      }
    }

    "getAuthorisedEoris" - {

      "must return authorised EORIs when repository returns them" in {
        when(mockRepository.getAuthorisedEoris(eori)).thenReturn(Future.successful(authorisedEoris))

        val result = service.getAuthorisedEoris(eori)

        result.futureValue mustEqual authorisedEoris
        verify(mockRepository).getAuthorisedEoris(eori)
      }

      "must fail when repository returns failed Future" in {
        val expectedException = new Exception("User not found")
        when(mockRepository.getAuthorisedEoris(eori)).thenReturn(Future.failed(expectedException))

        val result = service.getAuthorisedEoris(eori)

        whenReady(result.failed) { ex =>
          ex mustEqual expectedException
        }
      }
    }

    "getNotificationEmail" - {
      val eori              = "EORI1234"
      val notificationEmail = NotificationEmail("test@email.com")

      "must return notification email when connector returns it" in {
        when(mockCustomsDataStoreConnector.getNotificationEmail(eori)).thenReturn(Future.successful(notificationEmail))
        val result = service.getNotificationEmail(eori)
        result.futureValue mustEqual notificationEmail
      }

      "must fail when connector returns failed Future" in {
        val expectedException = new Exception("Email not found")
        when(mockCustomsDataStoreConnector.getNotificationEmail(eori)).thenReturn(Future.failed(expectedException))
        val result            = service.getNotificationEmail(eori)
        whenReady(result.failed) { ex =>
          ex mustEqual expectedException
        }
      }
    }

    "getUserAndEmailDetails" - {

      "must return user details with notification email when both user and email are found" in {
        val user               = User(eori, additionalEmails = Seq(), authorisedUsers = Seq.empty)
        val companyInformation = CompanyInformation(
          name = "Test Company",
          consent = "Yes"
        )
        val notificationEmail  =
          NotificationEmail("test@test.com", LocalDateTime.now())
        when(mockCustomsDataStoreConnector.getCompanyInformation(eori))
          .thenReturn(Future.successful(companyInformation))
        when(mockRepository.getOrCreateUser(eori)).thenReturn(Future.successful(user))
        when(mockCustomsDataStoreConnector.getNotificationEmail(eori)).thenReturn(Future.successful(notificationEmail))
        val result             = service.getUserAndEmailDetails(eori)
        result.futureValue mustEqual UserDetails(
          eori = user.eori,
          additionalEmails = user.additionalEmails,
          authorisedUsers = user.authorisedUsers,
          companyInformation = companyInformation,
          notificationEmail = notificationEmail
        )
      }

      "must return user details without notification email when email is not found" in {
        val user               = User(eori, additionalEmails = Seq(), authorisedUsers = Seq.empty)
        val companyInformation = CompanyInformation(
          name = "Test Company",
          consent = "1",
          address = AddressInformation()
        )
        val notificationEmail  = NotificationEmail()
        when(mockCustomsDataStoreConnector.getCompanyInformation(eori))
          .thenReturn(Future.successful(companyInformation))
        when(mockRepository.getOrCreateUser(eori)).thenReturn(Future.successful(user))
        when(mockCustomsDataStoreConnector.getNotificationEmail(eori)).thenReturn(Future.successful(notificationEmail))
        val result             = service.getUserAndEmailDetails(eori)
        result.futureValue mustEqual UserDetails(
          eori = user.eori,
          additionalEmails = user.additionalEmails,
          authorisedUsers = user.authorisedUsers,
          companyInformation = companyInformation,
          notificationEmail = notificationEmail
        )
      }
    }

    "addAuthorisedUser" - {
      "should return confirmation when user is added" in {
        val eori           = "GB123456789000"
        val authorisedUser = AuthorisedUser(
          eori = "GB123456789001",
          accessStart = Instant.parse("2024-01-01T00:00:00Z"),
          accessEnd = Some(Instant.parse("2024-12-31T23:59:59Z")),
          reportDataStart = Some(Instant.parse("2024-01-01T10:00:00Z")),
          reportDataEnd = Some(Instant.parse("2024-12-31T23:59:59Z")),
          accessType = Set.empty
        )
        val confirmation   = ThirdPartyAddedConfirmation(
          thirdPartyEori = "GB123456789001"
        )

        when(mockRepository.addAuthorisedUser(any(), any()))
          .thenReturn(Future.successful(confirmation))

        val result = service.addAuthorisedUser(eori, authorisedUser)
        whenReady(result) { res =>
          res mustBe confirmation
        }
      }

      "should fail if repository fails" in {
        val eori           = "GB123456789000"
        val authorisedUser = AuthorisedUser(
          eori = "GB123456789001",
          accessStart = Instant.parse("2024-01-01T00:00:00Z"),
          accessEnd = Some(Instant.parse("2024-12-31T23:59:59Z")),
          reportDataStart = Some(Instant.parse("2024-01-01T10:00:00Z")),
          reportDataEnd = Some(Instant.parse("2024-12-31T23:59:59Z")),
          accessType = Set.empty
        )

        when(mockRepository.addAuthorisedUser(any(), any()))
          .thenReturn(Future.failed(new Exception("User not found")))

        val result = service.addAuthorisedUser(eori, authorisedUser)
        whenReady(result.failed) { ex =>
          ex mustBe an[Exception]
          ex.getMessage must include("User not found")
        }
      }
    }
  }
}
