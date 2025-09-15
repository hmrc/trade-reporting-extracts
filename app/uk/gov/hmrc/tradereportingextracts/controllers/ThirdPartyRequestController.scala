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

import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.internalauth.client.BackendAuthComponents
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.tradereportingextracts.models.{AccessType, AuthorisedUser}
import uk.gov.hmrc.tradereportingextracts.models.thirdParty.ThirdPartyRequest
import uk.gov.hmrc.tradereportingextracts.services.UserService
import uk.gov.hmrc.tradereportingextracts.utils.PermissionsUtil.readPermission

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ThirdPartyRequestController @Inject() (
  cc: ControllerComponents,
  userService: UserService,
  auth: BackendAuthComponents
)(implicit
  executionContext: ExecutionContext
) extends BackendController(cc) {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  def addThirdPartyRequest(): Action[JsValue] =
    auth.authorizedAction(readPermission).async(parse.json) { implicit request =>
      request.body.validate[ThirdPartyRequest] match {
        case JsSuccess(value, _) =>
          val authorisedUser = AuthorisedUser(
            eori = value.thirdPartyEORI,
            accessStart = value.accessStart,
            accessEnd = value.accessEnd,
            reportDataStart = value.reportDateStart,
            reportDataEnd = value.reportDateEnd,
            accessType = getAccessType(value.accessType),
            referenceName = value.referenceName
          )
          (for {
            thirdPartyAddedConfirmed <- userService.addAuthorisedUser(value.userEORI, authorisedUser)
            // TODO TRE-709 - Email functionality to be implemented in third party confirmation
            // userEmail      <- customsDataStoreConnector.getNotificationEmail(value.userEORI).map(_.address)
            // _              <- sendThirdPartyRegisteredEmail(userEmail)
          } yield Ok(Json.toJson(thirdPartyAddedConfirmed)))
            .recover { case ex =>
              BadRequest(Json.obj("error" -> ex.getMessage))
            }
        case JsError(_)          =>
          Future.successful(BadRequest(Json.obj("error" -> "Invalid request format")))
      }
    }

//  private def sendThirdPartyRegisteredEmail(userEmail: String) = {
//    userEmail match {
//      case userEmail =>
//        emailConnector.sendEmailRequest(
//          templateId = "tre_report_available", // TODO - Update template when TRE-709 is implemented
//          email = userEmail,
//          params = Map("reportRequestId" -> "maskedId")
//        )
//    }
//  }

  private def getAccessType(accessTypes: Set[String]): Set[AccessType] =
    accessTypes.flatMap {
      case s if s.equalsIgnoreCase("IMPORT") => Some(AccessType.IMPORTS)
      case s if s.equalsIgnoreCase("EXPORT") => Some(AccessType.EXPORTS)
      case _                                 => None
    }

  def deleteThirdPartyDetails(): Action[JsValue] =
    auth.authorizedAction(readPermission).async(parse.json) { implicit request =>
      try {
        val eoriResult           = (request.body \ "eori").validate[String]
        val thirdPartyEoriResult = (request.body \ "thirdPartyEori").validate[String]
        (eoriResult, thirdPartyEoriResult) match {
          case (JsSuccess(eori, _), JsSuccess(thirdPartyEori, _)) =>
            userService
              .deleteAuthorisedUser(eori, thirdPartyEori)
              .map {
                case true  =>
                  // userEmail      <- customsDataStoreConnector.getNotificationEmail(value.userEORI).map(_.address)
                  // _              <- sendThirdPartyRegisteredEmail(userEmail)
                  NoContent
                case false => NotFound("No authorised user found for third party EORI")
              }
              .recover { case ex: Exception =>
                InternalServerError(Json.obj("error" -> ex.getMessage))
              }
          case (JsError(_), _)                                    => Future.successful(BadRequest("Missing or invalid 'eori' field"))
          case (_, JsError(_))                                    => Future.successful(BadRequest("Missing or invalid 'thirdPartyEori' field"))
        }
      } catch {
        case ex: Exception =>
          Future.successful(InternalServerError(Json.obj("error" -> ex.getMessage)))
      }
    }
}
