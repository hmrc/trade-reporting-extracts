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

package uk.gov.hmrc.tradereportingextracts.config

import org.scalatest.matchers.should.Matchers
import play.api.Application
import uk.gov.hmrc.tradereportingextracts.utils.SpecBase

class AppConfigSpec extends SpecBase with Matchers {

  override def fakeApplication(): Application = application.build()

  private lazy val appConfig: AppConfig = app.injector.instanceOf[AppConfig]

  "AppConfig" should {

    "return the correct appName" in {
      appConfig.appName shouldBe "trade-reporting-extracts"
    }

    "return the correct customsDataStore URL" in {
      appConfig.customsDataStore shouldBe "http://localhost:2101/trade-reporting-extracts-stub"
    }

    "return correct eis" in {
      appConfig.eis shouldBe "http://localhost:2101/trade-reporting-extracts-stub/gbe/requesttraderreport/v1"
    }
  }
}
