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
import org.mockito.Mockito.{reset, verify, when}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.matchers.should.Matchers.shouldBe
import org.scalatest.{BeforeAndAfterEach, OptionValues, TryValues}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.mockito.MockitoSugar.mock
import uk.gov.hmrc.tradereportingextracts.connectors.CustomsDataStoreConnector
import uk.gov.hmrc.tradereportingextracts.models.*
import uk.gov.hmrc.tradereportingextracts.models.AccessType.IMPORTS
import uk.gov.hmrc.tradereportingextracts.models.etmp.EoriUpdate
import uk.gov.hmrc.tradereportingextracts.models.thirdParty.{EoriBusinessInfo, ThirdPartyAddedConfirmation}
import uk.gov.hmrc.tradereportingextracts.repositories.{ReportRequestRepository, UserRepository}

import java.time.{Instant, LocalDate, LocalDateTime}
import scala.concurrent.{ExecutionContext, Future}

class UserServiceSpec
    extends AnyFreeSpec
    with Matchers
    with TryValues
    with OptionValues
    with ScalaFutures
    with IntegrationPatience
    with BeforeAndAfterEach {

  implicit val ec: ExecutionContext = ExecutionContext.global

  val mockRepository                = mock[UserRepository]
  val mockCustomsDataStoreConnector = mock[CustomsDataStoreConnector]
  val mockReportRequestRepository   = mock[ReportRequestRepository]
  val mockAdditionEmailService      = mock[AdditionalEmailService]
  val service                       = new UserService(
    mockRepository,
    mockReportRequestRepository,
    mockCustomsDataStoreConnector,
    mockAdditionEmailService
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockRepository, mockCustomsDataStoreConnector, mockReportRequestRepository)
  }

  "UserService" - {

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
        when(mockRepository.updateAuthorisedUserEori(eoriUpdate)).thenReturn(Future.successful(true))

        val result = service.updateEori(eoriUpdate)

        result.futureValue mustEqual true
        verify(mockRepository).updateEori(eoriUpdate)
        verify(mockRepository).updateAuthorisedUserEori(eoriUpdate)
      }

      "must fail when repository returns false" in {
        val eoriUpdate = EoriUpdate("EORI1234", "EORI5678")
        when(mockRepository.updateEori(eoriUpdate)).thenReturn(Future.successful(false))
        when(mockRepository.updateAuthorisedUserEori(eoriUpdate)).thenReturn(Future.successful(false))

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

    "keepAlive" - {

      "must update user TTL when repository returns true" in {
        when(mockRepository.keepAlive(eori)).thenReturn(Future.successful(true))

        val result = service.keepAlive(eori)

        result.futureValue mustEqual true
        verify(mockRepository).keepAlive(eori)
      }

      "must fail when repository returns false" in {
        when(mockRepository.keepAlive(eori)).thenReturn(Future.successful(false))

        val result = service.keepAlive(eori)

        result.futureValue mustEqual false
        verify(mockRepository).keepAlive(eori)
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

    "getUsersByAuthorisedEoriWithStatus" - {

      "return EoriBusinessInfo with status and business info when consent is 1" in {
        val authorisedEori = "GB111111111111"
        val companyInfo    = CompanyInformation(name = "Test Ltd", consent = "1")
        val user           = User(
          eori = "GB123456789000",
          authorisedUsers = Seq(
            AuthorisedUser(
              eori = authorisedEori,
              accessStart = Instant.now(),
              accessEnd = None,
              reportDataStart = None,
              reportDataEnd = None,
              accessType = Set(IMPORTS),
              referenceName = None
            )
          )
        )

        val userWithStatus = UserWithStatus(user, UserActiveStatus.Active)

        when(mockRepository.getUsersByAuthorisedEoriWithStatus(authorisedEori))
          .thenReturn(Future.successful(Seq(userWithStatus)))
        when(mockCustomsDataStoreConnector.getCompanyInformation(user.eori)).thenReturn(Future.successful(companyInfo))

        val result = service.getUsersByAuthorisedEoriWithStatus(authorisedEori)

        result.futureValue shouldBe Seq(
          EoriBusinessInfo(
            eori = "GB123456789000",
            businessInfo = Some("Test Ltd"),
            status = Some(UserActiveStatus.Active)
          )
        )
      }

      "return EoriBusinessInfo with no business info when consent is not 1" in {
        val authorisedEori = "GB111111111111"
        val companyInfo    = CompanyInformation(name = "Test Ltd", consent = "0")
        val user           = User(
          eori = "GB123456789000",
          authorisedUsers = Seq(
            AuthorisedUser(
              eori = authorisedEori,
              accessStart = Instant.now(),
              accessEnd = None,
              reportDataStart = None,
              reportDataEnd = None,
              accessType = Set(IMPORTS),
              referenceName = None
            )
          )
        )

        val userWithStatus = UserWithStatus(user, UserActiveStatus.Active)

        when(mockRepository.getUsersByAuthorisedEoriWithStatus(authorisedEori))
          .thenReturn(Future.successful(Seq(userWithStatus)))
        when(mockCustomsDataStoreConnector.getCompanyInformation(user.eori)).thenReturn(Future.successful(companyInfo))

        val result = service.getUsersByAuthorisedEoriWithStatus(authorisedEori)

        result.futureValue shouldBe Seq(
          EoriBusinessInfo(
            eori = "GB123456789000",
            businessInfo = None,
            status = Some(UserActiveStatus.Active)
          )
        )
      }

      "fail when repository throws exception" in {
        val authorisedEori    = "GB111111111111"
        val expectedException = new Exception("Repository failure")

        when(mockRepository.getUsersByAuthorisedEoriWithStatus(authorisedEori))
          .thenReturn(Future.failed(expectedException))

        val result = service.getUsersByAuthorisedEoriWithStatus(authorisedEori)

        whenReady(result.failed) { ex =>
          ex shouldBe expectedException
        }
      }
    }

    "getUsersByAuthorisedEoriWithDateFilter" - {

      "return EoriBusinessInfo with status and business info when consent is 1" in {
        val authorisedEori = "GB111111111111"
        val companyInfo    = CompanyInformation(name = "Test Ltd", consent = "1")
        val user           = User(
          eori = "GB123456789000",
          authorisedUsers = Seq(
            AuthorisedUser(
              eori = authorisedEori,
              accessStart = Instant.now(),
              accessEnd = None,
              reportDataStart = None,
              reportDataEnd = None,
              accessType = Set(IMPORTS),
              referenceName = None
            )
          )
        )

        when(mockRepository.getUsersByAuthorisedEoriWithDateFilter(authorisedEori))
          .thenReturn(Future.successful(Seq(user)))
        when(mockCustomsDataStoreConnector.getCompanyInformation(user.eori)).thenReturn(Future.successful(companyInfo))

        val result = service.getUsersByAuthorisedEoriWithDateFilter(authorisedEori)

        result.futureValue shouldBe Seq(
          EoriBusinessInfo(
            eori = "GB123456789000",
            businessInfo = Some("Test Ltd"),
            status = None
          )
        )
      }

      "return EoriBusinessInfo with no business info when consent is not 1" in {
        val authorisedEori = "GB111111111111"
        val companyInfo    = CompanyInformation(name = "Test Ltd", consent = "0")
        val user           = User(
          eori = "GB123456789000",
          authorisedUsers = Seq(
            AuthorisedUser(
              eori = authorisedEori,
              accessStart = Instant.now(),
              accessEnd = None,
              reportDataStart = None,
              reportDataEnd = None,
              accessType = Set(IMPORTS),
              referenceName = None
            )
          )
        )

        when(mockRepository.getUsersByAuthorisedEoriWithDateFilter(authorisedEori))
          .thenReturn(Future.successful(Seq(user)))
        when(mockCustomsDataStoreConnector.getCompanyInformation(user.eori)).thenReturn(Future.successful(companyInfo))

        val result = service.getUsersByAuthorisedEoriWithDateFilter(authorisedEori)

        result.futureValue shouldBe Seq(
          EoriBusinessInfo(
            eori = "GB123456789000",
            businessInfo = None,
            status = None
          )
        )
      }

      "fail when repository throws exception" in {
        val authorisedEori    = "GB111111111111"
        val expectedException = new Exception("Repository failure")

        when(mockRepository.getUsersByAuthorisedEoriWithDateFilter(authorisedEori))
          .thenReturn(Future.failed(expectedException))

        val result = service.getUsersByAuthorisedEoriWithDateFilter(authorisedEori)

        whenReady(result.failed) { ex =>
          ex shouldBe expectedException
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
        when(mockRepository.getOrCreateUser(eori)).thenReturn(Future.successful(user, false))
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
        when(mockRepository.getOrCreateUser(eori)).thenReturn(Future.successful(user, false))
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

    "getAuthorisedUser" - {

      val eori           = "123"
      val thirdPartyEori = "456"

      val authorisedUser = AuthorisedUser(
        eori = thirdPartyEori,
        accessStart = Instant.parse("2023-01-01T00:00:00Z"),
        accessEnd = Some(Instant.parse("2023-12-31T23:59:59Z")),
        reportDataStart = Some(Instant.parse("2023-01-01T10:00:00Z")),
        reportDataEnd = Some(Instant.parse("2023-12-31T23:59:59Z")),
        accessType = Set(IMPORTS)
      )

      "must return authorised user when found" in {
        when(mockRepository.getAuthorisedUser(any(), any())).thenReturn(Future.successful(Some(authorisedUser)))

        val result = service.getAuthorisedUser(eori, thirdPartyEori).futureValue
        result mustBe Some(authorisedUser)
      }

      "return none when no authorised user found" in {
        when(mockRepository.getAuthorisedUser(any(), any())).thenReturn(Future.successful(None))

        val result = service.getAuthorisedUser(eori, thirdPartyEori).futureValue
        result mustBe None
      }
    }

    "getAuthorisedBusiness" - {

      val eori         = "123"
      val businessEori = "456"

      val authorisedUser = AuthorisedUser(
        eori = eori,
        accessStart = Instant.parse("2023-01-01T00:00:00Z"),
        accessEnd = Some(Instant.parse("2023-12-31T23:59:59Z")),
        reportDataStart = Some(Instant.parse("2023-01-01T10:00:00Z")),
        reportDataEnd = Some(Instant.parse("2023-12-31T23:59:59Z")),
        accessType = Set(IMPORTS)
      )

      "must return authorised user when found" in {
        when(mockRepository.getAuthorisedUser(any(), any())).thenReturn(Future.successful(Some(authorisedUser)))

        val result = service.getAuthorisedBusiness(eori, businessEori).futureValue
        result mustBe Some(authorisedUser)
      }

      "return none when no authorised user found" in {
        when(mockRepository.getAuthorisedUser(any(), any())).thenReturn(Future.successful(None))

        val result = service.getAuthorisedBusiness(eori, businessEori).futureValue
        result mustBe None
      }
    }

    "transformToThirdPartyDetails" - {

      val thirdPartyEori = "456"

      val authorisedUser = AuthorisedUser(
        eori = thirdPartyEori,
        accessStart = Instant.parse("2023-01-01T00:00:00Z"),
        accessEnd = Some(Instant.parse("2023-12-31T23:59:59Z")),
        reportDataStart = Some(Instant.parse("2023-01-01T10:00:00Z")),
        reportDataEnd = Some(Instant.parse("2023-12-31T23:59:59Z")),
        accessType = Set(IMPORTS),
        referenceName = Some("foo")
      )

      "should transform AuthorisedUser to ThirdPartyDetails correctly with optional dates" in {
        val result = service.transformToThirdPartyDetails(authorisedUser)

        result.referenceName mustBe Some("foo")
        result.accessStartDate mustBe LocalDate.of(2023, 1, 1)
        result.accessEndDate mustBe Some(LocalDate.of(2023, 12, 31))
        result.dataTypes mustBe Set("imports")
        result.dataStartDate mustBe Some(LocalDate.of(2023, 1, 1))
        result.dataEndDate mustBe Some(LocalDate.of(2023, 12, 31))
      }

      "should transform AuthorisedUser to ThirdPartyDetails correctly without optional dates" in {
        val authoriseduUserOngoing = authorisedUser.copy(accessEnd = None, reportDataStart = None, reportDataEnd = None)
        val result                 = service.transformToThirdPartyDetails(authoriseduUserOngoing)

        result.referenceName mustBe Some("foo")
        result.accessStartDate mustBe LocalDate.of(2023, 1, 1)
        result.accessEndDate mustBe None
        result.dataTypes mustBe Set("imports")
        result.dataStartDate mustBe None
        result.dataEndDate mustBe None
      }
    }

    "deleteAuthorisedUser" - {

      val eori           = "GB123456789000"
      val thirdPartyEori = "GB123456789001"

      "should return true when deletion succeeds" in {
        when(mockRepository.deleteAuthorisedUser(any(), any()))
          .thenReturn(Future.successful(true))

        val result = service.deleteAuthorisedUser(eori, thirdPartyEori).futureValue
        result mustBe true
      }

      "should return false when no user was deleted" in {
        when(mockRepository.deleteAuthorisedUser(any(), any()))
          .thenReturn(Future.successful(false))

        val result = service.deleteAuthorisedUser(eori, thirdPartyEori).futureValue
        result mustBe false
      }

      "should fail if repository fails" in {
        when(mockRepository.deleteAuthorisedUser(any(), any()))
          .thenReturn(Future.failed(new Exception("Delete failed")))

        val result = service.deleteAuthorisedUser(eori, thirdPartyEori)
        whenReady(result.failed) { ex =>
          ex mustBe an[Exception]
          ex.getMessage must include("Delete failed")
        }
      }
    }

    "cleanExpiredAccesses" - {
      "should delete expired authorised users and their reports" in {
        val now                = Instant.now()
        val expiredUser        = AuthorisedUser(
          eori = "AUTH-EORI-EXPIRED",
          accessStart = now.minusSeconds(3600),
          accessEnd = Some(now.minusSeconds(10)),
          reportDataStart = None,
          reportDataEnd = None,
          accessType = Set.empty
        )
        val userWithoutEndDate = AuthorisedUser(
          eori = "AUTH-EORI-ONGOING",
          accessStart = now.minusSeconds(3600),
          accessEnd = None,
          reportDataStart = None,
          reportDataEnd = None,
          accessType = Set.empty
        )
        val activeUser         = AuthorisedUser(
          eori = "AUTH-EORI-ACTIVE",
          accessStart = now.minusSeconds(3600),
          accessEnd = Some(now.plusSeconds(3600)),
          reportDataStart = None,
          reportDataEnd = None,
          accessType = Set.empty
        )
        val user               = User("EORI-TEST", Seq(), Seq(expiredUser, activeUser, userWithoutEndDate))

        when(mockRepository.getUsersByAuthorisedEori(user.eori)).thenReturn(Future.successful(Seq.empty))
        when(mockReportRequestRepository.deleteReportsForThirdPartyRemoval(user.eori, expiredUser.eori))
          .thenReturn(Future.successful(true))
        when(mockRepository.deleteAuthorisedUser(user.eori, expiredUser.eori)).thenReturn(Future.successful(true))

        service.cleanExpiredAccesses(user).futureValue

        verify(mockReportRequestRepository).deleteReportsForThirdPartyRemoval(user.eori, expiredUser.eori)
        verify(mockRepository).deleteAuthorisedUser(user.eori, expiredUser.eori)
        // Should not delete active user and user without an end date
        verify(mockReportRequestRepository, org.mockito.Mockito.never())
          .deleteReportsForThirdPartyRemoval(user.eori, activeUser.eori)
        verify(mockRepository, org.mockito.Mockito.never()).deleteAuthorisedUser(user.eori, activeUser.eori)
        verify(mockReportRequestRepository, org.mockito.Mockito.never())
          .deleteReportsForThirdPartyRemoval(user.eori, userWithoutEndDate.eori)
        verify(mockRepository, org.mockito.Mockito.never()).deleteAuthorisedUser(user.eori, userWithoutEndDate.eori)
      }

      "should delete expired authorised users from other traders and their reports" in {
        val now                    = Instant.now()
        val authorisedUser         = AuthorisedUser(
          eori = "AUTH-EORI-EXPIRED",
          accessStart = now.minusSeconds(3600),
          accessEnd = Some(now.minusSeconds(10)),
          reportDataStart = None,
          reportDataEnd = None,
          accessType = Set.empty
        )
        val authUserWithoutEndDate = AuthorisedUser(
          eori = "AUTH-EORI-ONGOING",
          accessStart = now.minusSeconds(3600),
          accessEnd = None,
          reportDataStart = None,
          reportDataEnd = None,
          accessType = Set.empty
        )
        val trader                 = User("EORI-TRADER", Seq(), Seq(authorisedUser, authUserWithoutEndDate))
        val user                   = User("AUTH-EORI-EXPIRED", Seq(), Seq())
        val userWithoutEndDate     = User("AUTH-EORI-ONGOING", Seq(), Seq())

        when(mockRepository.getUsersByAuthorisedEori(user.eori)).thenReturn(Future.successful(Seq(trader)))
        when(mockRepository.getUsersByAuthorisedEori(userWithoutEndDate.eori))
          .thenReturn(Future.successful(Seq(trader)))
        when(mockReportRequestRepository.deleteReportsForThirdPartyRemoval(trader.eori, authorisedUser.eori))
          .thenReturn(Future.successful(true))
        when(mockRepository.deleteAuthorisedUser(trader.eori, authorisedUser.eori)).thenReturn(Future.successful(true))

        service.cleanExpiredAccesses(user).futureValue

        verify(mockReportRequestRepository).deleteReportsForThirdPartyRemoval(trader.eori, authorisedUser.eori)
        verify(mockRepository).deleteAuthorisedUser(trader.eori, authorisedUser.eori)

        service.cleanExpiredAccesses(userWithoutEndDate).futureValue

        // Should not delete active user and user without an end date
        verify(mockReportRequestRepository, org.mockito.Mockito.never())
          .deleteReportsForThirdPartyRemoval(trader.eori, userWithoutEndDate.eori)
        verify(mockRepository, org.mockito.Mockito.never()).deleteAuthorisedUser(trader.eori, userWithoutEndDate.eori)
      }
    }

    "updateAuthorisedUser" - {

      val eori           = "GB1"
      val authorisedUser = AuthorisedUser(
        eori = "GB1",
        accessStart = Instant.parse("2024-01-01T00:00:00Z"),
        accessEnd = Some(Instant.parse("2024-12-31T23:59:59Z")),
        reportDataStart = Some(Instant.parse("2024-01-01T10:00:00Z")),
        reportDataEnd = Some(Instant.parse("2024-12-31T23:59:59Z")),
        accessType = Set(AccessType.IMPORTS)
      )

      "should return confirmation when user is updated" in {
        val confirmation = ThirdPartyAddedConfirmation(
          thirdPartyEori = "GB2"
        )
        when(mockRepository.updateAuthorisedUser(any(), any()))
          .thenReturn(Future.successful(confirmation))

        val result = service.updateAuthorisedUser(eori, authorisedUser).futureValue
        result mustBe confirmation
      }

      "should fail if repository fails" in {

        when(mockRepository.updateAuthorisedUser(any(), any()))
          .thenReturn(Future.failed(new Exception("User not found")))

        val result = service.updateAuthorisedUser(eori, authorisedUser).failed.futureValue
        result mustBe an[Exception]
        result.getMessage must include("User not found")
      }
    }
  }
}
