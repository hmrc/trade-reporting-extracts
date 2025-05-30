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

import javax.inject.{Inject, Singleton}
import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

@Singleton
class AppConfig @Inject() (val config: Configuration, servicesConfig: ServicesConfig):

  val appName: String = config.get[String]("appName")

  lazy val customsDataStore: String = servicesConfig.baseUrl("customs-data-store") +
    config.get[String]("microservice.services.customs-data-store.context")

  lazy val eisAuthToken: String        = config.get[String]("microservice.services.eis.auth-token")
  lazy val etmpAuthToken: String       = config.get[String]("etmp.auth-token")
  lazy val sdesAuthToken: String       = config.get[String]("sdes.auth-token")
  lazy val sdesInformationType: String = config.get[String]("sdes.information-type")
  lazy val treXClientId: String        = config.get[String]("sdes.x-client-id")

  lazy val eis: String  = servicesConfig.baseUrl("eis") + config.get[String]("microservice.services.eis.context")
  lazy val sdes: String = servicesConfig.baseUrl("sdes") + config.get[String]("microservice.services.sdes.context")

  lazy val eisRequestTraderReportMaxRetries: Int =
    config.get[Int]("microservice.services.eis.request-trader-report.max-retries")

  // need to remove once we have a correct way to find end of report download
  lazy val reportFilesPart: String = config.get[String]("report-files.part")
