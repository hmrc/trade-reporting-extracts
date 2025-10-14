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
import play.api.Application
import play.api.libs.json.{JsArray, JsObject, JsString, Json}
import play.api.mvc.Result
import play.api.test.Helpers.*
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.internalauth.client.*
import uk.gov.hmrc.internalauth.client.Retrieval.EmptyRetrieval
import uk.gov.hmrc.internalauth.client.test.{BackendAuthComponentsStub, StubBehaviour}
import uk.gov.hmrc.tradereportingextracts.models.thirdParty.EoriBusinessInfo
import uk.gov.hmrc.tradereportingextracts.models.*
import uk.gov.hmrc.tradereportingextracts.repositories.ReportRequestRepository
import uk.gov.hmrc.tradereportingextracts.services.UserService
import uk.gov.hmrc.tradereportingextracts.utils.SpecBase

import java.time.{Instant, LocalDate, LocalDateTime, ZoneOffset}
import scala.concurrent.{ExecutionContext, Future}

class UserControllerSpec extends SpecBase {

  implicit val ec: ExecutionContext                        = ExecutionContext.Implicits.global
  private val mockUserService: UserService                 = mock[UserService]
  private val mockStubBehaviour                            = mock[StubBehaviour]
  private val backendAuthComponents: BackendAuthComponents =
    BackendAuthComponentsStub(mockStubBehaviour)(Helpers.stubControllerComponents())
  private val mockReportRequestRepository                  = mock[ReportRequestRepository]
  val controller                                           =
    new UserController(
      mockUserService,
      Helpers.stubControllerComponents(),
      backendAuthComponents,
      mockReportRequestRepository
    )(using ec)
  val readPermission: Predicate.Permission                 = Predicate.Permission(
    Resource(ResourceType("trade-reporting-extracts"), ResourceLocation("trade-reporting-extracts/*")),
    IAAction("READ")
  )
  val writePermission                                      = Predicate.Permission(
    Resource(ResourceType("trade-reporting-extracts"), ResourceLocation("trade-reporting-extracts/*")),
    IAAction("WRITE")
  )

  "UserController.getNotificationEmail" should {

    "return 200 OK with NotificationEmail when valid EORI is provided" in new Setup {
      val eori                     = "GB123456789000"
      val email: NotificationEmail = NotificationEmail("user@example.com", LocalDateTime.now())

      when(mockUserService.getNotificationEmail(eori))
        .thenReturn(Future.successful(email))
      when(mockStubBehaviour.stubAuth(Some(readPermission), EmptyRetrieval))
        .thenReturn(Future.successful(EmptyRetrieval))

      val request: FakeRequest[JsObject] = FakeRequest(POST, routes.UserController.getNotificationEmail.url)
        .withHeaders("Content-Type" -> "application/json", AUTHORIZATION -> "my-token")
        .withBody(Json.obj("eori" -> eori))

      val result: Future[Result] = controller.getNotificationEmail.apply(request)

      status(result)        shouldBe OK
      contentAsJson(result) shouldBe Json.toJson(email)
    }

    "return 400 BadRequest when EORI is missing" in new Setup {
      when(mockStubBehaviour.stubAuth(Some(readPermission), EmptyRetrieval))
        .thenReturn(Future.successful(EmptyRetrieval))
      val request: FakeRequest[JsObject] = FakeRequest(POST, routes.UserController.getNotificationEmail.url)
        .withHeaders("Content-Type" -> "application/json", AUTHORIZATION -> "my-token")
        .withBody(Json.obj("invalidField" -> "value"))

      val result: Future[Result] = controller.getNotificationEmail.apply(request)

      status(result)        shouldBe BAD_REQUEST
      contentAsString(result) should include("Missing or invalid 'eori' field")
    }

    "return 500 InternalServerError when service fails" in new Setup {
      val eori = "GB123456789000"

      when(mockUserService.getNotificationEmail(eori))
        .thenReturn(Future.failed(new RuntimeException("Service failure")))
      when(mockStubBehaviour.stubAuth(Some(readPermission), EmptyRetrieval))
        .thenReturn(Future.successful(EmptyRetrieval))
      val request: FakeRequest[JsObject] = FakeRequest(POST, routes.UserController.getNotificationEmail.url)
        .withHeaders("Content-Type" -> "application/json", AUTHORIZATION -> "my-token")
        .withBody(Json.obj("eori" -> eori))

      val result: Future[Result] = controller.getNotificationEmail.apply(request)

      status(result)        shouldBe INTERNAL_SERVER_ERROR
      contentAsString(result) should include("Service failure")
    }
  }

