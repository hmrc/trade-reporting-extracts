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
import uk.gov.hmrc.internalauth.client.*
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.tradereportingextracts.models.AuthorisedUser
import uk.gov.hmrc.tradereportingextracts.models.thirdParty.EoriBusinessInfo
import uk.gov.hmrc.tradereportingextracts.repositories.ReportRequestRepository
import uk.gov.hmrc.tradereportingextracts.services.UserService
import uk.gov.hmrc.tradereportingextracts.utils.PermissionsUtil.{readPermission, writePermission}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton()
class UserController @Inject() (
  userService: UserService,
  cc: ControllerComponents,
  auth: BackendAuthComponents,
  reportRequestRepository: ReportRequestRepository
)(using executionContext: ExecutionContext)
    extends BackendController(cc):

  def setupUser(): Action[JsValue] = auth.authorizedAction(readPermission).async(parse.json) { implicit request =>
    (request.body \ "eori").validate[String] match {
      case JsSuccess(eori, _) =>
        userService.getOrCreateUser(eori).map { userDetails =>
          Created(Json.toJson(userDetails))
        }
      case JsError(_)         =>
        Future.successful(BadRequest("Missing or invalid 'eori' field"))
    }
  }

  def getAuthorisedEoris: Action[JsValue] =
    auth.authorizedAction(readPermission).async(parse.json) { implicit request =>
      (request.body \ "eori").validate[String] match {
        case JsSuccess(eori, _) =>
          userService
            .getAuthorisedEoris(eori)
            .map { authorisedEoris =>
              Ok(Json.toJson(authorisedEoris))
            }
            .recover { case e: Exception =>
              InternalServerError(e.getMessage)
            }
        case JsError(_)         =>
          Future.successful(BadRequest("Missing or invalid 'eori' field"))
      }
    }

  def getNotificationEmail: Action[JsValue] =
    auth.authorizedAction(readPermission).async(parse.json) { implicit request =>
      (request.body \ "eori").validate[String] match {
        case JsSuccess(eori, _) =>
          userService
            .getNotificationEmail(eori)
            .map(email => Ok(Json.toJson(email)))
            .recover { case e: Exception =>
              InternalServerError(e.getMessage)
            }
        case JsError(_)         =>
          Future.successful(BadRequest("Missing or invalid 'eori' field"))
      }
    }

  def getUserAndEmail: Action[JsValue] = auth.authorizedAction(readPermission).async(parse.json) { implicit request =>
    (request.body \ "eori").validate[String] match {
      case JsSuccess(eori, _) =>
        userService.getUserAndEmailDetails(eori).map { userDetails =>
          Created(Json.toJson(userDetails))
        }
      case JsError(_)         =>
        Future.successful(BadRequest("Missing or invalid 'eori' field"))
    }
  }

  def getThirdPartyDetails: Action[JsValue] =
    auth.authorizedAction(readPermission).async(parse.json) { implicit request =>
      val eoriResult           = (request.body \ "eori").validate[String]
      val thirdPartyEoriResult = (request.body \ "thirdPartyEori").validate[String]

      (eoriResult, thirdPartyEoriResult) match {
        case (JsSuccess(eori, _), JsSuccess(thirdPartyEori, _)) =>
          userService.getAuthorisedUser(eori, thirdPartyEori).map {
            case Some(authorisedUser) => Ok(Json.toJson(userService.transformToThirdPartyDetails(authorisedUser)))
            case None                 => NotFound(s"No authorised user found for third party EORI:")
          }
        case (JsError(_), _)                                    => Future.successful(BadRequest("Missing or invalid 'eori' field"))
        case (_, JsError(_))                                    => Future.successful(BadRequest("Missing or invalid 'thirdPartyEori' field"))
      }
    }

  def getAuthorisedBusinessDetails: Action[JsValue] =
    auth.authorizedAction(readPermission).async(parse.json) { implicit request =>
      val eoriResult           = (request.body \ "thirdPartyEori").validate[String]
      val thirdPartyEoriResult = (request.body \ "traderEori").validate[String]

      (eoriResult, thirdPartyEoriResult) match {
        case (JsSuccess(thirdPartyEori, _), JsSuccess(traderEori, _)) =>
          userService.getAuthorisedBusiness(thirdPartyEori, traderEori).map {
            case Some(authorisedUser) => Ok(Json.toJson(userService.transformToThirdPartyDetails(authorisedUser)))
            case None                 => NotFound(s"No authorised user found for the trader EORI:")
          }
        case (JsError(_), _)                                          => Future.successful(BadRequest("Missing or invalid 'thirdPartyEori' field"))
        case (_, JsError(_))                                          => Future.successful(BadRequest("Missing or invalid 'traderEori' field"))
      }
    }

  def getUsersByAuthorisedEoriWithStatus: Action[JsValue] =
    auth.authorizedAction(readPermission).async(parse.json) { implicit request =>
      (request.body \ "thirdPartyEori").validate[String] match {
        case JsSuccess(thirdPartyEori, _) =>
          userService
            .getUsersByAuthorisedEoriWithStatus(thirdPartyEori)
            .map(eoriInfos => Ok(Json.toJson(eoriInfos)))
            .recover { case e: Exception =>
              InternalServerError(e.getMessage)
            }

        case JsError(_) =>
          Future.successful(BadRequest("Missing or invalid 'thirdPartyEori' field"))
      }
    }

  def getUsersByAuthorisedEoriWithDateFilter: Action[JsValue] =
    auth.authorizedAction(readPermission).async(parse.json) { implicit request =>
      (request.body \ "thirdPartyEori").validate[String] match {
        case JsSuccess(thirdPartyEori, _) =>
          userService
            .getUsersByAuthorisedEoriWithDateFilter(thirdPartyEori)
            .map(eoriInfos => Ok(Json.toJson(eoriInfos)))
            .recover { case e: Exception =>
              InternalServerError(e.getMessage)
            }

        case JsError(_) =>
          Future.successful(BadRequest("Missing or invalid 'thirdPartyEori' field"))
      }
    }

  def thirdPartyAccessSelfRemoval: Action[JsValue] =
    auth.authorizedAction(writePermission).async(parse.json) { implicit request =>
      ((request.body \ "traderEori").validate[String], (request.body \ "thirdPartyEori").validate[String]) match {
        case (JsSuccess(traderEori, _), JsSuccess(thirdPartyEori, _)) =>
          userService.deleteAuthorisedUser(traderEori, thirdPartyEori).flatMap {
            case true  =>
              reportRequestRepository.deleteReportsForThirdPartyRemoval(traderEori, thirdPartyEori).map {
                case true  => Ok
                case false => InternalServerError("Failed to remove reports for third party access removal")
              }
            case false => Future.successful(InternalServerError("Failed to remove third party access"))
          }
        case (JsError(_), JsError(_))                                 =>
          Future.successful(BadRequest("Missing or invalid 'traderEori' and 'thirdPartyEori' fields"))
        case (JsError(_), _)                                          =>
          Future.successful(BadRequest("Missing or invalid 'traderEori' field"))
        case (_, JsError(_))                                          =>
          Future.successful(BadRequest("Missing or invalid 'thirdPartyEori' field"))
      }
    }
