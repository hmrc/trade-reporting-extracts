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

package uk.gov.hmrc.tradereportingextracts.utils

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

class HttpDateFormatterSpec extends AnyFreeSpec with Matchers {
  "HttpDateFormatter" - {

    "must format current date in HTTP date format" in {
      val httpDate = HttpDateFormatter.getCurrentHttpDate
      httpDate must fullyMatch regex """[A-Za-z]{3}, \d{2} [A-Za-z]{3,4} \d{4} \d{2}:\d{2}:\d{2} GMT"""
    }
  }
}