  "UserController.setupUser" should {

    "return 201 Created with user details when valid EORI is provided" in new Setup {
      val eori                     = "GB123456789000"
      val userDetails: UserDetails = UserDetails(
        eori = eori,
        additionalEmails = Seq.empty,
        authorisedUsers = Seq.empty,
        companyInformation = CompanyInformation(), // assuming default constructor exists
        notificationEmail = NotificationEmail("user@example.com", LocalDateTime.now())
      )

      when(mockUserService.getOrCreateUser(eori))
        .thenReturn(Future.successful(userDetails))
      when(mockStubBehaviour.stubAuth(Some(readPermission), EmptyRetrieval))
        .thenReturn(Future.successful(EmptyRetrieval))

      val request: FakeRequest[JsObject] = FakeRequest(GET, routes.UserController.setupUser().url)
        .withHeaders("Content-Type" -> "application/json", AUTHORIZATION -> "my-token")
        .withBody(Json.obj("eori" -> eori))

      val result: Future[Result] = controller.setupUser().apply(request)

      status(result)        shouldBe CREATED
      contentAsJson(result) shouldBe Json.toJson(userDetails)
    }

    "return 400 BadRequest when EORI is missing" in new Setup {
      when(mockStubBehaviour.stubAuth(Some(readPermission), EmptyRetrieval))
        .thenReturn(Future.successful(EmptyRetrieval))
      val request: FakeRequest[JsObject] = FakeRequest(GET, routes.UserController.setupUser().url)
        .withHeaders("Content-Type" -> "application/json", AUTHORIZATION -> "my-token")
        .withBody(Json.obj("wrongField" -> "value"))

      val result: Future[Result] = controller.setupUser().apply(request)

      status(result)        shouldBe BAD_REQUEST
      contentAsString(result) should include("Missing or invalid 'eori' field")
    }
  }

  "UserController.getAuthorisedEoris" should {

    "return 200 OK with list of authorised EORIs" in new Setup {
      val eori                         = "GB123456789000"
      val authorisedEoris: Seq[String] = Seq("GB111111111111", "GB222222222222")

      when(mockUserService.getAuthorisedEoris(eori))
        .thenReturn(Future.successful(authorisedEoris))
      when(mockStubBehaviour.stubAuth(Some(readPermission), EmptyRetrieval))
        .thenReturn(Future.successful(EmptyRetrieval))

      val request: FakeRequest[JsObject] = FakeRequest(POST, routes.UserController.getAuthorisedEoris.url)
        .withHeaders("Content-Type" -> "application/json", AUTHORIZATION -> "my-token")
        .withBody(Json.obj("eori" -> eori))

      val result: Future[Result] = controller.getAuthorisedEoris.apply(request)

      status(result)        shouldBe OK
      contentAsJson(result) shouldBe JsArray(authorisedEoris.map(JsString(_)))
    }

    "return 500 InternalServerError when service fails" in new Setup {
      val eori = "GB123456789000"

      when(mockUserService.getAuthorisedEoris(eori))
        .thenReturn(Future.failed(new RuntimeException("Service failure")))
      when(mockStubBehaviour.stubAuth(Some(readPermission), EmptyRetrieval))
        .thenReturn(Future.successful(EmptyRetrieval))
      val request: FakeRequest[JsObject] = FakeRequest(POST, routes.UserController.getAuthorisedEoris.url)
        .withHeaders("Content-Type" -> "application/json", AUTHORIZATION -> "my-token")
        .withBody(Json.obj("eori" -> eori))

      val result: Future[Result] = controller.getAuthorisedEoris.apply(request)

      status(result)        shouldBe INTERNAL_SERVER_ERROR
      contentAsString(result) should include("Service failure")
    }
  }

