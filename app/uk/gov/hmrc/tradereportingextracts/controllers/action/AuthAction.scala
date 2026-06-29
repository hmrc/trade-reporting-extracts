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

import com.google.inject.ImplementedBy
import play.api.Logging
import play.api.mvc.*
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.tradereportingextracts.controllers.action.AuthAction.{cdsEnrolmentIdentifier, cdsEnrolmentKey}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AuthActionImpl @Inject() (
  override val authConnector: AuthConnector,
  val bodyParser: BodyParsers.Default,
  cc: ControllerComponents
)(implicit val ec: ExecutionContext)
    extends BackendController(cc)
    with AuthorisedFunctions
    with AuthAction
    with Logging {

  override val parser: BodyParsers.Default                  = bodyParser
  override protected def executionContext: ExecutionContext = ec

  override def invokeBlock[A](
    request: Request[A],
    block: Request[A] => Future[Result]
  ): Future[Result] = {

    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequest(request)

    authorised(Enrolment(AuthAction.cdsEnrolmentKey))
      .retrieve(Retrievals.affinityGroup and Retrievals.authorisedEnrolments) {
        case Some(affinityGroup) ~ authorisedEnrolments =>
          handleEnrolments(authorisedEnrolments, request, block)
        case _                                          =>
          throw InternalError("Undefined authorisation error")
      } recover handleAuthorisationFailures
  }
  private def handleEnrolments[A](
    authorisedEnrolments: Enrolments,
    request: Request[A],
    block: Request[A] => Future[Result]
  )(implicit hc: HeaderCarrier): Future[Result] = {
    val maybeEnrolment = authorisedEnrolments
      .getEnrolment(cdsEnrolmentKey)
      .flatMap(_.getIdentifier(cdsEnrolmentIdentifier))

    maybeEnrolment match {
      case Some(enrolment) if enrolment.value.nonEmpty =>
        block(request)
      case Some(_)                                     =>
        throw InternalError("EORI is empty")
      case None                                        =>
        throw InsufficientEnrolments("Unable to retrieve Enrolment")
    }
  }

  private def handleAuthorisationFailures: PartialFunction[Throwable, Result] = {
    case _: UnsupportedAffinityGroup       =>
      throw UpstreamErrorResponse("Authorisation failure: Unsupported Affinity Group.", UNAUTHORIZED)
    case _: InsufficientEnrolments         =>
      throw UpstreamErrorResponse("Authorisation failure: Insufficient Enrolments.", FORBIDDEN)
    case exception: AuthorisationException =>
      throw UpstreamErrorResponse("Authorisation failure: AuthorisationException.", FORBIDDEN)
  }
}

object AuthAction {
  val cdsEnrolmentKey        = "HMRC-CUS-ORG"
  val cdsEnrolmentIdentifier = "EORINumber"
}

@ImplementedBy(classOf[AuthActionImpl])
trait AuthAction extends ActionBuilder[Request, AnyContent] with ActionFunction[Request, Request]
