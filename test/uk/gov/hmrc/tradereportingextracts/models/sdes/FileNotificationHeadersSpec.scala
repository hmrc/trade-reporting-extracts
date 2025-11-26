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

package uk.gov.hmrc.tradereportingextracts.models.sdes

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class FileNotificationHeadersSpec extends AnyWordSpec with Matchers {
  "FileNotificationHeaders" should {
    "expose the expected header keys" in {
      val expected = Set(
        "authorization",
        "content-type",
        "date",
        "x-correlation-id",
        "x-transmitting-system",
        "source-system"
      )

      FileNotificationHeaders.allHeaders.toSet             shouldBe expected
      FileNotificationHeaders.values.map(_.toString).toSet shouldBe expected
    }

    "have no duplicate entries in allHeaders" in {
      val headers = FileNotificationHeaders.allHeaders
      headers.distinct.size shouldBe headers.size
    }

    "report the correct number of headers" in {
      FileNotificationHeaders.allHeaders.size shouldBe FileNotificationHeaders.values.size
    }
  }
}