  "UserController.getUserDetails" should {

    "return 201 OK with user details when valid EORI is provided" in new Setup {
      val eori                           = "GB123456789000"
      val userDetails: UserDetails       = UserDetails(
        eori = eori,
        additionalEmails = Seq.empty,
        authorisedUsers = Seq.empty,
        companyInformation = CompanyInformation(
          name = "Test Company",
          consent = "1",
          address = AddressInformation(
            streetAndNumber = "123 Test Street",
            city = "Test City",
            postalCode = Some("12345"),
            countryCode = "GB"
          )
        ),
        notificationEmail = NotificationEmail("test@test.com", LocalDateTime.now())
      )
      when(mockUserService.getUserAndEmailDetails(eori)).thenReturn(Future.successful(userDetails))
      when(mockStubBehaviour.stubAuth(Some(readPermission), EmptyRetrieval))
        .thenReturn(Future.successful(EmptyRetrieval))
      val request: FakeRequest[JsObject] = FakeRequest(GET, routes.UserController.getUserAndEmail.url)
        .withHeaders("Content-Type" -> "application/json", AUTHORIZATION -> "my-token")
        .withBody(Json.obj("eori" -> eori))
      val result: Future[Result]         = controller.getUserAndEmail.apply(request)
      status(result)        shouldBe CREATED
      contentAsJson(result) shouldBe Json.toJson(userDetails)
    }
  }

