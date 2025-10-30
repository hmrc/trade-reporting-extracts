/*
 * Copyright 2024 HM Revenue & Customs
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

import org.apache.pekko.Done
import play.api.libs.json.Json
import play.api.libs.ws.writeableOf_JsValue
import play.api.{Configuration, Logging}
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

abstract class InternalAuthTokenInitializer {
  val initialised: Future[Done]
}

@Singleton
class NoOpInternalAuthTokenInitializer @Inject() () extends InternalAuthTokenInitializer {
  override val initialised: Future[Done] = Future.successful(Done)
}

@Singleton
class InternalAuthTokenInitializerImpl @Inject() (
  configuration: Configuration,
  servicesConfig: ServicesConfig,
  httpClient: HttpClientV2
)(implicit ec: ExecutionContext)
    extends InternalAuthTokenInitializer
    with Logging {

  private val authToken: String =
    configuration.get[String]("internal-auth.token")

  override val initialised: Future[Done] =
    setup()

  private def setup(): Future[Done] = for {
    _ <- ensureAuthToken()
  } yield Done

  private def ensureAuthToken(): Future[Done] =
    authTokenIsValid.flatMap { isValid =>
      if (isValid) {
        logger.info("Auth token is already valid")
        Future.successful(Done)
      } else {
        createClientAuthToken()
      }
    }

  private def createClientAuthToken(): Future[Done] = {
    logger.info("Initialising auth token")
    httpClient
      .post(url"${servicesConfig.baseUrl("internal-auth")}/test-only/token")(HeaderCarrier())
      .withBody(
        Json.obj(
          "token"       -> authToken,
          "principal"   -> "trade-reporting-extracts-frontend",
          "permissions" -> Seq(
            Json.obj(
              "resourceType"     -> "trade-reporting-extracts",
              "resourceLocation" -> "trade-reporting-extracts/*",
              "actions"          -> List("WRITE", "READ", "DELETE")
            ),
            Json.obj(
              "resourceType"     -> "user-allow-list",
              "resourceLocation" -> "trade-reporting-extracts-frontend",
              "actions"          -> List("READ")
            )
          )
        )
      )
      .execute
      .flatMap { response =>
        if (response.status == 201) {
          logger.info("Auth token initialised")
          Future.successful(Done)
        } else {
          Future.failed(new RuntimeException("Unable to initialise internal-auth token"))
        }
      }
  }

  private def authTokenIsValid: Future[Boolean] = {
    logger.info("Checking auth token")
    httpClient
      .get(url"${servicesConfig.baseUrl("internal-auth")}/test-only/token")(HeaderCarrier())
      .setHeader("Authorization" -> authToken)
      .execute
      .map(_.status == 200)
  }
}
