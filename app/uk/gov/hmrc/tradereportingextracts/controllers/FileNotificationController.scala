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

import play.api.libs.json.*
import play.api.mvc.*
import uk.gov.hmrc.tradereportingextracts.config.AppConfig
import uk.gov.hmrc.tradereportingextracts.models.FileNotification as TreFileNotication
import uk.gov.hmrc.tradereportingextracts.models.sdes.FileNotificationHeaders.*
import uk.gov.hmrc.tradereportingextracts.models.sdes.FileNotificationResponse

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.tradereportingextracts.models.{FileType, ReportTypeName}
import uk.gov.hmrc.tradereportingextracts.models.sdes.FileNotificationMetadata
import uk.gov.hmrc.tradereportingextracts.services.{FileNotificationService, ReportRequestService}
@Singleton
class FileNotificationController @Inject() (
  cc: ControllerComponents,
  appConfig: AppConfig,
  reportRequestService: ReportRequestService,
  fileNotificationService: FileNotificationService
)(implicit ec: ExecutionContext)
    extends AbstractController(cc) {

  def fileNotification(): Action[AnyContent] = Action.async { request =>
    def missingHeaders: Seq[String] =
      allHeaders.filterNot(header => request.headers.get(header).isDefined)

    def isAuthorized: Boolean =
      request.headers.get(Authorization.toString).getOrElse("").equals(appConfig.sdesAuthToken)

    (missingHeaders, isAuthorized, request.body.asJson) match {
      case (headers, _, _) if headers.nonEmpty =>
        Future.successful(BadRequest(s"Failed header validation: Missing headers: ${headers.mkString(", ")}"))
      case (_, false, _)                       =>
        Future.successful(Forbidden)
      case (_, _, None)                        =>
        Future.successful(BadRequest("Expected application/json request body"))
      case (_, _, Some(json))                  =>
        json.validate[FileNotificationResponse] match {
          case JsSuccess(fileNotification, _) =>
            fileNotificationService.processFileNotification(fileNotification).map { (status, message) =>
              Status(status)(message)
            }
          case JsError(errors)                =>
            val errorMessage = errors
              .map { case (path, validationErrors) =>
                s"Invalid value at path $path: ${validationErrors.map(_.message).mkString(", ")}"
              }
              .mkString(", ")
            Future.successful(BadRequest(errorMessage))
        }
    }
  }

  def serverOtherMethods(): Action[AnyContent] = Action.async { request =>
    Future.successful(
      MethodNotAllowed(
        s"Method ${request.method} not allowed. Only PUT is allowed for this endpoint."
      )
    )
  }
}
