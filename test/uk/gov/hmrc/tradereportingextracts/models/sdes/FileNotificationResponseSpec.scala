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
import play.api.libs.json.{JsValue, Json}

class FileNotificationResponseSpec extends AnyWordSpec with Matchers {

  "FileNotificationResponse JSON format" should {

    "serialize and deserialize with empty metadata" in {
      val model = FileNotificationResponse(
        eori = "GB123456789000",
        fileName = "report.csv",
        fileSize = 1024L,
        metadata = Nil
      )

      val json = Json.toJson(model)

      (json \ "eori").as[String]           shouldBe "GB123456789000"
      (json \ "fileName").as[String]       shouldBe "report.csv"
      (json \ "fileSize").as[Long]         shouldBe 1024L
      (json \ "metadata").as[Seq[JsValue]] shouldBe empty

      val parsed = json.as[FileNotificationResponse]
      parsed shouldBe model
    }
  }
}
