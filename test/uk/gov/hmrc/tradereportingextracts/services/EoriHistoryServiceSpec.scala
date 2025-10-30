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

import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.mockito.MockitoSugar.mock
import uk.gov.hmrc.tradereportingextracts.connectors.CustomsDataStoreConnector
import uk.gov.hmrc.tradereportingextracts.models.{EoriHistory, EoriHistoryResponse}

import scala.concurrent.{ExecutionContext, Future}

class EoriHistoryServiceSpec extends AsyncWordSpec with Matchers with ScalaFutures with MockitoSugar {
  implicit val ec: ExecutionContext            = ExecutionContext.Implicits.global
  val mockConnector: CustomsDataStoreConnector = mock[CustomsDataStoreConnector]
  val service                                  = new EoriHistoryService(mockConnector)(using ec)

  val eori                        = "GB123456789012"
  val history1: EoriHistory       =
    EoriHistory(eori, Some("2024-01-01"), Some("2024-06-30"))
  val history2: EoriHistory       =
    EoriHistory(eori, Some("2024-07-01"), Some("2024-12-31"))
  val histories: Seq[EoriHistory] = Seq(history1, history2)

  "EoriHistoryService.fetchEoriHistory" should {
    "return Some(seq) when connector returns non-empty sequence" in {
      val eori                           = "GB123456789000"
      val histories: EoriHistoryResponse = EoriHistoryResponse(Seq(EoriHistory(eori, None, None)))
      when(mockConnector.getEoriHistory(eori)).thenReturn(Future.successful(histories))
      val service                        = new EoriHistoryService(mockConnector)(using ec)
      service.fetchEoriHistory(eori).map(_ shouldBe Some(histories))
    }

    "return None when connector returns empty sequence" in {
      val eori          = "GB123456789000"
      val emptyResponse = EoriHistoryResponse(Seq.empty)
      when(mockConnector.getEoriHistory(eori)).thenReturn(Future.successful(emptyResponse))
      val service       = new EoriHistoryService(mockConnector)
      service.fetchEoriHistory(eori).map(_ shouldBe None)
    }
  }
}
