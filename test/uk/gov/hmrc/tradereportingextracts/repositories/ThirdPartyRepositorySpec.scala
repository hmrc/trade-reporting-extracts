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
import uk.gov.hmrc.tradereportingextracts.models.ThirdParty

import scala.concurrent.ExecutionContext.Implicits.global

class ThirdPartyRepositorySpec extends AnyWordSpec,
  MockitoSugar,
  GuiceOneAppPerSuite,
  CleanMongoCollectionSupport,
  Matchers:

  private val thirdParty = ThirdParty(123, "2025-01-01", "2025-12-31", "2025-01-01", "2025-12-31", Some("read"))
  private val thirdParty2 = ThirdParty(123, "2025-01-01", "2025-12-31", "2025-01-01", "2025-12-31", Some("write"))

  val thirdPartyRepository: ThirdPartyRepository = ThirdPartyRepository(mongoComponent)

  "insertThirdParty" should {

    "must insert a third party successfully" in {

      val insertResult = thirdPartyRepository.insertThirdParty(thirdParty).futureValue

      insertResult mustEqual true
    }
  }

  "findByUserId" should {

    "must be able to retrieve a third party successfully using a userId" in {

      val insertResult = thirdPartyRepository.insertThirdParty(thirdParty).futureValue
      val fetchedRecord = thirdPartyRepository.findByUserId(thirdParty.userId).futureValue

      insertResult mustEqual true
      fetchedRecord.get mustEqual thirdParty
    }

    "must return none if userId not found" in {

      val insertResult = thirdPartyRepository.insertThirdParty(thirdParty).futureValue
      val fetchedRecord = thirdPartyRepository.findByUserId(23).futureValue

      insertResult mustEqual true
      fetchedRecord must be(None)
    }
  }

  "updateByUserId" should {

    "must be able to update an existing third party" in {

      val insertResult = thirdPartyRepository.insertThirdParty(thirdParty).futureValue
      val fetchedBeforeUpdateRecord = thirdPartyRepository.findByUserId(thirdParty.userId).futureValue
      val updatedRecord = thirdPartyRepository.updateByUserId(thirdParty2).futureValue
      val fetchedRecord = thirdPartyRepository.findByUserId(thirdParty2.userId).futureValue

      insertResult mustEqual true
      fetchedBeforeUpdateRecord.get mustEqual thirdParty
      updatedRecord mustEqual true
      fetchedRecord.get mustEqual thirdParty2
    }
  }

  "deleteByUserId" should {

    "must be able to delete an existing third party" in {

      val insertResult = thirdPartyRepository.insertThirdParty(thirdParty).futureValue
      val fetchedBeforeDeleteRecord = thirdPartyRepository.findByUserId(thirdParty.userId).futureValue
      val deletedRecord = thirdPartyRepository.deleteByUserId(thirdParty.userId).futureValue

      insertResult mustEqual true
      fetchedBeforeDeleteRecord.get mustEqual thirdParty
      deletedRecord mustEqual true
    }
  }
