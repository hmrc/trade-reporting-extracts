/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.tradereportingextracts.connectors

import uk.gov.hmrc.tradereportingextracts.connectors.ConnectorFailureLogger.FromResultToConnectorFailureLogger
import org.scalatest.{OptionValues, TryValues}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import uk.gov.hmrc.http.{JsValidationException, UpstreamErrorResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ConnectorFailureLoggerSpec extends AnyFreeSpec with Matchers with ScalaFutures with OptionValues with TryValues {

  "ConnectorFailureLogger" - {

    "log when upstream error response is received" in {

      val futureFailingResponse = Future.failed[Nothing](UpstreamErrorResponse("Error", 500))

      val loggedFuture = futureFailingResponse.logFailureReason("fooConnector")

      whenReady(loggedFuture.failed) { exception =>
        exception mustBe an[UpstreamErrorResponse]
        exception.asInstanceOf[UpstreamErrorResponse].statusCode mustBe 500
      }
    }

    "log when JsValidationException is received" in {
      val futureFailingResponse = Future.failed[Nothing](
        new JsValidationException("GET", "http://test.url", classOf[SomeResponseType], "Invalid format")
      )

      val loggedFuture = futureFailingResponse.logFailureReason("fooConnector")

      whenReady(loggedFuture.failed) { exception =>
        exception `mustBe` a[JsValidationException]
        exception.getMessage `must` `include`(
          "GET of 'http://test.url' returned invalid json. Attempting to convert to"
        )
        exception.getMessage `must` `include`("SomeResponseType")
        exception.getMessage `must` `include`("gave errors: Invalid format")
      }
    }

    "log when when anything else is received" in {
      val futureFailingResponse = Future.failed[Nothing](new Exception("Generic exception"))

      val loggedFuture = futureFailingResponse.logFailureReason("fooConnector")

      whenReady(loggedFuture.failed) { exception =>
        exception `mustBe` an[Exception]
        exception.getMessage `mustBe` "Generic exception"
      }
    }
  }
}

case class SomeResponseType()