  "UserController.getThirdPartyDetails" should {

    val eori           = "123"
    val thirdPartyEori = "456"

    "return a 200 when provided a valid eori and thirdPartyEori" in new Setup {

      val authUser: AuthorisedUser = AuthorisedUser(
        eori = thirdPartyEori,
        referenceName = Some("foo"),
        accessStart = Instant.now(),
        accessEnd = Some(Instant.now()),
        accessType = Set(AccessType.IMPORTS, AccessType.EXPORTS),
        reportDataStart = Some(Instant.now()),
        reportDataEnd = Some(Instant.now())
      )

      val thirdPartyDetails: ThirdPartyDetails = ThirdPartyDetails(
        referenceName = Some("foo"),
        accessStartDate = LocalDate.ofInstant(Instant.now(), ZoneOffset.UTC),
        accessEndDate = Some(LocalDate.ofInstant(Instant.now(), ZoneOffset.UTC)),
        dataTypes = Set("imports", "exports"),
        dataStartDate = Some(LocalDate.ofInstant(Instant.now(), ZoneOffset.UTC)),
        dataEndDate = Some(LocalDate.ofInstant(Instant.now(), ZoneOffset.UTC))
      )

      when(mockUserService.getAuthorisedUser(any(), any())).thenReturn(Future.successful(Some(authUser)))

      when(mockStubBehaviour.stubAuth(Some(readPermission), EmptyRetrieval))
        .thenReturn(Future.successful(EmptyRetrieval))

      when(mockUserService.transformToThirdPartyDetails(any())).thenReturn(thirdPartyDetails)

      val request: FakeRequest[JsObject] = FakeRequest(GET, routes.UserController.getAuthorisedEoris.url)
        .withHeaders(AUTHORIZATION -> "my-token")
        .withBody(Json.obj("eori" -> eori, "thirdPartyEori" -> thirdPartyEori))

      val result: Future[Result] = controller.getThirdPartyDetails.apply(request)
      status(result)        shouldBe OK
      contentAsJson(result) shouldBe Json.toJson(thirdPartyDetails)

    }

    "return a 404 when no authorised user is found" in new Setup {

      when(mockUserService.getAuthorisedUser(any(), any())).thenReturn(Future.successful(None))

      when(mockStubBehaviour.stubAuth(Some(readPermission), EmptyRetrieval))
        .thenReturn(Future.successful(EmptyRetrieval))

      val request: FakeRequest[JsObject] = FakeRequest(GET, routes.UserController.getAuthorisedEoris.url)
        .withHeaders(AUTHORIZATION -> "my-token")
        .withBody(Json.obj("eori" -> eori, "thirdPartyEori" -> thirdPartyEori))

      val result: Future[Result] = controller.getThirdPartyDetails.apply(request)
      status(result)        shouldBe NOT_FOUND
      contentAsString(result) should include("No authorised user found for third party EORI")
    }

    "return a bad request when eori invalid" in new Setup {

      val thirdPartyEori = "456"

      when(mockStubBehaviour.stubAuth(Some(readPermission), EmptyRetrieval))
        .thenReturn(Future.successful(EmptyRetrieval))

      val request: FakeRequest[JsObject] = FakeRequest(GET, routes.UserController.getAuthorisedEoris.url)
        .withHeaders(AUTHORIZATION -> "my-token")
        .withBody(Json.obj("thirdPartyEori" -> thirdPartyEori))

      val result: Future[Result] = controller.getThirdPartyDetails.apply(request)
      status(result)        shouldBe BAD_REQUEST
      contentAsString(result) should include("Missing or invalid 'eori' field")

    }

    "return a bad request when thirdPartyEori invalid" in new Setup {

      when(mockStubBehaviour.stubAuth(Some(readPermission), EmptyRetrieval))
        .thenReturn(Future.successful(EmptyRetrieval))

      val request: FakeRequest[JsObject] = FakeRequest(GET, routes.UserController.getAuthorisedEoris.url)
        .withHeaders(AUTHORIZATION -> "my-token")
        .withBody(Json.obj("eori" -> eori))

      val result: Future[Result] = controller.getThirdPartyDetails.apply(request)
      status(result)        shouldBe BAD_REQUEST
      contentAsString(result) should include("Missing or invalid 'thirdPartyEori' field")
    }
  }

  "UserController.getAuthorisedBusinessDetails" should {

    val eori         = "123"
    val businessEori = "456"

    "return a 200 when provided a valid eori and businessEori" in new Setup {

      val authUser = AuthorisedUser(
        eori = eori,
        referenceName = Some("foo"),
        accessStart = Instant.now(),
        accessEnd = Some(Instant.now()),
        accessType = Set(AccessType.IMPORTS, AccessType.EXPORTS),
        reportDataStart = Some(Instant.now()),
        reportDataEnd = Some(Instant.now())
      )

      val thirdPartyDetails = ThirdPartyDetails(
        referenceName = Some("foo"),
        accessStartDate = LocalDate.ofInstant(Instant.now(), ZoneOffset.UTC),
        accessEndDate = Some(LocalDate.ofInstant(Instant.now(), ZoneOffset.UTC)),
        dataTypes = Set("imports", "exports"),
        dataStartDate = Some(LocalDate.ofInstant(Instant.now(), ZoneOffset.UTC)),
        dataEndDate = Some(LocalDate.ofInstant(Instant.now(), ZoneOffset.UTC))
      )

      when(mockUserService.getAuthorisedBusiness(any(), any())).thenReturn(Future.successful(Some(authUser)))

      when(mockStubBehaviour.stubAuth(Some(readPermission), EmptyRetrieval))
        .thenReturn(Future.successful(EmptyRetrieval))

      when(mockUserService.transformToThirdPartyDetails(any())).thenReturn(thirdPartyDetails)

      val request: FakeRequest[JsObject] = FakeRequest(GET, routes.UserController.getAuthorisedBusinessDetails.url)
        .withHeaders(AUTHORIZATION -> "my-token")
        .withBody(Json.obj("thirdPartyEori" -> eori, "traderEori" -> businessEori))

      val result: Future[Result] = controller.getAuthorisedBusinessDetails.apply(request)
      status(result)        shouldBe OK
      contentAsJson(result) shouldBe Json.toJson(thirdPartyDetails)

    }

    "return a 404 when no authorised user is found" in new Setup {

      when(mockUserService.getAuthorisedBusiness(any(), any())).thenReturn(Future.successful(None))

      when(mockStubBehaviour.stubAuth(Some(readPermission), EmptyRetrieval))
        .thenReturn(Future.successful(EmptyRetrieval))

      val request: FakeRequest[JsObject] = FakeRequest(GET, routes.UserController.getAuthorisedBusinessDetails.url)
        .withHeaders(AUTHORIZATION -> "my-token")
        .withBody(Json.obj("thirdPartyEori" -> eori, "traderEori" -> businessEori))

      val result: Future[Result] = controller.getAuthorisedBusinessDetails.apply(request)
      status(result)        shouldBe NOT_FOUND
      contentAsString(result) should include("No authorised user found for the trader EORI")
    }

  }

