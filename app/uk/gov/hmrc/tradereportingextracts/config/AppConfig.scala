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

  val reportRequestTTLDays: Long = config.get[Long]("mongodb.reportRequestTTLInDays")
  var userTTLDays: Long          = config.get[Long]("mongodb.userTTLInDays")

  lazy val customsDataStore: String = servicesConfig.baseUrl("customs-data-store") +
    config.get[String]("microservice.services.customs-data-store.context")
  lazy val verifiedEmailUrl: String =
    customsDataStore + config.get[String]("microservice.services.customs-data-store.verified-email")

  lazy val companyInformationUrl: String =
    customsDataStore + config.get[String]("microservice.services.customs-data-store.company-information")

  lazy val eoriHistoryUrl: String =
    customsDataStore + config.get[String]("microservice.services.customs-data-store.eori-history")

  lazy val eisAuthToken: String     = config.get[String]("microservice.services.eis.auth-token")
  lazy val etmpAuthToken: String    = config.get[String]("etmp.auth-token")
  lazy val eisAPI6AuthToken: String = config.get[String]("eis.auth-token")

  lazy val eis: String                 = servicesConfig.baseUrl("eis") + config.get[String]("microservice.services.eis.context")
  lazy val sdes: String                = servicesConfig.baseUrl("sdes") + config.get[String]("microservice.services.sdes.context")
  lazy val sdesInformationType: String = config.get[String]("microservice.services.sdes.information-type")
  lazy val treXClientId: String        = config.get[String]("microservice.services.sdes.x-client-id")
  lazy val email: String               = servicesConfig.baseUrl("email")

  lazy val eisRequestTraderReportMaxRetries: Int =
    config.get[Int]("microservice.services.eis.request-trader-report.max-retries")
  lazy val eisRequestTraderReportRetryDelay: Int =
    config.get[Int]("microservice.services.eis.request-trader-report.retry-delay")

  lazy val dailySubmissionLimit: Int = config.get[Int]("reportRequest.dailySubmissionLimit")
