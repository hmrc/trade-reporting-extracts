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

package uk.gov.hmrc.tradereportingextracts.models.thirdParty

import play.api.libs.json.{Json, OFormat}

import java.time.Instant

case class EditThirdPartyRequest(
                              userEORI: String,
                              thirdPartyEORI: String,
                              accessStart: Option[Instant],
                              accessEnd: Option[Instant],
                              reportDateStart: Option[Instant],
                              reportDateEnd: Option[Instant],
                              accessType: Option[Set[String]],
                              referenceName: Option[String]
                            )

object EditThirdPartyRequest {
  implicit val format: OFormat[EditThirdPartyRequest] = Json.format[EditThirdPartyRequest]
}
