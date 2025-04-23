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

import org.scalatest.matchers.must.Matchers.{must, mustEqual}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import uk.gov.hmrc.mongo.test.CleanMongoCollectionSupport
import uk.gov.hmrc.tradereportingextracts.models.AccessType.IMPORTS
import uk.gov.hmrc.tradereportingextracts.models.{AuthorisedUser, User, UserType}

import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global

class UserRepositorySpec extends AnyWordSpec, MockitoSugar, GuiceOneAppPerSuite, CleanMongoCollectionSupport, Matchers:

  val userRepository: UserRepository = UserRepository(mongoComponent)
  val user: User = User(eori = "EORI1234", additionalEmails = Seq("asd@gmail.com", "dfsf@gmail.com"),
    authorisedUsers = Seq(
      AuthorisedUser(
        eori = "EORI1234",
        accessStart = Instant.parse("2023-01-01T00:00:00Z"),
        accessEnd = Instant.parse("2023-12-31T23:59:59Z"),
        reportDataStart = Instant.parse("2023-01-01T10:00:00Z"),
        reportDataEnd = Instant.parse("2023-12-31T23:59:59Z"),
        accessType = IMPORTS
      )
    )
  )

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
      val eoriNew = "EORI-NEW"
      val insertResult              = userRepository.insert(user).futureValue
      val fetchedBeforeUpdateRecord = userRepository.findByEori(user.eori).futureValue
      val updatedRecord             = userRepository.updateEori(user.eori, eoriNew).futureValue
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