  "UserController.getUsersByAuthorisedEoriWithStatus" should {

    "return 200 OK with list of users including status" in new Setup {
      val authorisedEori    = "GB111111111111"
      val eoriBusinessInfos = Seq(
        EoriBusinessInfo(
          eori = "GB123456789000",
          businessInfo = Some("ABC Ltd"),
          status = Some(UserActiveStatus.Active)
        )
      )

      when(mockUserService.getUsersByAuthorisedEoriWithStatus(authorisedEori))
        .thenReturn(Future.successful(eoriBusinessInfos))
      when(mockStubBehaviour.stubAuth(Some(readPermission), EmptyRetrieval))
        .thenReturn(Future.successful(EmptyRetrieval))

      val request = FakeRequest(GET, routes.UserController.getUsersByAuthorisedEoriWithStatus.url)
        .withHeaders("Content-Type" -> "application/json", AUTHORIZATION -> "my-token")
        .withBody(Json.obj("thirdPartyEori" -> authorisedEori))

      val result = controller.getUsersByAuthorisedEoriWithStatus.apply(request)
      contentAsJson(result) shouldBe Json.toJson(eoriBusinessInfos)
    }

    "return 200 OK with list of users and no company information if no consent" in new Setup {
      val authorisedEori    = "GB111111111111"
      val eoriBusinessInfos = Seq(
        EoriBusinessInfo(
          eori = "GB123456789000",
          businessInfo = None,
          status = Some(UserActiveStatus.Active)
        )
      )

      when(mockUserService.getUsersByAuthorisedEoriWithStatus(authorisedEori))
        .thenReturn(Future.successful(eoriBusinessInfos))
      when(mockStubBehaviour.stubAuth(Some(readPermission), EmptyRetrieval))
        .thenReturn(Future.successful(EmptyRetrieval))

      val request = FakeRequest(GET, routes.UserController.getUsersByAuthorisedEoriWithStatus.url)
        .withHeaders("Content-Type" -> "application/json", AUTHORIZATION -> "my-token")
        .withBody(Json.obj("thirdPartyEori" -> authorisedEori))

      val result = controller.getUsersByAuthorisedEoriWithStatus.apply(request)
      contentAsJson(result) shouldBe Json.toJson(eoriBusinessInfos)
    }
  }

