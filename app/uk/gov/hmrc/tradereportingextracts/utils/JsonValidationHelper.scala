/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.tradereportingextracts.utils

import play.api.libs.json.{JsError, JsResult, JsSuccess}
import play.api.mvc.{Result, Results}

object JsonValidationHelper extends Results {

  def validateFields(
    fields: (String, JsResult[String])*
  ): Either[Result, Map[String, String]] = {

    val invalidFields =
      fields.collect {
        case (name, JsError(_))                                => name
        case (name, JsSuccess(value, _)) if value.trim.isEmpty => name
      }

    if (invalidFields.nonEmpty) {

      val fieldPart =
        if (invalidFields.size == 1)
          s"'${invalidFields.head}' field"
        else
          invalidFields.map(f => s"'$f'").mkString(", ") + " fields"

      Left(BadRequest(s"Missing or invalid $fieldPart"))
    } else {
      Right(
        fields.collect { case (name, JsSuccess(value, _)) =>
          name -> value
        }.toMap
      )
    }
  }
}
