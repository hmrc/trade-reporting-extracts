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
import uk.gov.hmrc.tradereportingextracts.models.User

import scala.concurrent.ExecutionContext.Implicits.global

class UserRepositorySpec extends AnyWordSpec, MockitoSugar, GuiceOneAppPerSuite, CleanMongoCollectionSupport, Matchers:

  private val user  = User(123, "EORI1234", Array("asd@gmail.com", "dfsf@gmail.com"))
  private val user2 = User(123, "EORI1434", Array("asd@gmail.com", "dfsf@gmail.com"))

  val userRepository: UserRepository = UserRepository(mongoComponent)

  "insertUser" should {

    "must insert a user successfully" in {

      val insertResult = userRepository.insertUser(user).futureValue

      insertResult mustEqual true
    }
  }

  "findByUserid" should {

    "must be able to retrieve a user successfully using a userid" in {

      val insertResult  = userRepository.insertUser(user).futureValue
      val fetchedRecord = userRepository.findByUserId(user.userid).futureValue

      insertResult mustEqual true
      fetchedRecord.get mustEqual user
    }

    "must return none if uid not found" in {

      val insertResult  = userRepository.insertUser(user).futureValue
      val fetchedRecord = userRepository.findByUserId(23).futureValue

      insertResult mustEqual true
      fetchedRecord must be(None)
    }
  }

  "updateByUserId" should {

    "must be able to update an existing user" in {

      val insertResult              = userRepository.insertUser(user).futureValue
      val fetchedBeforeUpdateRecord = userRepository.findByUserId(user.userid).futureValue
      val updatedRecord             = userRepository.updateByUserId(user2).futureValue
      val fetchedRecord             = userRepository.findByUserId(user2.userid).futureValue

      insertResult mustEqual true
      fetchedBeforeUpdateRecord.get mustEqual user
      updatedRecord mustEqual true
      fetchedRecord.get mustEqual user2
    }
  }

  "deleteByUserId" should {

    "must be able to delete an existing user" in {

      val insertResult              = userRepository.insertUser(user).futureValue
      val fetchedBeforeUpdateRecord = userRepository.findByUserId(user.userid).futureValue
      val deletedRecord             = userRepository.deleteByUserId(user.userid).futureValue

      insertResult mustEqual true
      fetchedBeforeUpdateRecord.get mustEqual user
      deletedRecord mustEqual true
    }

  }
