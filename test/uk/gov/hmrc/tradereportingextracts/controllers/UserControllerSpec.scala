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
import org.apache.pekko.Done
import org.mockito.ArgumentMatchers.any
import org.mockito.stubbing.OngoingStubbing
import play.api.Application
import play.api.libs.json.{JsArray, JsObject, JsString, JsValue, Json}
import play.api.mvc.Results.Status
import play.api.mvc.{Action, BodyParser, Request, Result}
import play.api.test.Helpers.*
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.tradereportingextracts.connectors.{CustomsDataStoreConnector, EmailConnector}
import uk.gov.hmrc.tradereportingextracts.models.thirdParty.EoriBusinessInfo
import uk.gov.hmrc.tradereportingextracts.models.*
import uk.gov.hmrc.tradereportingextracts.repositories.ReportRequestRepository
import uk.gov.hmrc.tradereportingextracts.services.UserService
import uk.gov.hmrc.tradereportingextracts.utils.{ApplicationConstants, SpecBase, WireMockHelper}
import uk.gov.hmrc.tradereportingextracts.controllers.action.AuthAction

import java.time.{Instant, LocalDate, LocalDateTime, ZoneOffset}
import scala.concurrent.{ExecutionContext, Future}

class UserControllerSpec extends SpecBase with WireMockHelper {

