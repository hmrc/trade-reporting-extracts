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
import org.mockito.Mockito.when
import org.scalatest.matchers.must.Matchers.mustBe
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.test.Helpers.*
import play.api.test.{FakeRequest, Helpers}
import play.api.{Application, inject}
import uk.gov.hmrc.tradereportingextracts.connectors.CustomsDataStoreConnector
import uk.gov.hmrc.tradereportingextracts.models.{AddressInformation, CompanyInformation}
import uk.gov.hmrc.tradereportingextracts.utils.SpecBase

import scala.concurrent.Future

class CompanyInformationControllerSpec extends SpecBase {

  "getCompanyInformation" should {

    "return company information" in new Setup {
      when(mockCustomsDataStoreConnector.getCompanyInformation()(using any()))
        .thenReturn(Future.successful(companyInformation))

      running(app) {
        val request = FakeRequest(GET, routes.CompanyInformationController.companyInformation().url)

        val result = route(app, request).value

        contentAsJson(result).as[CompanyInformation] mustBe companyInformation
      }
    }
  }

  trait Setup {

    val addressInformation: AddressInformation =
      AddressInformation("12 Example Street", "Example", Some("G64 2SZ"), "GB")

    val companyInformation: CompanyInformation = CompanyInformation("Example Ltd", "1", addressInformation)

    val mockCustomsDataStoreConnector: CustomsDataStoreConnector = mock[CustomsDataStoreConnector]

    val app: Application = application
      .overrides(
        inject.bind[CustomsDataStoreConnector].toInstance(mockCustomsDataStoreConnector)
      )
      .build()
  }
}
