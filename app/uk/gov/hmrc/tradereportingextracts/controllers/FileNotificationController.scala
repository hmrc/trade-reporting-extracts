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
import uk.gov.hmrc.tradereportingextracts.models.sdes.FileNotificationResponse
import uk.gov.hmrc.tradereportingextracts.services.FileNotificationService

import javax.inject.{Inject, Singleton}

@Singleton
class FileNotificationController @Inject() (
  cc: ControllerComponents,
  fileNotificationService: FileNotificationService
) extends AbstractController(cc) {

  def fileNotification(): Action[AnyContent] = Action { request =>
    request.body.asJson match {
      case None       =>
        BadRequest("Expected application/json request body")
      case Some(json) =>
        json.validate[FileNotificationResponse] match {
          case JsSuccess(fileNotification, _) =>
            fileNotificationService.processFileNotification(fileNotification)
            Created("Accepted")
          case JsError(errors)                =>
            val errorMessage = errors
              .map { case (path, validationErrors) =>
                s"Invalid value at path $path: ${validationErrors.map(_.message).mkString(", ")}"
              }
              .mkString(", ")
            BadRequest(errorMessage)
        }
    }
  }
}
