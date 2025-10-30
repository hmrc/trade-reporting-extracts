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

import org.scalatest.wordspec.AsyncWordSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.Mockito.*

import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tradereportingextracts.connectors.CustomsDataStoreConnector
import uk.gov.hmrc.tradereportingextracts.models.{AddressInformation, CompanyInformation}

class CompanyInformationServiceSpec extends AsyncWordSpec with Matchers with MockitoSugar {

  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  implicit val hc: HeaderCarrier    = HeaderCarrier()

  val mockConnector: CustomsDataStoreConnector = mock[CustomsDataStoreConnector]
  val service                                  = new CompanyInformationService(mockConnector)

  val eori                            = "GB123456789000"
  val companyInfo: CompanyInformation = CompanyInformation(
    name = "Acme Ltd",
    consent = "1",
    address = AddressInformation("123 Street", "City", Some("AB12 3CD"), "UK")
  )

  "CompanyInformationService.getVisibleCompanyInformation" should {

    "return CompanyInformation when connector returns successfully" in {
      when(mockConnector.getCompanyInformation(eori)).thenReturn(Future.successful(companyInfo))

      service.getVisibleCompanyInformation(eori).map { result =>
        result shouldBe companyInfo
      }
    }

    "fail when connector throws an exception" in {
      val exception = new RuntimeException("Service failure")
      when(mockConnector.getCompanyInformation(eori)).thenReturn(Future.failed(exception))

      recoverToExceptionIf[RuntimeException] {
        service.getVisibleCompanyInformation(eori)
      }.map { ex =>
        ex.getMessage shouldBe "Service failure"
      }
    }
  }
}
