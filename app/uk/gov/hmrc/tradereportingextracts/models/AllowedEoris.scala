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

trait AllowedEoris {
  val allowedEoris: Seq[String] = Seq(
    "GB123456789012",
    "GB123456789013",
    "GB123456789014",
    "GB123456789015",
    "GB123456789016",
    "GB123456789017",
    "GB123456789018",
    "GB123456789019",
    "GB123456789020"
  )
}
