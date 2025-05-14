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

package uk.gov.hmrc.tradereportingextracts.models.eis

import play.api.Logging
import play.api.http.Status.*
import play.api.libs.json.*
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

import scala.reflect.ClassTag

object EisHttpReader extends Logging {

  case class StatusHttpReader(reportRequestId: String, errorHandler: (HttpResponse, String) => EisHttpErrorResponse)
      extends HttpReads[Either[EisHttpErrorResponse, HttpResponse]] {
    override def read(method: String, url: String, response: HttpResponse): Either[EisHttpErrorResponse, HttpResponse] =
      response match {
        case response if isSuccessful(response.status) => Right(response)
        case response                                  =>
          logger.warn(
            s"[StatusHttpReader] - Downstream error, method: $method, url: $url, reportRequestId: $reportRequestId, body: ${response.body}"
          )
          Left(errorHandler(response, reportRequestId))
      }
  }

  def isSuccessful(status: Int): Boolean = status >= 200 && status < 300
}
