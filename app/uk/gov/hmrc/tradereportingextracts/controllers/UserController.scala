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

import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.internalauth.client.*
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.tradereportingextracts.models.AuthorisedUser
import uk.gov.hmrc.tradereportingextracts.models.thirdParty.EoriBusinessInfo
import uk.gov.hmrc.tradereportingextracts.repositories.ReportRequestRepository
import uk.gov.hmrc.tradereportingextracts.services.UserService
import uk.gov.hmrc.tradereportingextracts.utils.ApplicationConstants.eori
import uk.gov.hmrc.tradereportingextracts.utils.JsonValidationHelper.validateFields
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

  def getOrSetupUser(): Action[JsValue] = auth.authorizedAction(readPermission).async(parse.json) { implicit request =>
    validateFields(
      "eori" -> (request.body \ eori).validate[String]
    ) match {

      case Right(values) =>
        val eori = values("eori")

        userService.getOrCreateUser(eori).map { userDetails =>
          Created(Json.toJson(userDetails))
        }

      case Left(errorResult) =>
        Future.successful(errorResult)
    }
  }

  def getAuthorisedEoris: Action[JsValue] =
    auth.authorizedAction(readPermission).async(parse.json) { implicit request =>
      validateFields(
        "eori" -> (request.body \ eori).validate[String]
      ) match {

        case Right(values) =>
          val eori = values("eori")

          userService
            .getAuthorisedEoris(eori)
            .map { authorisedEoris =>
              Ok(Json.toJson(authorisedEoris))
            }
            .recover { case e: Exception =>
              InternalServerError(e.getMessage)
            }

        case Left(errorResult) =>
          Future.successful(errorResult)
      }
    }

  def getNotificationEmail: Action[JsValue] =
    auth.authorizedAction(readPermission).async(parse.json) { implicit request =>
      validateFields(
        "eori" -> (request.body \ eori).validate[String]
      ) match {

        case Right(values) =>
          val eori = values("eori")

          userService
            .getNotificationEmail(eori)
            .map(email => Ok(Json.toJson(email)))
            .recover { case e: Exception =>
              InternalServerError(e.getMessage)
            }

        case Left(errorResult) =>
          Future.successful(errorResult)
      }
    }

  def getUserAndEmail: Action[JsValue] =
    auth.authorizedAction(readPermission).async(parse.json) { implicit request =>
      validateFields(
        "eori" -> (request.body \ eori).validate[String]
      ) match {

        case Right(values) =>
          val eori = values("eori")

          userService
            .getUserAndEmailDetails(eori)
            .map { userDetails =>
              Created(Json.toJson(userDetails))
            }

        case Left(errorResult) =>
          Future.successful(errorResult)
      }
    }

  def getThirdPartyDetails: Action[JsValue] =
    auth.authorizedAction(readPermission).async(parse.json) { implicit request =>
      validateFields(
        "eori"           -> (request.body \ eori).validate[String],
        "thirdPartyEori" -> (request.body \ "thirdPartyEori").validate[String]
      ) match {

        case Right(values) =>
          val eori           = values("eori")
          val thirdPartyEori = values("thirdPartyEori")

          userService
            .getAuthorisedUser(eori, thirdPartyEori)
            .map {
              case Some(authorisedUser) =>
                Ok(Json.toJson(userService.transformToThirdPartyDetails(authorisedUser)))

              case None =>
                NotFound("No authorised user found for third party EORI")
            }

        case Left(errorResult) =>
          Future.successful(errorResult)
      }
    }

  def getAuthorisedBusinessDetails: Action[JsValue] =
    auth.authorizedAction(readPermission).async(parse.json) { implicit request =>
      validateFields(
        "thirdPartyEori" -> (request.body \ "thirdPartyEori").validate[String],
        "traderEori"     -> (request.body \ "traderEori").validate[String]
      ) match {

        case Right(values) =>
          val thirdPartyEori = values("thirdPartyEori")
          val traderEori     = values("traderEori")

          userService
            .getAuthorisedBusiness(thirdPartyEori, traderEori)
            .map {
              case Some(authorisedUser) =>
                Ok(Json.toJson(userService.transformToThirdPartyDetails(authorisedUser)))

              case None =>
                NotFound("No authorised user found for the trader EORI")
            }

        case Left(errorResult) =>
          Future.successful(errorResult)
      }
    }

  def getUsersByAuthorisedEoriWithStatus: Action[JsValue] =
    auth.authorizedAction(readPermission).async(parse.json) { implicit request =>
      validateFields(
        "thirdPartyEori" -> (request.body \ "thirdPartyEori").validate[String]
      ) match {

        case Right(values) =>
          val thirdPartyEori = values("thirdPartyEori")

          userService
            .getUsersByAuthorisedEoriWithStatus(thirdPartyEori)
            .map(eoriInfos => Ok(Json.toJson(eoriInfos)))
            .recover { case _ =>
              InternalServerError("Failed to fetch users by authorised EORI with status")
            }

        case Left(errorResult) =>
          Future.successful(errorResult)
      }
    }

  def getUsersByAuthorisedEoriWithDateFilter: Action[JsValue] =
    auth.authorizedAction(readPermission).async(parse.json) { implicit request =>
      validateFields(
        "thirdPartyEori" -> (request.body \ "thirdPartyEori").validate[String]
      ) match {

        case Right(values) =>
          val thirdPartyEori = values("thirdPartyEori")

          userService
            .getUsersByAuthorisedEoriWithDateFilter(thirdPartyEori)
            .map(eoriInfos => Ok(Json.toJson(eoriInfos)))
            .recover { case _ =>
              InternalServerError("Failed to fetch users by authorised EORI with date filter")
            }

        case Left(errorResult) =>
          Future.successful(errorResult)
      }
    }

  def thirdPartyAccessSelfRemoval: Action[JsValue] =
    auth.authorizedAction(writePermission).async(parse.json) { implicit request =>
      validateFields(
        "traderEori"     -> (request.body \ "traderEori").validate[String],
        "thirdPartyEori" -> (request.body \ "thirdPartyEori").validate[String]
      ) match {

        case Right(values) =>
          val traderEori     = values("traderEori")
          val thirdPartyEori = values("thirdPartyEori")

          userService
            .deleteAuthorisedUser(traderEori, thirdPartyEori)
            .flatMap {
              case true =>
                reportRequestRepository
                  .deleteReportsForThirdPartyRemoval(traderEori, thirdPartyEori)
                  .map {
                    case true  => Ok
                    case false => InternalServerError("Failed to remove reports for third party access removal")
                  }

              case false =>
                Future.successful(InternalServerError("Failed to remove third party access"))
            }
            .recover { case _ =>
              InternalServerError("Failed to remove third party access removal request")
            }

        case Left(errorResult) =>
          Future.successful(errorResult)
      }
    }

    def getAdditionalEmails: Action[JsValue] =
      auth.authorizedAction(readPermission).async(parse.json) { implicit request =>
        (request.body \ eori).validate[String] match {
          case JsSuccess(eori, _) =>
            userService
              .getUserAdditionalEmails(eori)
              .map { additionalEmails =>
                Ok(Json.toJson(additionalEmails))
              }
              .recover { case e: Exception =>
                InternalServerError(e.getMessage)
              }
          case JsError(_)         =>
            Future.successful(BadRequest("Missing or invalid 'eori' field"))
        }
      }