  implicit val ec: ExecutionContext         = ExecutionContext.Implicits.global
  private val mockUserService: UserService  = mock[UserService]
  private val mockReportRequestRepository   = mock[ReportRequestRepository]
  private val mockCustomsDataStoreConnector = mock[CustomsDataStoreConnector]
  private val mockEmailConnector            = mock[EmailConnector]
  private val mockAuthAction                = mock[AuthAction]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockEmailConnector)
    reset(mockAuthAction)
  }

  val controllerWithConnectors =
    new UserController(
      mockUserService,
      Helpers.stubControllerComponents(),
      mockAuthAction,
      mockReportRequestRepository,
      mockCustomsDataStoreConnector,
      mockEmailConnector
    )(using ec)

  val controller =
    new UserController(
      mockUserService,
      Helpers.stubControllerComponents(),
      mockAuthAction,
      mockReportRequestRepository,
      null,
      null
    )(using ec)

  "UserController.getNotificationEmail" should {

    "return 200 OK with NotificationEmail when valid EORI is provided" in new Setup {
      val eori                     = "GB123456789000"
      val email: NotificationEmail = NotificationEmail("user@example.com", LocalDateTime.now())

      when(mockUserService.getNotificationEmail(eori))
        .thenReturn(Future.successful(email))

      successfulAuthAction(mockAuthAction)

      val request: FakeRequest[JsObject] = FakeRequest(POST, routes.UserController.getNotificationEmail.url)
        .withHeaders("Content-Type" -> "application/json", AUTHORIZATION -> "my-token")
        .withBody(Json.obj(ApplicationConstants.eori -> eori))

      val result: Future[Result] = controller.getNotificationEmail.apply(request)

      status(result)        shouldBe OK
      contentAsJson(result) shouldBe Json.toJson(email)
    }

    "return corresponding error code when call to auth fails" in new Setup {
      val eori                     = "GB123456789000"
      val email: NotificationEmail = NotificationEmail("user@example.com", LocalDateTime.now())

      when(mockUserService.getNotificationEmail(eori))
        .thenReturn(Future.successful(email))

      failingAuthAction(mockAuthAction)

      val request: FakeRequest[JsObject] = FakeRequest(POST, routes.UserController.getNotificationEmail.url)
        .withHeaders("Content-Type" -> "application/json", AUTHORIZATION -> "my-token")
        .withBody(Json.obj(ApplicationConstants.eori -> eori))

      val result: Future[Result] = controller.getNotificationEmail.apply(request)

      status(result) shouldBe FORBIDDEN
    }

    "return 400 BadRequest when EORI is missing" in new Setup {

      successfulAuthAction(mockAuthAction)

      val request: FakeRequest[JsObject] = FakeRequest(POST, routes.UserController.getNotificationEmail.url)
        .withHeaders("Content-Type" -> "application/json", AUTHORIZATION -> "my-token")
        .withBody(Json.obj("invalidField" -> "value"))

      val result: Future[Result] = controller.getNotificationEmail.apply(request)

      status(result)        shouldBe BAD_REQUEST
      contentAsString(result) should include("Missing or invalid 'eori' field")
    }

    "return 500 InternalServerError when service fails" in new Setup {
      val eori = "GB123456789000"

      successfulAuthAction(mockAuthAction)

      when(mockUserService.getNotificationEmail(eori))
        .thenReturn(Future.failed(new RuntimeException("Service failure")))
      val request: FakeRequest[JsObject] = FakeRequest(POST, routes.UserController.getNotificationEmail.url)
        .withHeaders("Content-Type" -> "application/json", AUTHORIZATION -> "my-token")
        .withBody(Json.obj(ApplicationConstants.eori -> eori))

      val result: Future[Result] = controller.getNotificationEmail.apply(request)

      status(result)        shouldBe INTERNAL_SERVER_ERROR
      contentAsString(result) should include("Service failure")
    }
  }

  "UserController.getOrSetupUser" should {

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

      successfulAuthAction(mockAuthAction)

      val request: FakeRequest[JsObject] = FakeRequest(GET, routes.UserController.getOrSetupUser().url)
        .withHeaders("Content-Type" -> "application/json", AUTHORIZATION -> "my-token")
        .withBody(Json.obj(ApplicationConstants.eori -> eori))

      val result: Future[Result] = controller.getOrSetupUser().apply(request)

      status(result)        shouldBe CREATED
      contentAsJson(result) shouldBe Json.toJson(userDetails)
    }

    "return corresponding error code when call to auth fails" in new Setup {
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

      failingAuthAction(mockAuthAction)

      val request: FakeRequest[JsObject] = FakeRequest(GET, routes.UserController.getOrSetupUser().url)
        .withHeaders("Content-Type" -> "application/json", AUTHORIZATION -> "my-token")
        .withBody(Json.obj(ApplicationConstants.eori -> eori))

      val result: Future[Result] = controller.getOrSetupUser().apply(request)

      status(result) shouldBe FORBIDDEN
    }

    "return 400 BadRequest when EORI is missing" in new Setup {

      successfulAuthAction(mockAuthAction)

      val request: FakeRequest[JsObject] = FakeRequest(GET, routes.UserController.getOrSetupUser().url)
        .withHeaders("Content-Type" -> "application/json", AUTHORIZATION -> "my-token")
        .withBody(Json.obj("wrongField" -> "value"))

      val result: Future[Result] = controller.getOrSetupUser().apply(request)

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

      successfulAuthAction(mockAuthAction)

      val request: FakeRequest[JsObject] = FakeRequest(POST, routes.UserController.getAuthorisedEoris.url)
        .withHeaders("Content-Type" -> "application/json", AUTHORIZATION -> "my-token")
        .withBody(Json.obj(ApplicationConstants.eori -> eori))

      val result: Future[Result] = controller.getAuthorisedEoris.apply(request)

      status(result)        shouldBe OK
      contentAsJson(result) shouldBe JsArray(authorisedEoris.map(JsString(_)))
    }

    "return corresponding error code when call to auth fails" in new Setup {
      val eori                         = "GB123456789000"
      val authorisedEoris: Seq[String] = Seq("GB111111111111", "GB222222222222")

      when(mockUserService.getAuthorisedEoris(eori))
        .thenReturn(Future.successful(authorisedEoris))

      failingAuthAction(mockAuthAction)

      val request: FakeRequest[JsObject] = FakeRequest(POST, routes.UserController.getAuthorisedEoris.url)
        .withHeaders("Content-Type" -> "application/json", AUTHORIZATION -> "my-token")
        .withBody(Json.obj(ApplicationConstants.eori -> eori))

      val result: Future[Result] = controller.getAuthorisedEoris.apply(request)

      status(result) shouldBe FORBIDDEN
    }

    "return 500 InternalServerError when service fails" in new Setup {
      val eori = "GB123456789000"

      when(mockUserService.getAuthorisedEoris(eori))
        .thenReturn(Future.failed(new RuntimeException("Service failure")))

      successfulAuthAction(mockAuthAction)

      val request: FakeRequest[JsObject] = FakeRequest(POST, routes.UserController.getAuthorisedEoris.url)
        .withHeaders("Content-Type" -> "application/json", AUTHORIZATION -> "my-token")
        .withBody(Json.obj(ApplicationConstants.eori -> eori))

      val result: Future[Result] = controller.getAuthorisedEoris.apply(request)

      status(result)        shouldBe INTERNAL_SERVER_ERROR
      contentAsString(result) should include("Service failure")
    }
  }

  "UserController.getAdditionalEmails" should {

    "return 200 OK with list of additional emails" in new Setup {
      val eori                          = "GB123456789000"
      val additionalEmails: Seq[String] = Seq("email1@example.com", "email2@example.com")

      when(mockUserService.getUserAdditionalEmails(eori))
        .thenReturn(Future.successful(additionalEmails))

      successfulAuthAction(mockAuthAction)

      val request: FakeRequest[JsObject] = FakeRequest(GET, routes.UserController.getAdditionalEmails.url)
        .withHeaders("Content-Type" -> "application/json", AUTHORIZATION -> "my-token")
        .withBody(Json.obj(ApplicationConstants.eori -> eori))

      val result: Future[Result] = controller.getAdditionalEmails.apply(request)

      status(result)        shouldBe OK
      contentAsJson(result) shouldBe JsArray(additionalEmails.map(JsString(_)))
    }

    "return corresponding error code when call to auth fails" in new Setup {
      val eori                          = "GB123456789000"
      val additionalEmails: Seq[String] = Seq("email1@example.com", "email2@example.com")

      when(mockUserService.getUserAdditionalEmails(eori))
        .thenReturn(Future.successful(additionalEmails))

      failingAuthAction(mockAuthAction)

      val request: FakeRequest[JsObject] = FakeRequest(GET, routes.UserController.getAdditionalEmails.url)
        .withHeaders("Content-Type" -> "application/json", AUTHORIZATION -> "my-token")
        .withBody(Json.obj(ApplicationConstants.eori -> eori))

      val result: Future[Result] = controller.getAdditionalEmails.apply(request)

      status(result) shouldBe FORBIDDEN
    }

    "return 500 InternalServerError when service fails" in new Setup {
      val eori = "GB123456789000"

      when(mockUserService.getUserAdditionalEmails(eori))
        .thenReturn(Future.failed(new RuntimeException("Service failure")))

      when(mockAuthAction.async[JsValue](any[BodyParser[JsValue]]())(any()))
        .thenAnswer { invocation =>
          val bodyParser = invocation.getArgument[BodyParser[JsValue]](0)
          val block      = invocation.getArgument[Request[JsValue] => Future[Result]](1)
          new Action[JsValue] {
            override def apply(request: Request[JsValue]): Future[Result] = block(request)

            override def parser: BodyParser[JsValue] = bodyParser

            override def executionContext: ExecutionContext = ExecutionContext.global
          }
        }

      val request: FakeRequest[JsObject] = FakeRequest(GET, routes.UserController.getAdditionalEmails.url)
        .withHeaders("Content-Type" -> "application/json", AUTHORIZATION -> "my-token")
        .withBody(Json.obj(ApplicationConstants.eori -> eori))

      val result: Future[Result] = controller.getAdditionalEmails.apply(request)

      status(result)        shouldBe INTERNAL_SERVER_ERROR
      contentAsString(result) should include("Service failure")
    }

    "return 400 BadRequest when EORI is missing" in new Setup {

      when(mockAuthAction.async[JsValue](any[BodyParser[JsValue]]())(any()))
        .thenAnswer { invocation =>
          val bodyParser = invocation.getArgument[BodyParser[JsValue]](0)
          val block      = invocation.getArgument[Request[JsValue] => Future[Result]](1)
          new Action[JsValue] {
            override def apply(request: Request[JsValue]): Future[Result] = block(request)

            override def parser: BodyParser[JsValue] = bodyParser

            override def executionContext: ExecutionContext = ExecutionContext.global
          }
        }

      val request: FakeRequest[JsObject] = FakeRequest(GET, routes.UserController.getAdditionalEmails.url)
        .withHeaders("Content-Type" -> "application/json", AUTHORIZATION -> "my-token")
        .withBody(Json.obj("wrongField" -> "value"))

      val result: Future[Result] = controller.getAdditionalEmails.apply(request)

      status(result)        shouldBe BAD_REQUEST
      contentAsString(result) should include("Missing or invalid 'eori' field")
    }
  }

  "UserController.getUserAndEmail" should {

    "return 201 OK with user details when valid EORI is provided" in new Setup {
      val eori                     = "GB123456789000"
      val userDetails: UserDetails = UserDetails(
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

      successfulAuthAction(mockAuthAction)

      val request: FakeRequest[JsObject] = FakeRequest(GET, routes.UserController.getUserAndEmail.url)
        .withHeaders("Content-Type" -> "application/json", AUTHORIZATION -> "my-token")
        .withBody(Json.obj(ApplicationConstants.eori -> eori))
      val result: Future[Result]         = controller.getUserAndEmail.apply(request)
      status(result)        shouldBe CREATED
      contentAsJson(result) shouldBe Json.toJson(userDetails)
    }

    "return corresponding error code when call to auth fails" in new Setup {
      val eori                     = "GB123456789000"
      val userDetails: UserDetails = UserDetails(
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

      failingAuthAction(mockAuthAction)

      val request: FakeRequest[JsObject] = FakeRequest(GET, routes.UserController.getUserAndEmail.url)
        .withHeaders("Content-Type" -> "application/json", AUTHORIZATION -> "my-token")
        .withBody(Json.obj(ApplicationConstants.eori -> eori))
      val result: Future[Result]         = controller.getUserAndEmail.apply(request)
      status(result) shouldBe FORBIDDEN
    }

    "return bad request when eori missing" in new Setup {

      successfulAuthAction(mockAuthAction)

      val request: FakeRequest[JsObject] = FakeRequest(GET, routes.UserController.getUserAndEmail.url)
        .withHeaders("Content-Type" -> "application/json", AUTHORIZATION -> "my-token")
        .withBody(Json.obj("invalid" -> "invalidEori"))
      val result: Future[Result]         = controller.getUserAndEmail.apply(request)
      status(result)        shouldBe BAD_REQUEST
      contentAsString(result) should include("Missing or invalid 'eori' field")
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

      successfulAuthAction(mockAuthAction)

      when(mockUserService.transformToThirdPartyDetails(any())).thenReturn(thirdPartyDetails)

      val request: FakeRequest[JsObject] = FakeRequest(GET, routes.UserController.getAuthorisedEoris.url)
        .withHeaders(AUTHORIZATION -> "my-token")
        .withBody(Json.obj(ApplicationConstants.eori -> eori, "thirdPartyEori" -> thirdPartyEori))

      val result: Future[Result] = controller.getThirdPartyDetails.apply(request)
      status(result)        shouldBe OK
      contentAsJson(result) shouldBe Json.toJson(thirdPartyDetails)

    }

    "return corresponding error code when call to auth fails" in new Setup {

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

      failingAuthAction(mockAuthAction)

      when(mockUserService.transformToThirdPartyDetails(any())).thenReturn(thirdPartyDetails)

      val request: FakeRequest[JsObject] = FakeRequest(GET, routes.UserController.getAuthorisedEoris.url)
        .withHeaders(AUTHORIZATION -> "my-token")
        .withBody(Json.obj(ApplicationConstants.eori -> eori, "thirdPartyEori" -> thirdPartyEori))

      val result: Future[Result] = controller.getThirdPartyDetails.apply(request)
      status(result) shouldBe FORBIDDEN

    }

    "return a 404 when no authorised user is found" in new Setup {

      when(mockUserService.getAuthorisedUser(any(), any())).thenReturn(Future.successful(None))

      successfulAuthAction(mockAuthAction)

      val request: FakeRequest[JsObject] = FakeRequest(GET, routes.UserController.getAuthorisedEoris.url)
        .withHeaders(AUTHORIZATION -> "my-token")
        .withBody(Json.obj(ApplicationConstants.eori -> eori, "thirdPartyEori" -> thirdPartyEori))

      val result: Future[Result] = controller.getThirdPartyDetails.apply(request)
      status(result)        shouldBe NOT_FOUND
      contentAsString(result) should include("No authorised user found for third party EORI")
    }

    "return a bad request when eori invalid" in new Setup {

      val thirdPartyEori = "456"

      successfulAuthAction(mockAuthAction)

      val request: FakeRequest[JsObject] = FakeRequest(GET, routes.UserController.getAuthorisedEoris.url)
        .withHeaders(AUTHORIZATION -> "my-token")
        .withBody(Json.obj("thirdPartyEori" -> thirdPartyEori))

      val result: Future[Result] = controller.getThirdPartyDetails.apply(request)
      status(result)        shouldBe BAD_REQUEST
      contentAsString(result) should include("Missing or invalid 'eori' field")

    }

    "return a bad request when thirdPartyEori invalid" in new Setup {

      successfulAuthAction(mockAuthAction)

      val request: FakeRequest[JsObject] = FakeRequest(GET, routes.UserController.getAuthorisedEoris.url)
        .withHeaders(AUTHORIZATION -> "my-token")
        .withBody(Json.obj(ApplicationConstants.eori -> eori))

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

      successfulAuthAction(mockAuthAction)

      when(mockUserService.getAuthorisedBusiness(any(), any())).thenReturn(Future.successful(Some(authUser)))

      when(mockUserService.transformToThirdPartyDetails(any())).thenReturn(thirdPartyDetails)

      val request: FakeRequest[JsObject] = FakeRequest(GET, routes.UserController.getAuthorisedBusinessDetails.url)
        .withHeaders(AUTHORIZATION -> "my-token")
        .withBody(Json.obj("thirdPartyEori" -> eori, "traderEori" -> businessEori))

      val result: Future[Result] = controller.getAuthorisedBusinessDetails.apply(request)
      status(result)        shouldBe OK
      contentAsJson(result) shouldBe Json.toJson(thirdPartyDetails)

    }

    "return corrensponding error code when call to auth fails" in new Setup {

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

      failingAuthAction(mockAuthAction)

      when(mockUserService.getAuthorisedBusiness(any(), any())).thenReturn(Future.successful(Some(authUser)))

      when(mockUserService.transformToThirdPartyDetails(any())).thenReturn(thirdPartyDetails)

      val request: FakeRequest[JsObject] = FakeRequest(GET, routes.UserController.getAuthorisedBusinessDetails.url)
        .withHeaders(AUTHORIZATION -> "my-token")
        .withBody(Json.obj("thirdPartyEori" -> eori, "traderEori" -> businessEori))

      val result: Future[Result] = controller.getAuthorisedBusinessDetails.apply(request)
      status(result) shouldBe FORBIDDEN

    }

    "return a 404 when no authorised user is found" in new Setup {

      when(mockUserService.getAuthorisedBusiness(any(), any())).thenReturn(Future.successful(None))

      successfulAuthAction(mockAuthAction)

      val request: FakeRequest[JsObject] = FakeRequest(GET, routes.UserController.getAuthorisedBusinessDetails.url)
        .withHeaders(AUTHORIZATION -> "my-token")
        .withBody(Json.obj("thirdPartyEori" -> eori, "traderEori" -> businessEori))

      val result: Future[Result] = controller.getAuthorisedBusinessDetails.apply(request)
      status(result)        shouldBe NOT_FOUND
      contentAsString(result) should include("No authorised user found for the trader EORI")
    }

    "should return bad request when invalid thirdPartyEori field" in new Setup {

      successfulAuthAction(mockAuthAction)

      val request: FakeRequest[JsObject] = FakeRequest(GET, routes.UserController.getAuthorisedBusinessDetails.url)
        .withHeaders(AUTHORIZATION -> "my-token")
        .withBody(Json.obj("traderEori" -> "eori", "invalidField" -> "eori"))

      val result: Future[Result] = controller.getAuthorisedBusinessDetails.apply(request)
      status(result)        shouldBe BAD_REQUEST
      contentAsString(result) should include("Missing or invalid 'thirdPartyEori' field")
    }

    "should return bad request when invalid traderEori field" in new Setup {

      successfulAuthAction(mockAuthAction)

      val request: FakeRequest[JsObject] = FakeRequest(GET, routes.UserController.getAuthorisedBusinessDetails.url)
        .withHeaders(AUTHORIZATION -> "my-token")
        .withBody(Json.obj("invalidField" -> "eori", "thirdPartyEori" -> "eori"))

      val result: Future[Result] = controller.getAuthorisedBusinessDetails.apply(request)
      status(result)        shouldBe BAD_REQUEST
      contentAsString(result) should include("Missing or invalid 'traderEori' field")
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

      successfulAuthAction(mockAuthAction)

      val request = FakeRequest(GET, routes.UserController.getUsersByAuthorisedEoriWithStatus.url)
        .withHeaders("Content-Type" -> "application/json", AUTHORIZATION -> "my-token")
        .withBody(Json.obj("thirdPartyEori" -> authorisedEori))

      val result = controller.getUsersByAuthorisedEoriWithStatus.apply(request)
      contentAsJson(result) shouldBe Json.toJson(eoriBusinessInfos)
    }

    "return corresponding error code when call to auth fails" in new Setup {
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

      failingAuthAction(mockAuthAction)

      val request = FakeRequest(GET, routes.UserController.getUsersByAuthorisedEoriWithStatus.url)
        .withHeaders("Content-Type" -> "application/json", AUTHORIZATION -> "my-token")
        .withBody(Json.obj("thirdPartyEori" -> authorisedEori))

      val result = controller.getUsersByAuthorisedEoriWithStatus.apply(request)
      status(result) shouldBe FORBIDDEN

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

      successfulAuthAction(mockAuthAction)

      val request = FakeRequest(GET, routes.UserController.getUsersByAuthorisedEoriWithStatus.url)
        .withHeaders("Content-Type" -> "application/json", AUTHORIZATION -> "my-token")
        .withBody(Json.obj("thirdPartyEori" -> authorisedEori))

      val result = controller.getUsersByAuthorisedEoriWithStatus.apply(request)
      contentAsJson(result) shouldBe Json.toJson(eoriBusinessInfos)
    }

    "return 500 InternalServerError when the service throws an exceptions" in new Setup {
      val authorisedEori = "GB111111111111"

      when(mockUserService.getUsersByAuthorisedEoriWithStatus(authorisedEori))
        .thenReturn(Future.failed(new RuntimeException("error")))

      successfulAuthAction(mockAuthAction)

      val request = FakeRequest(GET, routes.UserController.getUsersByAuthorisedEoriWithStatus.url)
        .withHeaders("Content-Type" -> "application/json", AUTHORIZATION -> "my-token")
        .withBody(Json.obj("thirdPartyEori" -> authorisedEori))

      val result = controller.getUsersByAuthorisedEoriWithStatus.apply(request)
      status(result) shouldBe INTERNAL_SERVER_ERROR
    }

    "return bad request when invalid body" in new Setup {
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

      successfulAuthAction(mockAuthAction)

      val request = FakeRequest(GET, routes.UserController.getUsersByAuthorisedEoriWithStatus.url)
        .withHeaders("Content-Type" -> "application/json", AUTHORIZATION -> "my-token")
        .withBody(Json.obj("invalidField" -> authorisedEori))

      val result = controller.getUsersByAuthorisedEoriWithStatus.apply(request)
      status(result)        shouldBe BAD_REQUEST
      contentAsString(result) should include("Missing or invalid 'thirdPartyEori' field")
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

      successfulAuthAction(mockAuthAction)

      val request = FakeRequest(GET, routes.UserController.getUsersByAuthorisedEoriWithDateFilter.url)
        .withHeaders("Content-Type" -> "application/json", AUTHORIZATION -> "my-token")
        .withBody(Json.obj("thirdPartyEori" -> authorisedEori))

      val result = controller.getUsersByAuthorisedEoriWithDateFilter.apply(request)
      contentAsJson(result) shouldBe Json.toJson(eoriBusinessInfos)
    }

    "return corresponding error code when call to auth fails" in new Setup {
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

      failingAuthAction(mockAuthAction)

      val request = FakeRequest(GET, routes.UserController.getUsersByAuthorisedEoriWithDateFilter.url)
        .withHeaders("Content-Type" -> "application/json", AUTHORIZATION -> "my-token")
        .withBody(Json.obj("thirdPartyEori" -> authorisedEori))

      val result = controller.getUsersByAuthorisedEoriWithDateFilter.apply(request)
      status(result) shouldBe FORBIDDEN
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

      successfulAuthAction(mockAuthAction)

      val request = FakeRequest(GET, routes.UserController.getUsersByAuthorisedEoriWithDateFilter.url)
        .withHeaders("Content-Type" -> "application/json", AUTHORIZATION -> "my-token")
        .withBody(Json.obj("thirdPartyEori" -> authorisedEori))

      val result = controller.getUsersByAuthorisedEoriWithDateFilter.apply(request)
      contentAsJson(result) shouldBe Json.toJson(eoriBusinessInfos)
    }

    "return 500 InternalServerError when the service throws an exceptions" in new Setup {
      val authorisedEori = "GB111111111111"

      when(mockUserService.getUsersByAuthorisedEoriWithDateFilter(authorisedEori))
        .thenReturn(Future.failed(new RuntimeException("error")))

      successfulAuthAction(mockAuthAction)

      val request = FakeRequest(GET, routes.UserController.getUsersByAuthorisedEoriWithDateFilter.url)
        .withHeaders("Content-Type" -> "application/json", AUTHORIZATION -> "my-token")
        .withBody(Json.obj("thirdPartyEori" -> authorisedEori))

      val result = controller.getUsersByAuthorisedEoriWithDateFilter.apply(request)
      status(result) shouldBe INTERNAL_SERVER_ERROR
    }

    "return bad request when invalid body" in new Setup {
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

      successfulAuthAction(mockAuthAction)

      val request = FakeRequest(GET, routes.UserController.getUsersByAuthorisedEoriWithDateFilter.url)
        .withHeaders("Content-Type" -> "application/json", AUTHORIZATION -> "my-token")
        .withBody(Json.obj("invalidField" -> authorisedEori))

      val result = controller.getUsersByAuthorisedEoriWithDateFilter.apply(request)
      status(result)        shouldBe BAD_REQUEST
      contentAsString(result) should include("Missing or invalid 'thirdPartyEori' field")
    }
  }

  "UserController.thirdPartyAccessSelfRemoval" should {

    val traderEori     = "123"
    val thirdPartyEori = "456"

    "return an OK when authorised user deleted and third party reports removed and send email" in new Setup {

      when(mockUserService.deleteAuthorisedUser(traderEori, thirdPartyEori))
        .thenReturn(Future.successful(true))
      when(mockReportRequestRepository.deleteReportsForThirdPartyRemoval(traderEori, thirdPartyEori))
        .thenReturn(Future.successful(true))
      when(mockCustomsDataStoreConnector.getNotificationEmail(traderEori))
        .thenReturn(Future.successful(NotificationEmail("trader@example.com", LocalDateTime.now())))
      when(mockEmailConnector.sendEmailRequest(any(), any(), any())(any()))
        .thenReturn(Future.successful(Done))

      successfulAuthAction(mockAuthAction)

      val request: FakeRequest[JsObject] = FakeRequest(POST, routes.UserController.thirdPartyAccessSelfRemoval.url)
        .withHeaders("Content-Type" -> "application/json", AUTHORIZATION -> "my-token")
        .withBody(Json.obj("traderEori" -> traderEori, "thirdPartyEori" -> thirdPartyEori))

      val result: Future[Result] = controllerWithConnectors.thirdPartyAccessSelfRemoval.apply(request)

      status(result) shouldBe OK

      verify(mockEmailConnector)
        .sendEmailRequest(any(), any(), any())(any())
    }

    "return corresponding error code when call to auth fails" in new Setup {

      when(mockUserService.deleteAuthorisedUser(traderEori, thirdPartyEori))
        .thenReturn(Future.successful(true))
      when(mockReportRequestRepository.deleteReportsForThirdPartyRemoval(traderEori, thirdPartyEori))
        .thenReturn(Future.successful(true))
      when(mockCustomsDataStoreConnector.getNotificationEmail(traderEori))
        .thenReturn(Future.successful(NotificationEmail("trader@example.com", LocalDateTime.now())))
      when(mockEmailConnector.sendEmailRequest(any(), any(), any())(any()))
        .thenReturn(Future.successful(Done))

      failingAuthAction(mockAuthAction)

      val request: FakeRequest[JsObject] = FakeRequest(POST, routes.UserController.thirdPartyAccessSelfRemoval.url)
        .withHeaders("Content-Type" -> "application/json", AUTHORIZATION -> "my-token")
        .withBody(Json.obj("traderEori" -> traderEori, "thirdPartyEori" -> thirdPartyEori))

      val result: Future[Result] = controllerWithConnectors.thirdPartyAccessSelfRemoval.apply(request)

      status(result) shouldBe FORBIDDEN
    }

    "return an internal server error when authorised user delete fails" in new Setup {

      when(mockUserService.deleteAuthorisedUser(traderEori, thirdPartyEori))
        .thenReturn(Future.successful(false))

      successfulAuthAction(mockAuthAction)

      val request: FakeRequest[JsObject] = FakeRequest(POST, routes.UserController.thirdPartyAccessSelfRemoval.url)
        .withHeaders("Content-Type" -> "application/json", AUTHORIZATION -> "my-token")
        .withBody(Json.obj("traderEori" -> traderEori, "thirdPartyEori" -> thirdPartyEori))

      val result: Future[Result] = controllerWithConnectors.thirdPartyAccessSelfRemoval.apply(request)

      status(result)        shouldBe INTERNAL_SERVER_ERROR
      contentAsString(result) should include("Failed to remove third party access")
    }

    "return an internal server error when deleteReportsForThirdParty fails" in new Setup {

      when(mockUserService.deleteAuthorisedUser(traderEori, thirdPartyEori))
        .thenReturn(Future.successful(true))
      when(mockReportRequestRepository.deleteReportsForThirdPartyRemoval(traderEori, thirdPartyEori))
        .thenReturn(Future.successful(false))

      successfulAuthAction(mockAuthAction)

      val request: FakeRequest[JsObject] = FakeRequest(POST, routes.UserController.thirdPartyAccessSelfRemoval.url)
        .withHeaders("Content-Type" -> "application/json", AUTHORIZATION -> "my-token")
        .withBody(Json.obj("traderEori" -> traderEori, "thirdPartyEori" -> thirdPartyEori))

      val result: Future[Result] = controllerWithConnectors.thirdPartyAccessSelfRemoval.apply(request)

      status(result) shouldBe INTERNAL_SERVER_ERROR

      verify(mockEmailConnector, never()).sendEmailRequest(any(), any(), any())(any())
    }

    "return a bad request when invalid traderEori" in new Setup {

      successfulAuthAction(mockAuthAction)

      val request: FakeRequest[JsObject] = FakeRequest(POST, routes.UserController.thirdPartyAccessSelfRemoval.url)
        .withHeaders("Content-Type" -> "application/json", AUTHORIZATION -> "my-token")
        .withBody(Json.obj("wrongField" -> "value", "thirdPartyEori" -> thirdPartyEori))

      val result: Future[Result] = controllerWithConnectors.thirdPartyAccessSelfRemoval.apply(request)

      status(result)        shouldBe BAD_REQUEST
      contentAsString(result) should include("Missing or invalid 'traderEori' field")
    }

    "return a bad request when invalid thirdPartyEori" in new Setup {

      successfulAuthAction(mockAuthAction)

      val request: FakeRequest[JsObject] = FakeRequest(POST, routes.UserController.thirdPartyAccessSelfRemoval.url)
        .withHeaders("Content-Type" -> "application/json", AUTHORIZATION -> "my-token")
        .withBody(Json.obj("traderEori" -> traderEori, "wrongField" -> "value"))

      val result: Future[Result] = controllerWithConnectors.thirdPartyAccessSelfRemoval.apply(request)

      status(result)        shouldBe BAD_REQUEST
      contentAsString(result) should include("Missing or invalid 'thirdPartyEori' field")
    }

    "return a bad request when both EORIs are invalid" in new Setup {

      successfulAuthAction(mockAuthAction)

      val request: FakeRequest[JsObject] = FakeRequest(POST, routes.UserController.thirdPartyAccessSelfRemoval.url)
        .withHeaders("Content-Type" -> "application/json", AUTHORIZATION -> "my-token")
        .withBody(Json.obj("foo" -> "bar", "fizz" -> "buzz"))

      val result: Future[Result] = controllerWithConnectors.thirdPartyAccessSelfRemoval.apply(request)

      status(result)        shouldBe BAD_REQUEST
      contentAsString(result) should include("Missing or invalid 'traderEori', 'thirdPartyEori' fields")
    }
  }

  trait Setup {
    val app: Application = application
      .build()
  }

  def successfulAuthAction(mockAuthAction: AuthAction): OngoingStubbing[Action[JsValue]] =
    when(mockAuthAction.async[JsValue](any[BodyParser[JsValue]]())(any()))
      .thenAnswer { invocation =>
        val bodyParser = invocation.getArgument[BodyParser[JsValue]](0)
        val block      = invocation.getArgument[Request[JsValue] => Future[Result]](1)
        new Action[JsValue] {
          override def apply(request: Request[JsValue]): Future[Result] = block(request)

          override def parser: BodyParser[JsValue] = bodyParser

          override def executionContext: ExecutionContext = ExecutionContext.global
        }
      }

  def failingAuthAction(mockAuthAction: AuthAction): OngoingStubbing[Action[JsValue]] =
    when(mockAuthAction.async[JsValue](any[BodyParser[JsValue]]())(any()))
      .thenAnswer { invocation =>
        val bodyParser = invocation.getArgument[BodyParser[JsValue]](0)
        new Action[JsValue] {
          override def apply(request: Request[JsValue]): Future[Result] =
            Future.successful(Status(FORBIDDEN))

          override def parser: BodyParser[JsValue] = bodyParser

          override def executionContext: ExecutionContext = ExecutionContext.global
        }
      }
}
