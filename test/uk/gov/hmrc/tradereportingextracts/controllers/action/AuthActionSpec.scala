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

package uk.gov.hmrc.tradereportingextracts.controllers.action

import com.google.inject.Inject
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import play.api.http.Status.OK
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.*
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.{Retrieval, ~}
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.tradereportingextracts.utils.WireMockHelper

import scala.concurrent.{ExecutionContext, Future}

class AuthActionSpec extends AnyFreeSpec with Matchers with BeforeAndAfterEach with WireMockHelper with ScalaFutures {

  private val app                             = GuiceApplicationBuilder().build()
  private val bodyParser: BodyParsers.Default = app.injector.instanceOf(classOf[BodyParsers.Default])
  private val cc: ControllerComponents        = app.injector.instanceOf(classOf[ControllerComponents])
  implicit val ec: ExecutionContext           = ExecutionContext.Implicits.global

  private val eoriNumber           = "GB123456789001"
  private val enrolmentKey         = "HMRC-CUS-ORG"
  private val enrolment: Enrolment = Enrolment(enrolmentKey).withIdentifier("EORINumber", eoriNumber)

  "end-user token auth check" - {
    "should succeed when enrolment and affinity group present" in {

      val retrieval: Option[AffinityGroup] ~ Enrolments =
        new ~(Some(Organisation), Enrolments(Set(enrolment)))

      val authAction = new AuthActionImpl(
        new FakeSuccessfulAuthConnector(retrieval),
        bodyParser = bodyParser,
        cc = cc
      )

      val resultFuture: Future[Result] = authAction.invokeBlock(FakeRequest(), block)
      val result                       = resultFuture.futureValue

      result.header.status shouldBe OK
    }

    "should fail when unsuppported affinity group missing" in {
      val authAction = new AuthActionImpl(
        new FakeFailingAuthConnector(new UnsupportedAffinityGroup),
        bodyParser = bodyParser,
        cc = cc
      )

      val resultFuture: Future[Result] = authAction.invokeBlock(FakeRequest(), block)
      val result                       = resultFuture.failed.futureValue

      result shouldBe UpstreamErrorResponse("Authorisation failure: Unsupported Affinity Group.", 401)

    }
    "should fail when authorisation exception" in {
      val authAction = new AuthActionImpl(
        new FakeFailingAuthConnector(new InsufficientEnrolments),
        bodyParser = bodyParser,
        cc = cc
      )

      val resultFuture: Future[Result] = authAction.invokeBlock(FakeRequest(), block)
      val result                       = resultFuture.failed.futureValue

      result shouldBe UpstreamErrorResponse("Authorisation failure: Insufficient Enrolments.", 403)
    }
  }

  private val block: Request[_] => Future[Result] =
    _ => Future.successful(Results.Ok)
}

class FakeFailingAuthConnector @Inject() (exceptionToReturn: Throwable) extends AuthConnector {
  val serviceUrl: String = ""
  override def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[A] =
    Future.failed(exceptionToReturn)
}

class FakeSuccessfulAuthConnector @Inject() (retrievalToReturn: Any) extends AuthConnector {
  val serviceUrl: String = ""
  override def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[A] =
    Future.successful(retrievalToReturn.asInstanceOf[A])
}
