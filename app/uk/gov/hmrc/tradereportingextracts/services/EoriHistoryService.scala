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

import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tradereportingextracts.connectors.CustomsDataStoreConnector
import uk.gov.hmrc.tradereportingextracts.models.EoriHistoryResponse

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EoriHistoryService @Inject() (
  customsDataStoreConnector: CustomsDataStoreConnector
)(using ec: ExecutionContext) {
  implicit val hc: HeaderCarrier = HeaderCarrier()

  def fetchEoriHistory(eori: String): Future[Option[EoriHistoryResponse]] =
    customsDataStoreConnector.getEoriHistory(eori).map { response =>
      if (response.eoriHistory.nonEmpty) Some(response) else None
    }
}
