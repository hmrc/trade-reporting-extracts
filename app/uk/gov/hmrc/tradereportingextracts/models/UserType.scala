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

package uk.gov.hmrc.tradereportingextracts.models

import play.api.libs.json.{Format, JsError, JsResult, JsString, JsSuccess, JsValue}

enum UserType:
  case Trader, Agent

object UserType:
  given Format[UserType] with
    def writes(userType: UserType): JsValue = JsString(userType.toString)

    def reads(json: JsValue): JsResult[UserType] = json match
      case JsString(value) =>
        values.find(_.toString == value) match
          case Some(userType) => JsSuccess(userType)
          case None           => JsError(s"Unknown UserType: $value")
      case _               => JsError("UserType must be a string")
