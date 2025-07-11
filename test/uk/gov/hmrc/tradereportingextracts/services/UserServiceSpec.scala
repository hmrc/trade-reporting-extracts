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

import org.mockito.Mockito.{verify, when}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{OptionValues, TryValues}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.mockito.MockitoSugar.mock
import uk.gov.hmrc.tradereportingextracts.connectors.CustomsDataStoreConnector
import uk.gov.hmrc.tradereportingextracts.repositories.UserRepository

import scala.concurrent.{ExecutionContext, Future}

class UserServiceSpec
    extends AnyFreeSpec
    with Matchers
    with TryValues
    with OptionValues
    with ScalaFutures
    with IntegrationPatience {

  "UserInformationServiceSpec" - {
    val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

    val mockRepository                = mock[UserRepository]
    val mockCustomsDataStoreConnector = mock[CustomsDataStoreConnector]

    val service = new UserService(mockRepository, mockCustomsDataStoreConnector)(using ec: ExecutionContext)

    val eori            = "EORI1234"
    val authorisedEoris = Seq("AUTH-EORI-1", "AUTH-EORI-2")

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
      val notificationEmail = uk.gov.hmrc.tradereportingextracts.models.NotificationEmail("test@email.com")

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
  }
}