  "UserController.getUsersByAuthorisedEoriWithDateFilter" should {

    "return 200 OK with list of users without status" in new Setup {
      val authorisedEori    = "GB111111111111"
      val eoriBusinessInfos = Seq(
        EoriBusinessInfo(
          eori = "GB123456789000",
          businessInfo = Some("ABC Ltd"),
          status = None
        )
      )

      when(mockUserService.getUsersByAuthorisedEoriWithDateFilter(authorisedEori))
        .thenReturn(Future.successful(eoriBusinessInfos))
      when(mockStubBehaviour.stubAuth(Some(readPermission), EmptyRetrieval))
        .thenReturn(Future.successful(EmptyRetrieval))

      val request = FakeRequest(GET, routes.UserController.getUsersByAuthorisedEoriWithDateFilter.url)
        .withHeaders("Content-Type" -> "application/json", AUTHORIZATION -> "my-token")
        .withBody(Json.obj("thirdPartyEori" -> authorisedEori))

      val result = controller.getUsersByAuthorisedEoriWithDateFilter.apply(request)
      contentAsJson(result) shouldBe Json.toJson(eoriBusinessInfos)
    }

    "return 200 OK with list of users and no company information if no consent" in new Setup {
      val authorisedEori    = "GB111111111111"
      val eoriBusinessInfos = Seq(
        EoriBusinessInfo(
          eori = "GB123456789000",
          businessInfo = None,
          status = None
        )
      )

      when(mockUserService.getUsersByAuthorisedEoriWithDateFilter(authorisedEori))
        .thenReturn(Future.successful(eoriBusinessInfos))
      when(mockStubBehaviour.stubAuth(Some(readPermission), EmptyRetrieval))
        .thenReturn(Future.successful(EmptyRetrieval))

      val request = FakeRequest(GET, routes.UserController.getUsersByAuthorisedEoriWithDateFilter.url)
        .withHeaders("Content-Type" -> "application/json", AUTHORIZATION -> "my-token")
        .withBody(Json.obj("thirdPartyEori" -> authorisedEori))

      val result = controller.getUsersByAuthorisedEoriWithDateFilter.apply(request)
      contentAsJson(result) shouldBe Json.toJson(eoriBusinessInfos)
    }
  }

