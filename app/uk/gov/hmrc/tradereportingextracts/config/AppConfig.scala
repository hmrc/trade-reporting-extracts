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
class AppConfig @Inject() (
  val config: Configuration,
  servicesConfig: ServicesConfig,
  eisConfig: EISConfig,
  sdesConfig: SdesConfig,
  customsDataStoreConfig: CustomsDataStoreConfig
):

  val appName: String = config.get[String]("appName")

  val reportRequestTTLDays: Long = config.get[Long]("mongodb.reportRequestTTLInDays")
  var userTTLDays: Long          = config.get[Long]("mongodb.userTTLInDays")

  lazy val customsDataStore: String      = customsDataStoreConfig.url
  lazy val verifiedEmailUrl: String      = customsDataStoreConfig.verifiedEmailUrl
  lazy val companyInformationUrl: String = customsDataStoreConfig.companyInformationUrl
  lazy val eoriHistoryUrl: String        = customsDataStoreConfig.eoriHistoryUrl

  lazy val eisAPI1AuthToken: String    = eisConfig.authTokenAPI1
  lazy val eisAPI6AuthToken: String    = eisConfig.authTokenAPI6
  lazy val eoriUpdateAuthToken: String = config.get[String]("etmp.auth-token")

  lazy val eis: String          = eisConfig.url
  lazy val sdes: String         = sdesConfig.url
  lazy val treXClientId: String = sdesConfig.treXClientId
  lazy val email: String        = servicesConfig.baseUrl("email") + "/hmrc/email"

  lazy val eisRequestTraderReportMaxRetries: Int = eisConfig.requestTraderReportMaxRetries
  lazy val eisRequestTraderReportRetryDelay: Int = eisConfig.requestTraderReportRetryDelay

  lazy val dailySubmissionLimit: Int = config.get[Int]("reportRequest.dailySubmissionLimit")
