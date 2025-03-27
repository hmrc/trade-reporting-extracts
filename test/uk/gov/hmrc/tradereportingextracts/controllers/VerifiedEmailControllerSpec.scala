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

package uk.gov.hmrc.tradereportingextracts.controllers

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.matchers.must.Matchers.mustBe
import org.scalatest.matchers.should.Matchers
import org.scalatest.matchers.should.Matchers.shouldBe
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.test.Helpers.status
import play.api.test.Helpers.defaultAwaitTimeout
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.tradereportingextracts.connectors.CustomsDataStoreConnector
import uk.gov.hmrc.tradereportingextracts.models.NotificationEmail
import uk.gov.hmrc.tradereportingextracts.utils.SpecBase

import java.time.LocalDateTime
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class VerifiedEmailControllerSpec extends SpecBase:

  lazy val mockCustomsDataStoreConnector: CustomsDataStoreConnector = mock[CustomsDataStoreConnector]

  private val fakeRequest       = FakeRequest("GET", s"/eori/verified-email")
  private val notificationEmail = NotificationEmail("example@test.com", LocalDateTime.now())
  private val controller        =
    new VerifiedEmailController(mockCustomsDataStoreConnector, Helpers.stubControllerComponents())

  "GET /verified-email" should {
    "return a valid email address" in {
      when(mockCustomsDataStoreConnector.getVerifiedEmail()(using any()))
        .thenReturn(Future.successful(notificationEmail))

      val result = controller.getVerifiedEmail()(fakeRequest)

      status(result) shouldBe OK

      verify(mockCustomsDataStoreConnector, times(1)).getVerifiedEmail()(using any)
    }

    "handle exceptions and return SERVICE_UNAVAILABLE" in {
      when(mockCustomsDataStoreConnector.getVerifiedEmail()(using any()))
        .thenReturn(Future.failed(new Exception("Service error")))

      val result = controller.getVerifiedEmail().apply(fakeRequest)

      status(result) mustBe INTERNAL_SERVER_ERROR
    }
  }