  "UserController.thirdPartyAccessSelfRemoval" should {

    "return an OK when authorised user deleted and third party reports removed" in new Setup {
      val traderEori     = "123"
      val thirdPartyEori = "456"

      when(mockUserService.deleteAuthorisedUser(traderEori, thirdPartyEori))
        .thenReturn(Future.successful(true))
      when(mockReportRequestRepository.deleteReportsForThirdPartyRemoval(traderEori, thirdPartyEori))
        .thenReturn(Future.successful(true))
      when(mockStubBehaviour.stubAuth(Some(writePermission), EmptyRetrieval))
        .thenReturn(Future.successful(EmptyRetrieval))

      val request: FakeRequest[JsObject] = FakeRequest(POST, routes.UserController.thirdPartyAccessSelfRemoval.url)
        .withHeaders("Content-Type" -> "application/json", AUTHORIZATION -> "my-token")
        .withBody(Json.obj("traderEori" -> traderEori, "thirdPartyEori" -> thirdPartyEori))

      val result: Future[Result] = controller.thirdPartyAccessSelfRemoval.apply(request)

      status(result) shouldBe OK
    }

    "return an internal server error when authorised user delete fails" in new Setup {
      val traderEori     = "123"
      val thirdPartyEori = "456"

      when(mockUserService.deleteAuthorisedUser(traderEori, thirdPartyEori))
        .thenReturn(Future.successful(false))
      when(mockStubBehaviour.stubAuth(Some(writePermission), EmptyRetrieval))
        .thenReturn(Future.successful(EmptyRetrieval))

      val request: FakeRequest[JsObject] = FakeRequest(POST, routes.UserController.thirdPartyAccessSelfRemoval.url)
        .withHeaders("Content-Type" -> "application/json", AUTHORIZATION -> "my-token")
        .withBody(Json.obj("traderEori" -> traderEori, "thirdPartyEori" -> thirdPartyEori))

      val result: Future[Result] = controller.thirdPartyAccessSelfRemoval.apply(request)

      status(result)        shouldBe INTERNAL_SERVER_ERROR
      contentAsString(result) should include("Failed to remove third party access")
    }

    "return an internal server error when deleteReportsForThirdParty fails" in new Setup {
      val traderEori     = "123"
      val thirdPartyEori = "456"

      when(mockUserService.deleteAuthorisedUser(traderEori, thirdPartyEori))
        .thenReturn(Future.successful(true))
      when(mockReportRequestRepository.deleteReportsForThirdPartyRemoval(traderEori, thirdPartyEori))
        .thenReturn(Future.successful(false))
      when(mockStubBehaviour.stubAuth(Some(writePermission), EmptyRetrieval))
        .thenReturn(Future.successful(EmptyRetrieval))

      val request: FakeRequest[JsObject] = FakeRequest(POST, routes.UserController.thirdPartyAccessSelfRemoval.url)
        .withHeaders("Content-Type" -> "application/json", AUTHORIZATION -> "my-token")
        .withBody(Json.obj("traderEori" -> traderEori, "thirdPartyEori" -> thirdPartyEori))

      val result: Future[Result] = controller.thirdPartyAccessSelfRemoval.apply(request)

      status(result)        shouldBe INTERNAL_SERVER_ERROR
      contentAsString(result) should include("Failed to remove reports for third party access removal")
    }

    "return a bad request when invalid traderEori" in new Setup {
      val thirdPartyEori = "456"

      when(mockStubBehaviour.stubAuth(Some(writePermission), EmptyRetrieval))
        .thenReturn(Future.successful(EmptyRetrieval))

      val request: FakeRequest[JsObject] = FakeRequest(POST, routes.UserController.thirdPartyAccessSelfRemoval.url)
        .withHeaders("Content-Type" -> "application/json", AUTHORIZATION -> "my-token")
        .withBody(Json.obj("wrongField" -> "value", "thirdPartyEori" -> thirdPartyEori))

      val result: Future[Result] = controller.thirdPartyAccessSelfRemoval.apply(request)

      status(result)        shouldBe BAD_REQUEST
      contentAsString(result) should include("Missing or invalid 'traderEori' field")
    }

    "return a bad request when invalid thirdPartyEori" in new Setup {
      val traderEori = "123"

      when(mockStubBehaviour.stubAuth(Some(writePermission), EmptyRetrieval))
        .thenReturn(Future.successful(EmptyRetrieval))

      val request: FakeRequest[JsObject] = FakeRequest(POST, routes.UserController.thirdPartyAccessSelfRemoval.url)
        .withHeaders("Content-Type" -> "application/json", AUTHORIZATION -> "my-token")
        .withBody(Json.obj("traderEori" -> traderEori, "wrongField" -> "value"))

      val result: Future[Result] = controller.thirdPartyAccessSelfRemoval.apply(request)

      status(result)        shouldBe BAD_REQUEST
      contentAsString(result) should include("Missing or invalid 'thirdPartyEori' field")
    }

    "return a bad request when both EORIs are invalid" in new Setup {
      val request: FakeRequest[JsObject] = FakeRequest(POST, routes.UserController.thirdPartyAccessSelfRemoval.url)
        .withHeaders("Content-Type" -> "application/json", AUTHORIZATION -> "my-token")
        .withBody(Json.obj("foo" -> "bar", "fizz" -> "buzz"))

      when(mockStubBehaviour.stubAuth(Some(writePermission), EmptyRetrieval))
        .thenReturn(Future.successful(EmptyRetrieval))

      val result: Future[Result] = controller.thirdPartyAccessSelfRemoval.apply(request)

      status(result)        shouldBe BAD_REQUEST
      contentAsString(result) should include("Missing or invalid 'traderEori' and 'thirdPartyEori' fields")
    }
  }

  trait Setup {
    val app: Application = application
      .build()
  }
}
