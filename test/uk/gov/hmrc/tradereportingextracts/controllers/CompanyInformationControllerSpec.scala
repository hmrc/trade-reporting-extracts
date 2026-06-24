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

import org.mockito.Mockito.when
import play.api.libs.json.Json
import play.api.test.Helpers.*
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.tradereportingextracts.models.{AddressInformation, CompanyInformation}
import uk.gov.hmrc.tradereportingextracts.services.CompanyInformationService
import uk.gov.hmrc.tradereportingextracts.utils.SpecBase
import uk.gov.hmrc.tradereportingextracts.utils.ApplicationConstants
import uk.gov.hmrc.tradereportingextracts.controllers.support.FakeAuth

import scala.concurrent.{ExecutionContext, Future}

class CompanyInformationControllerSpec extends SpecBase {

  implicit val ec: ExecutionContext         = ExecutionContext.Implicits.global
  private val mockCompanyInformationService = mock[CompanyInformationService]
  private val fakeAuthAction                = FakeAuth.Helpers.success(ec)

  private val controller = new CompanyInformationController(
    Helpers.stubControllerComponents(),
    fakeAuthAction,
    mockCompanyInformationService
  )

  "CompanyInformationController.getCompanyInformation" should {

    "return 200 OK with CompanyInformation when valid EORI is provided" in {
      val eori        = "GB123456789000"
      val companyInfo = CompanyInformation(
        name = "Acme Ltd",
        consent = "1",
        address = AddressInformation("123 Street", "City", Some("AB12 3CD"), "UK")
      )

      when(mockCompanyInformationService.getVisibleCompanyInformation(eori))
        .thenReturn(Future.successful(companyInfo))

      val request = FakeRequest(POST, routes.CompanyInformationController.getCompanyInformation.url)
        .withHeaders("Content-Type" -> "application/json", AUTHORIZATION -> "Bearer token")
        .withBody(Json.obj(ApplicationConstants.eori -> eori))

      val result = controller.getCompanyInformation.apply(request)

      status(result)        shouldBe OK
      contentAsJson(result) shouldBe Json.toJson(companyInfo)
    }

    "return 400 BadRequest when EORI is missing" in {

      val request = FakeRequest(POST, routes.CompanyInformationController.getCompanyInformation.url)
        .withHeaders("Content-Type" -> "application/json", AUTHORIZATION -> "Bearer token")
        .withBody(Json.obj("wrongField" -> "value"))

      val result = controller.getCompanyInformation.apply(request)

      status(result)        shouldBe BAD_REQUEST
      contentAsString(result) should include("Missing or invalid EORI in request body")
    }
  }

  "when call to auth fails return corresponding status code" in {

    val failingFakeAuthAction = FakeAuth.Helpers.forbidden(ec)

    val controller = new CompanyInformationController(
      Helpers.stubControllerComponents(),
      failingFakeAuthAction,
      mockCompanyInformationService
    )

    val eori        = "GB123456789000"
    val companyInfo = CompanyInformation(
      name = "Acme Ltd",
      consent = "1",
      address = AddressInformation("123 Street", "City", Some("AB12 3CD"), "UK")
    )

    when(mockCompanyInformationService.getVisibleCompanyInformation(eori))
      .thenReturn(Future.successful(companyInfo))

    val request = FakeRequest(POST, routes.CompanyInformationController.getCompanyInformation.url)
      .withHeaders("Content-Type" -> "application/json", AUTHORIZATION -> "Bearer token")
      .withBody(Json.obj(ApplicationConstants.eori -> eori))

    val result = controller.getCompanyInformation.apply(request)

    status(result) shouldBe FORBIDDEN

  }
}
