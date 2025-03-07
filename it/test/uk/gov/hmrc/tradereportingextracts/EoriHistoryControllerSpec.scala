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

package uk.gov.hmrc.tradereportingextracts

import org.scalatestplus.play._
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers._
import play.api.test._
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers._
import scala.concurrent.Future
import play.api.libs.json.Json
import uk.gov.hmrc.tradereportingextracts.connectors.CustomsDataStoreConnector
import uk.gov.hmrc.tradereportingextracts.controllers.EoriHistoryController
import uk.gov.hmrc.tradereportingextracts.models.{EoriHistoryResponse, EoriPeriod}
import play.api.mvc.Results
import uk.gov.hmrc.http.HeaderCarrier
import scala.concurrent.ExecutionContext.Implicits.global

class EoriHistoryControllerSpec extends PlaySpec with MockitoSugar with Results {

  "EoriHistoryController GET" should {

    "return OK and the correct response for a valid EORI" in {
      val mockCustomsDataStoreConnector = mock[CustomsDataStoreConnector]
      val mockControllerComponents = stubControllerComponents()
      val eori = "GB123456789000"
      val eoriHistoryResponse = EoriHistoryResponse(Seq(EoriPeriod(eori, Some("2020-01-01"), Some("2021-01-01"))))

      when(mockCustomsDataStoreConnector.getEoriHistory()(using any())).thenReturn(Future.successful(eoriHistoryResponse))

      val controller = new EoriHistoryController(mockCustomsDataStoreConnector, mockControllerComponents)
      val result = controller.getEoriHistory().apply(FakeRequest(GET, s"/eori/history"))

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(eoriHistoryResponse)
    }

    "handle exceptions and return SERVICE_UNAVAILABLE" in {
      val mockCustomsDataStoreConnector = mock[CustomsDataStoreConnector]
      val mockControllerComponents = stubControllerComponents()
      val eori = "GB123456789000"

      when(mockCustomsDataStoreConnector.getEoriHistory()(using any())).thenReturn(Future.failed(new Exception("Service error")))

      val controller = new EoriHistoryController(mockCustomsDataStoreConnector, mockControllerComponents)
      val result = controller.getEoriHistory().apply(FakeRequest(GET, s"/eori/history"))

      status(result) mustBe SERVICE_UNAVAILABLE
    }
  }
}