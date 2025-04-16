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

enum AccessType:
  case Importer, Exporter, Declarant

object AccessType:
  given Format[AccessType] with
    def writes(accessType: AccessType): JsValue = JsString(accessType.toString)

    def reads(json: JsValue): JsResult[AccessType] = json match
      case JsString(value) =>
        values.find(_.toString == value) match
          case Some(accessType) => JsSuccess(accessType)
          case None             => JsError(s"Unknown AccessType: $value")
      case _               => JsError("AccessType must be a string")
