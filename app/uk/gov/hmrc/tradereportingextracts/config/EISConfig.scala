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
class EISConfig @Inject() (servicesConfig: ServicesConfig, config: Configuration):

  val baseUrl: String = servicesConfig.baseUrl("eis")
  val context: String = config.get[String]("microservice.services.eis.context")
  val url: String = baseUrl + context

  val authTokenAPI1: String =
    config.get[String]("eis.auth-token")
  val authTokenAPI6: String = config.get[String]("microservice.services.eis.auth-token")
  
  val requestTraderReportMaxRetries: Int =
    config.get[Int]("microservice.services.eis.request-trader-report.max-retries")

  val requestTraderReportRetryDelay: Int =
    config.get[Int]("microservice.services.eis.request-trader-report.retry-delay")