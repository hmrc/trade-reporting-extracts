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

package uk.gov.hmrc.tradereportingextracts.controllers.support

import play.api.mvc.*

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import play.api.test.Helpers
import play.api.test.Helpers.stubControllerComponents
import uk.gov.hmrc.tradereportingextracts.controllers.action.AuthAction

object FakeAuth {

  class FakeSuccessAuthAction(
    val parser: BodyParser[AnyContent]
  )(implicit val executionContext: ExecutionContext)
      extends AuthAction {
    protected def ec: ExecutionContext = executionContext
    override def invokeBlock[A](
      request: Request[A],
      block: Request[A] => Future[Result]
    ): Future[Result] =
      block(request)
  }

  class FakeForbiddenAuthAction(
    val parser: BodyParser[AnyContent]
  )(implicit val executionContext: ExecutionContext)
      extends AuthAction {

    protected def ec = ExecutionContext.global
    override def invokeBlock[A](
      request: Request[A],
      block: Request[A] => Future[Result]
    ): Future[Result] =
      Future.successful(Results.Forbidden("Forbidden by FakeAuth"))
  }

  object Helpers {
    def success(implicit ec: ExecutionContext): FakeSuccessAuthAction =
      new FakeSuccessAuthAction(stubParsers)(ec)

    def forbidden(implicit ec: ExecutionContext): FakeForbiddenAuthAction =
      new FakeForbiddenAuthAction(stubParsers)(ec)

    private def stubParsers: BodyParser[AnyContent] =
      stubControllerComponents().parsers.defaultBodyParser
  }
}
