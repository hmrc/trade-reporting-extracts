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

package uk.gov.hmrc.tradereportingextracts.repositories

import org.mockito.Mockito.{verify, when}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.IntegrationPatience
import org.scalatest.matchers.must.Matchers.{must, mustBe, mustEqual}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import uk.gov.hmrc.mongo.test.CleanMongoCollectionSupport
import uk.gov.hmrc.tradereportingextracts.config.AppConfig
import uk.gov.hmrc.tradereportingextracts.models.AccessType.{EXPORTS, IMPORTS}
import uk.gov.hmrc.tradereportingextracts.models.etmp.EoriUpdate
import uk.gov.hmrc.tradereportingextracts.models.thirdParty.ThirdPartyAddedConfirmation
import uk.gov.hmrc.tradereportingextracts.models.{AuthorisedUser, User, UserActiveStatus}
import uk.gov.hmrc.tradereportingextracts.services.UserService

import java.time.temporal.ChronoUnit
import java.time.{Clock, Instant, LocalDate, LocalDateTime, ZoneOffset}
import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global

class UserRepositorySpec
    extends AnyWordSpec,
      MockitoSugar,
      GuiceOneAppPerSuite,
      CleanMongoCollectionSupport,
      Matchers,
      IntegrationPatience,
      BeforeAndAfterAll:

  val appConfig: AppConfig = app.injector.instanceOf[AppConfig]

  val clock: Clock         = Clock.fixed(Instant.parse("2025-10-09T00:00:00Z"), ZoneOffset.UTC)
  val today: LocalDateTime = LocalDate.now(clock).atStartOfDay()

  val userRepository: UserRepository = UserRepository(appConfig, mongoComponent)
  val user: User                     = User(
    eori = "EORI1234",
    additionalEmails = Seq("asd@gmail.com", "dfsf@gmail.com"),
    authorisedUsers = Seq(
      AuthorisedUser(
        eori = "AUTH-EORI-1",
        accessStart = Instant.parse("2023-01-01T00:00:00Z"),
        accessEnd = Some(Instant.parse("2023-12-31T23:59:59Z")),
        reportDataStart = Some(Instant.parse("2023-01-01T10:00:00Z")),
        reportDataEnd = Some(Instant.parse("2023-12-31T23:59:59Z")),
        accessType = Set(IMPORTS)
      ),
      AuthorisedUser(
        eori = "AUTH-EORI-2",
        accessStart = Instant.parse("2023-01-01T00:00:00Z"),
        accessEnd = Some(Instant.parse("2023-12-31T23:59:59Z")),
        reportDataStart = Some(Instant.parse("2023-01-01T10:00:00Z")),
        reportDataEnd = Some(Instant.parse("2023-12-31T23:59:59Z")),
        accessType = Set(IMPORTS)
      )
    ),
    accessDate = Instant.parse("2023-01-01T00:00:00Z")
  )

  "UserRepositorySpec" should {

    //    "insertUser with TTL" should {
    //      "must insert a user with TTL successfully" in {
    //        implicit val patienceConfig: PatienceConfig = PatienceConfig(timeout = 70.seconds, interval = 1.second)
    //
    //        val result = for {
    //          _             <- createIndex(mongoComponent.database, "tre-user", "accessDate", "accessDate-ttl-index")
    //          insertResult  <- userRepository.insert(user)
    //          _              = Thread.sleep(65000)
    //          fetchedRecord <- userRepository.findByEori(user.eori)
    //        } yield {
    //          insertResult mustEqual true
    //          fetchedRecord mustBe None
    //        }
    //        result.futureValue
    //      }
    //    }

    "insertUser" should {
      "must insert a user successfully" in {
        val insertResult = userRepository.insert(user).futureValue
        insertResult mustEqual true
      }
    }

    "findByUserid" should {
      "must be able to retrieve a user successfully using a userid" in {
        val insertResult  = userRepository.insert(user).futureValue
        val fetchedRecord = userRepository.findByEori(user.eori).futureValue
        insertResult mustEqual true
        fetchedRecord.get mustEqual user
      }

      "must return none if eori not found" in {
        val insertResult  = userRepository.insert(user).futureValue
        val fetchedRecord = userRepository.findByEori("nonExistingEori").futureValue
        insertResult mustEqual true
        fetchedRecord must be(None)
      }
    }

    "updateByUserEori" should {
      "must be able to update an existing user" in {
        val eoriNew                   = "EORI-NEW"
        val insertResult              = userRepository.insert(user).futureValue
        val fetchedBeforeUpdateRecord = userRepository.findByEori(user.eori).futureValue
        val updatedRecord             = userRepository.updateEori(EoriUpdate(eoriNew, user.eori)).futureValue
        val fetchedRecord             = userRepository.findByEori(eoriNew).futureValue
        insertResult mustEqual true
        fetchedBeforeUpdateRecord.get mustEqual user
        updatedRecord mustEqual true
        fetchedRecord.get.eori mustEqual eoriNew
      }
    }

    "updateAuthorisedUserEori" should {
      "must be able to update an existing authorised user" in {
        val eoriNew                   = "EORI-NEW"
        val insertResult              = userRepository.insert(user).futureValue
        val fetchedBeforeUpdateRecord = userRepository.findByEori(user.eori).futureValue
        val updatedRecord             =
          userRepository.updateAuthorisedUserEori(EoriUpdate(eoriNew, user.authorisedUsers.head.eori)).futureValue
        val fetchedRecord             = userRepository.findByEori(user.eori).futureValue
        insertResult mustEqual true
        fetchedBeforeUpdateRecord.get mustEqual user
        updatedRecord mustEqual true
        fetchedRecord.get.authorisedUsers.head.eori mustEqual eoriNew
      }
    }

    "deleteByUserId" should {
      "must be able to delete an existing user" in {
        val insertResult              = userRepository.insert(user).futureValue
        val fetchedBeforeUpdateRecord = userRepository.findByEori(user.eori).futureValue
        val deletedRecord             = userRepository.deleteByEori(user.eori).futureValue
        insertResult mustEqual true
        fetchedBeforeUpdateRecord.get mustEqual user
        deletedRecord mustEqual true
      }
    }

    "getAuthorisedEoris" should {

      "must return all authorised EORIs for a given user" in {
        userRepository.insert(user).futureValue
        val result = userRepository.getAuthorisedEoris(user.eori).futureValue

        result mustEqual Seq("AUTH-EORI-1", "AUTH-EORI-2")
      }

      "must fail if user is not found for given EORI" in {
        val result = userRepository.getAuthorisedEoris("non-existent-eori")

        whenReady(result.failed) { ex =>
          ex mustBe an[Exception]
          ex.getMessage mustEqual "User with EORI non-existent-eori not found"
        }
      }
    }

    "getOrCreateUser" should {
      "must create a new user if it does not exist" in {
        val eori              = "NEW-EORI"
        val (result, isExist) = userRepository.getOrCreateUser(eori).futureValue
        result.eori mustEqual eori
        isExist mustEqual false
      }

      "must return existing user with updatd accessDate if it exists" in {
        val insertResult      = userRepository.insert(user).futureValue
        val (result, isExist) = userRepository.getOrCreateUser(user.eori).futureValue
        insertResult mustEqual true
        result.accessDate.compareTo(Instant.now().minusSeconds(1)) >= 0
        isExist mustEqual true
      }
    }

    "keepAlive" should {
      "update accessDate for existing user and return true" in {
        val existingUser = user.copy(accessDate = Instant.now().minus(1, ChronoUnit.HOURS))
        userRepository.insert(existingUser).futureValue

        val result = userRepository.keepAlive(existingUser.eori).futureValue
        result mustBe true

        // Verify the accessDate was updated
        val updatedUser = userRepository.findByEori(existingUser.eori).futureValue.get
        updatedUser.accessDate.compareTo(Instant.now().minusSeconds(5)) >= 0 mustBe true
      }

      "return false when user does not exist" in {
        val result = userRepository.keepAlive("non-existent-eori").futureValue
        result mustBe false
      }
    }

    "addAuthorisedUser" should {
      "add a new authorised user to an existing user" in {
        val baseUser = user.copy(authorisedUsers = Seq.empty)
        userRepository.insert(baseUser).futureValue

        val newAuthorisedUser = AuthorisedUser(
          eori = "GB987654321098",
          accessStart = Instant.parse("2024-01-01T00:00:00Z"),
          accessEnd = Some(Instant.parse("2024-12-31T23:59:59Z")),
          reportDataStart = Some(Instant.parse("2024-01-01T10:00:00Z")),
          reportDataEnd = Some(Instant.parse("2024-12-31T23:59:59Z")),
          accessType = Set(IMPORTS)
        )

        val confirmation = userRepository.addAuthorisedUser(baseUser.eori, newAuthorisedUser).futureValue
        confirmation.thirdPartyEori mustBe "GB987654321098"

      }

      "fail if the user does not exist" in {
        val nonExistentEori   = "NON-EXISTENT-EORI"
        val newAuthorisedUser = AuthorisedUser(
          eori = "AUTH-EORI-NEW",
          accessStart = Instant.parse("2024-01-01T00:00:00Z"),
          accessEnd = Some(Instant.parse("2024-12-31T23:59:59Z")),
          reportDataStart = Some(Instant.parse("2024-01-01T10:00:00Z")),
          reportDataEnd = Some(Instant.parse("2024-12-31T23:59:59Z")),
          accessType = Set(IMPORTS)
        )

        val result = userRepository.addAuthorisedUser(nonExistentEori, newAuthorisedUser)
        whenReady(result.failed) { ex =>
          ex mustBe an[Exception]
          ex.getMessage must include(nonExistentEori)
        }
      }
    }

    "getAuthorisedUser" should {
      "return authorised user if there is one" in {

        val eori           = "EORI1234"
        val thirdPartyEori = "AUTH-EORI-1"
        userRepository.insert(user).futureValue
        val result         = userRepository.getAuthorisedUser(eori, thirdPartyEori).futureValue
        result mustBe Some(
          AuthorisedUser(
            eori = "AUTH-EORI-1",
            accessStart = Instant.parse("2023-01-01T00:00:00Z"),
            accessEnd = Some(Instant.parse("2023-12-31T23:59:59Z")),
            reportDataStart = Some(Instant.parse("2023-01-01T10:00:00Z")),
            reportDataEnd = Some(Instant.parse("2023-12-31T23:59:59Z")),
            accessType = Set(IMPORTS)
          )
        )
      }

      "return none if no authorised user found" in {
        val eori           = "EORI1234"
        val thirdPartyEori = "NON-AUTH-EORI"
        val result         = userRepository.getAuthorisedUser(eori, thirdPartyEori).futureValue
        result mustBe None
      }
    }

    "getUsersByAuthorisedEori" should {
      "return users who have authorised a specific EORI" in {

        val user1 = User(
          eori = "EORI1",
          authorisedUsers = Seq(
            AuthorisedUser(
              eori = "AUTH-EORI-1",
              accessStart = Instant.parse("2023-01-01T00:00:00Z"),
              accessEnd = Some(Instant.parse("2023-12-31T23:59:59Z")),
              reportDataStart = Some(Instant.parse("2023-01-01T10:00:00Z")),
              reportDataEnd = Some(Instant.parse("2023-12-31T23:59:59Z")),
              accessType = Set(IMPORTS)
            ),
            AuthorisedUser(
              eori = "AUTH-EORI-2",
              accessStart = Instant.parse("2023-01-01T00:00:00Z"),
              accessEnd = Some(Instant.parse("2023-12-31T23:59:59Z")),
              reportDataStart = Some(Instant.parse("2023-01-01T10:00:00Z")),
              reportDataEnd = Some(Instant.parse("2023-12-31T23:59:59Z")),
              accessType = Set(IMPORTS)
            )
          ),
          accessDate = Instant.parse("2023-01-01T00:00:00Z")
        )

        userRepository.insert(user).futureValue
        userRepository.insert(user1).futureValue
        val result = userRepository.getUsersByAuthorisedEori("AUTH-EORI-1").futureValue
        result.map(_.eori) must contain theSameElementsAs Seq("EORI1234", "EORI1")
      }
    }

    "getUsersByAuthorisedEoriWithStatus" should {
      val cutoffDate = today.minusDays(3)
      "return users who have authorised a specific EORI with correct status" in {
        val accessStart     = today.minusDays(1).toInstant(ZoneOffset.UTC)
        val accessEnd       = today.plusDays(5).toInstant(ZoneOffset.UTC)
        val reportDataStart = cutoffDate.toInstant(ZoneOffset.UTC)

        val authorisedEori = "AUTH-EORI-1"

        val user1 = User(
          eori = "EORI1",
          authorisedUsers = Seq(
            AuthorisedUser(
              eori = authorisedEori,
              accessStart = accessStart,
              accessEnd = Some(accessEnd),
              reportDataStart = Some(reportDataStart),
              reportDataEnd = Some(accessEnd),
              accessType = Set(IMPORTS)
            )
          ),
          accessDate = Instant.parse("2023-01-01T00:00:00Z")
        )

        val user2 = User(
          eori = "EORI2",
          authorisedUsers = Seq(
            AuthorisedUser(
              eori = "AUTH-EORI-2",
              accessStart = Instant.parse("2023-01-01T00:00:00Z"),
              accessEnd = Some(Instant.parse("2023-12-31T23:59:59Z")),
              reportDataStart = Some(Instant.parse("2023-01-01T10:00:00Z")),
              reportDataEnd = Some(Instant.parse("2023-12-31T23:59:59Z")),
              accessType = Set(IMPORTS)
            )
          ),
          accessDate = Instant.parse("2023-01-01T00:00:00Z")
        )

        val user3 = User(
          eori = "EORI3",
          authorisedUsers = Seq(
            AuthorisedUser(
              eori = authorisedEori,
              accessStart = accessStart,
              accessEnd = Some(accessEnd),
              reportDataStart = Some(reportDataStart),
              reportDataEnd = Some(accessEnd),
              accessType = Set(IMPORTS)
            )
          ),
          accessDate = Instant.parse("2023-01-01T00:00:00Z")
        )

        userRepository.insert(user1).futureValue
        userRepository.insert(user2).futureValue
        userRepository.insert(user3).futureValue

        val result = userRepository.getUsersByAuthorisedEoriWithStatus(authorisedEori).futureValue

        result.map(_.user.eori) mustBe List("EORI1", "EORI3")
        result.map(_.status) must contain only UserActiveStatus.Active
      }

      "return empty if authorised user is not found" in {
        val authorisedEori = "AUTH-EORI-3"

        val user = User(
          eori = "EORI3",
          authorisedUsers = Seq.empty,
          accessDate = Instant.parse("2023-01-01T00:00:00Z")
        )

        userRepository.insert(user).futureValue

        val result = userRepository.getUsersByAuthorisedEoriWithStatus(authorisedEori).futureValue

        result mustBe empty
      }
    }

    "deleteAuthorisedUser" should {
      "should return true when repository deletion succeeds" in {
        val repo                    = mock[UserRepository]
        val cds                     = mock[uk.gov.hmrc.tradereportingextracts.connectors.CustomsDataStoreConnector]
        val reportRequestRepository = mock[ReportRequestRepository]
        val service                 = new UserService(repo, reportRequestRepository, cds)
        val eori                    = "GB987654321098"
        val thirdEori               = "GB123456123456"

        org.mockito.Mockito
          .when(repo.deleteAuthorisedUser(eori, thirdEori))
          .thenReturn(scala.concurrent.Future.successful(true))

        whenReady(service.deleteAuthorisedUser(eori, thirdEori)) { result =>
          result mustBe true
          org.mockito.Mockito.verify(repo).deleteAuthorisedUser(eori, thirdEori)
        }
      }

      "should return false when repository indicates not found" in {
        val repo                    = mock[UserRepository]
        val cds                     = mock[uk.gov.hmrc.tradereportingextracts.connectors.CustomsDataStoreConnector]
        val reportRequestRepository = mock[ReportRequestRepository]
        val service                 = new UserService(repo, reportRequestRepository, cds)
        val eori                    = "GB987654321098"
        val thirdEori               = "GB000000000000"

        org.mockito.Mockito
          .when(repo.deleteAuthorisedUser(eori, thirdEori))
          .thenReturn(scala.concurrent.Future.successful(false))

        whenReady(service.deleteAuthorisedUser(eori, thirdEori)) { result =>
          result mustBe false
          verify(repo).deleteAuthorisedUser(eori, thirdEori)
        }
      }

      "should fail the future when repository fails" in {
        val repo                    = mock[UserRepository]
        val cds                     = mock[uk.gov.hmrc.tradereportingextracts.connectors.CustomsDataStoreConnector]
        val reportRequestRepository = mock[ReportRequestRepository]
        val service                 = new UserService(repo, reportRequestRepository, cds)
        val eori                    = "GB987654321098"
        val thirdEori               = "GB123456123456"

        when(
          repo.deleteAuthorisedUser(
            org.mockito.ArgumentMatchers.any[String],
            org.mockito.ArgumentMatchers.any[String]
          )
        ).thenReturn(scala.concurrent.Future.failed(new Exception("failure")))

        whenReady(service.deleteAuthorisedUser(eori, thirdEori).failed) { ex =>
          ex.getMessage must include("failure")
        }
      }
    }

    "getUsersByAuthorisedEoriWithDateFilter" should {
      val fixedInstant: Instant = LocalDate.of(2025, 9, 26).atStartOfDay(ZoneOffset.UTC).toInstant
      val fixedClock: Clock     = Clock.fixed(fixedInstant, ZoneOffset.UTC)

      val now      = fixedInstant // 2025-09-26T00:00:00Z
      val t2Cutoff = LocalDate.of(2025, 9, 23).atStartOfDay(ZoneOffset.UTC).toInstant

      def insertTestUsers(): Unit = {
        val user1 = User(
          eori = "GB123456124444",
          authorisedUsers = Seq(
            AuthorisedUser(
              eori = "GB123456789011",
              accessStart = now.minus(1, ChronoUnit.DAYS), // 1 day before now
              accessEnd = Some(now.plus(92, ChronoUnit.DAYS)), // 92 days after now
              reportDataStart = Some(now.minus(3, ChronoUnit.DAYS)), // before t2Cutoff
              reportDataEnd = None,
              accessType = Set(IMPORTS, EXPORTS)
            )
          ),
          accessDate = now
        )

        val user2 = User(
          eori = "GB123456789011",
          authorisedUsers = Seq(),
          accessDate = now
        )

        val user3 = User(
          eori = "GB999999999999",
          authorisedUsers = Seq(
            AuthorisedUser(
              eori = "GB123456789011",
              accessStart = now.minus(1, ChronoUnit.DAYS), // 1 day before now
              accessEnd = None, // open-ended
              reportDataStart = None, // missing
              reportDataEnd = None,
              accessType = Set(IMPORTS)
            )
          ),
          accessDate = now
        )

        val user4 = User(
          eori = "GB888888888888",
          authorisedUsers = Seq(
            AuthorisedUser(
              eori = "GB123456789011",
              accessStart = now.minus(1, ChronoUnit.DAYS),
              accessEnd = Some(now.minus(0, ChronoUnit.DAYS)), // already ended
              reportDataStart = Some(now.minus(1, ChronoUnit.DAYS)), // after t2Cutoff
              reportDataEnd = None,
              accessType = Set(IMPORTS)
            )
          ),
          accessDate = now
        )

        val user5 = User(
          eori = "GB777777777777",
          authorisedUsers = Seq(
            AuthorisedUser(
              eori = "GB000000000000",
              accessStart = now.minus(1, ChronoUnit.DAYS),
              accessEnd = None,
              reportDataStart = Some(t2Cutoff.minus(1, ChronoUnit.DAYS)), // before t2Cutoff
              reportDataEnd = None,
              accessType = Set(IMPORTS)
            )
          ),
          accessDate = now
        )

        Seq(user1, user2, user3, user4, user5).foreach(u => userRepository.insert(u).futureValue)
      }

      "return users with authorisedEori and valid date filters" in {
        insertTestUsers()
        val result = userRepository.getUsersByAuthorisedEoriWithDateFilter("GB123456789011", fixedClock).futureValue
        val eoris  = result.map(_.eori)
        eoris must contain allOf ("GB123456124444", "GB999999999999")
        eoris must not contain "GB888888888888"
        eoris must not contain "GB123456789011"
        eoris must not contain "GB777777777777"
      }

      "not return users if authorisedEori does not match" in {
        insertTestUsers()
        val result = userRepository.getUsersByAuthorisedEoriWithDateFilter("GB000000000000", fixedClock).futureValue
        result.map(_.eori) must contain only "GB777777777777"
      }

      "not return users if reportDataStart is after cutoff" in {
        val user   = User(
          eori = "GB666666666666",
          authorisedUsers = Seq(
            AuthorisedUser(
              eori = "GB123456789011",
              accessStart = now.minus(1, ChronoUnit.DAYS),
              accessEnd = None,
              reportDataStart = Some(now.minus(2, ChronoUnit.DAYS)), // after t2Cutoff
              reportDataEnd = None,
              accessType = Set(IMPORTS)
            )
          ),
          accessDate = now
        )
        userRepository.insert(user).futureValue
        val result = userRepository.getUsersByAuthorisedEoriWithDateFilter("GB123456789011", fixedClock).futureValue
        result.map(_.eori) must not contain "GB666666666666"
      }

      "return empty if no users match" in {
        val result = userRepository.getUsersByAuthorisedEoriWithDateFilter("NON-EXISTENT-EORI", fixedClock).futureValue
        result mustBe empty
      }

      "not return users if accessStart is after now" in {
        val user   = User(
          eori = "GB222222222222",
          authorisedUsers = Seq(
            AuthorisedUser(
              eori = "GB123456789011",
              accessStart = now.plus(1, ChronoUnit.DAYS), // future start
              accessEnd = None,
              reportDataStart = Some(t2Cutoff.minus(1, ChronoUnit.DAYS)),
              reportDataEnd = None,
              accessType = Set(IMPORTS)
            )
          ),
          accessDate = now
        )
        userRepository.insert(user).futureValue
        val result = userRepository.getUsersByAuthorisedEoriWithDateFilter("GB123456789011", fixedClock).futureValue
        result.map(_.eori) must not contain "GB222222222222"
      }

      "not return users if accessEnd is before now" in {
        val user   = User(
          eori = "GB333333333333",
          authorisedUsers = Seq(
            AuthorisedUser(
              eori = "GB123456789011",
              accessStart = now.minus(10, ChronoUnit.DAYS),
              accessEnd = Some(now.minus(1, ChronoUnit.DAYS)), // already ended
              reportDataStart = Some(t2Cutoff.minus(1, ChronoUnit.DAYS)),
              reportDataEnd = None,
              accessType = Set(IMPORTS)
            )
          ),
          accessDate = now
        )
        userRepository.insert(user).futureValue
        val result = userRepository.getUsersByAuthorisedEoriWithDateFilter("GB123456789011", fixedClock).futureValue
        result.map(_.eori) must not contain "GB333333333333"
      }
    }

    "update authorised user" should {

      val updatedAuthorisedUser = AuthorisedUser(
        eori = "AUTH-EORI-1",
        accessStart = Instant.parse("2024-01-01T00:00:00Z"),
        accessEnd = Some(Instant.parse("2024-12-31T23:59:59Z")),
        reportDataStart = Some(Instant.parse("2024-01-01T10:00:00Z")),
        reportDataEnd = Some(Instant.parse("2024-12-31T23:59:59Z")),
        accessType = Set(IMPORTS, EXPORTS),
        referenceName = Some("Updated Reference")
      )

      "must update an existing authorised user successfully" in {
        val insertResult  = userRepository.insert(user).futureValue
        val updatedRecord = userRepository.updateAuthorisedUser(user.eori, updatedAuthorisedUser).futureValue
        val fetchedRecord = userRepository.getAuthorisedUser(user.eori, "AUTH-EORI-1").futureValue
        insertResult mustEqual true
        updatedRecord mustEqual ThirdPartyAddedConfirmation("AUTH-EORI-1")
        fetchedRecord.get mustEqual updatedAuthorisedUser
      }

      "must not update other authorised users when updating one" in {
        val insertResult        = userRepository.insert(user).futureValue
        val updatedRecord       = userRepository.updateAuthorisedUser(user.eori, updatedAuthorisedUser).futureValue
        val fetchedRecord       = userRepository.getAuthorisedUser(user.eori, "AUTH-EORI-2").futureValue
        val otherAuthorisedUser = AuthorisedUser(
          eori = "AUTH-EORI-2",
          accessStart = Instant.parse("2023-01-01T00:00:00Z"),
          accessEnd = Some(Instant.parse("2023-12-31T23:59:59Z")),
          reportDataStart = Some(Instant.parse("2023-01-01T10:00:00Z")),
          reportDataEnd = Some(Instant.parse("2023-12-31T23:59:59Z")),
          accessType = Set(IMPORTS)
        )
        insertResult mustEqual true
        updatedRecord mustEqual ThirdPartyAddedConfirmation("AUTH-EORI-1")
        fetchedRecord.get mustEqual otherAuthorisedUser
      }

      "Must fail if the user does not exist" in {
        val nonExistentEori = "foo"

        val result = userRepository.updateAuthorisedUser(nonExistentEori, updatedAuthorisedUser).failed.futureValue
        result mustBe an[Exception]
      }
    }
  }
