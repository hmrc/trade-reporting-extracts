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

import org.mockito.Mockito.*
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.*
import play.api.libs.json.Json
import play.api.mvc.Results
import play.api.test.*
import play.api.test.Helpers.*
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tradereportingextracts.controllers.EoriHistoryController
import uk.gov.hmrc.tradereportingextracts.models.{EoriHistory, EoriHistoryResponse}
import uk.gov.hmrc.tradereportingextracts.services.EoriHistoryService

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

class EoriHistoryControllerSpec extends PlaySpec with MockitoSugar with Results {
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

  private given HeaderCarrier = HeaderCarrier()
  "EoriHistoryController GET" should {

    "return OK and the correct response for a valid EORI" in {
      val eoriHistoryService                       = mock[EoriHistoryService]
      val mockControllerComponents                 = stubControllerComponents()
      val eori                                     = "GB123456789000"
      val eoriHistoryResponse: EoriHistoryResponse = EoriHistoryResponse(
        Seq(EoriHistory(eori, Some(LocalDate.parse("2020-01-01")), Some(LocalDate.parse("2021-01-01"))))
      )
      when(eoriHistoryService.fetchEoriHistory(eori)).thenReturn(Future.successful(Some(eoriHistoryResponse)))
      val controller                               = new EoriHistoryController(eoriHistoryService, mockControllerComponents)(using ec)
      val result                                   = controller
        .getEoriHistory()
        .apply(
          FakeRequest(GET, s"/eori/eori-history")
            .withHeaders(CONTENT_TYPE -> JSON)
            .withJsonBody(Json.obj("eori" -> eori))
        )
      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(eoriHistoryResponse)
    }

    "handle exceptions and return SERVICE_UNAVAILABLE" in {
      val eoriHistoryService       = mock[EoriHistoryService]
      val mockControllerComponents = stubControllerComponents()
      val eori                     = "GB123456789000"

      when(eoriHistoryService.fetchEoriHistory(eori)).thenReturn(Future.failed(new Exception("Service error")))

      val controller = new EoriHistoryController(eoriHistoryService, mockControllerComponents)(using ec)
      val result     = controller
        .getEoriHistory()
        .apply(
          FakeRequest(POST, s"/eori/eori-history")
            .withHeaders(CONTENT_TYPE -> JSON)
            .withJsonBody(Json.obj("eori" -> eori))
        )
      status(result) mustBe SERVICE_UNAVAILABLE
    }
  }
}
