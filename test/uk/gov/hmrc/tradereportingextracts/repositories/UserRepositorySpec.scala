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

import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.IntegrationPatience
import org.scalatest.matchers.must.Matchers.{must, mustBe, mustEqual}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import uk.gov.hmrc.mongo.test.CleanMongoCollectionSupport
import uk.gov.hmrc.tradereportingextracts.config.AppConfig
import uk.gov.hmrc.tradereportingextracts.models.AccessType.IMPORTS
import uk.gov.hmrc.tradereportingextracts.models.etmp.EoriUpdate
import uk.gov.hmrc.tradereportingextracts.models.{AuthorisedUser, User}

import java.time.Instant
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

  val appConfig: AppConfig           = app.injector.instanceOf[AppConfig]
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
        val eori   = "NEW-EORI"
        val result = userRepository.getOrCreateUser(eori).futureValue
        result.eori mustEqual eori
      }

      "must return existing user with updatd accessDate if it exists" in {
        val insertResult = userRepository.insert(user).futureValue
        val result       = userRepository.getOrCreateUser(user.eori).futureValue
        insertResult mustEqual true
        result.accessDate.compareTo(Instant.now().minusSeconds(1)) >= 0
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
        val insertResult   = userRepository.insert(user).futureValue
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
  }
