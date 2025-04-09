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
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers.{must, mustEqual}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tradereportingextracts.models.{User, UserType}
import uk.gov.hmrc.tradereportingextracts.repositories.UserRepository

import scala.concurrent.{ExecutionContext, Future}

class UserServiceSpec extends AnyWordSpec, GuiceOneAppPerSuite, Matchers, ScalaFutures:

  given ExecutionContext = ExecutionContext.global

  given HeaderCarrier = HeaderCarrier()

  lazy val mockUserRepository: UserRepository = mock[UserRepository]

  private val userService =
    new UserService(mockUserRepository)

  private val user = User(123, "EORI1234", UserType.Trader, Array("asd@gmail.com", "dfsf@gmail.com"))

  "UserService" should {
    "insertUser" should {
      "must insert a user successfully" in {

        when(mockUserRepository.insertUser(user)).thenReturn(Future.successful(true))

        val result = userService.insertUser(user).futureValue

        result shouldBe true

        verify(mockUserRepository, times(1)).insertUser(any)(using any())
      }
    }

    "findByUserid" should {

      "must be able to retrieve a user successfully using a userid" in {

        when(mockUserRepository.findByUserId(user.userid)).thenReturn(Future.successful(Some(user)))

        val fetchedRecord = userService.findByUserId(user.userid).futureValue

        fetchedRecord.get mustEqual user
      }

      "must return none if uid not found" in {

        when(mockUserRepository.findByUserId(23)).thenReturn(Future.successful(None))

        val fetchedRecord = userService.findByUserId(23).futureValue

        fetchedRecord must be(None)
      }
    }

    "updateByUserId" should {

      "must be able to update an existing user" in {

        when(mockUserRepository.updateByUserId(user)).thenReturn(Future.successful(true))

        val updatedRecord = userService.updateByUserId(user).futureValue

        updatedRecord mustEqual true
      }
    }

    "deleteByUserId" should {

      "must be able to delete an existing user" in {

        when(mockUserRepository.deleteByUserId(23)).thenReturn(Future.successful(true))

        val deletedRecord = userService.deleteByUserId(23).futureValue

        deletedRecord mustEqual true
      }
    }
  }
