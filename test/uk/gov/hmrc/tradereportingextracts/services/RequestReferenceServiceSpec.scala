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

package uk.gov.hmrc.tradereportingextracts.services

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers.{all, be, must, mustBe, not, startWith}

class RequestReferenceServiceSpec extends AnyFreeSpec {

  "RequestReferenceService" - {
    val service = new RequestReferenceService()
    "generate a random reference with 'RE-' prefix and 8 digits" in {

      val reference = service.random()

      reference must startWith("RE-")
      reference.length mustBe 11

      val digitsPart = reference.stripPrefix("RE-")
      all(digitsPart.toList) must (be >= '0' and be <= '9')
    }
  }
}
