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

package uk.gov.hmrc.tradereportingextracts.services

import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tradereportingextracts.connectors.CustomsDataStoreConnector
import uk.gov.hmrc.tradereportingextracts.models.{AllowedEoris, User}
import uk.gov.hmrc.tradereportingextracts.repositories.UserRepository

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UserInformationService @Inject() (
  userRepository: UserRepository,
  customsDataStoreConnector: CustomsDataStoreConnector
) extends AllowedEoris:
  def insert(user: User)(using ec: ExecutionContext): Future[Boolean] =
    userRepository.insert(user)

  def getUserByEori(eori: String)(using ec: ExecutionContext, hc: HeaderCarrier): Future[Either[String, User]] =
    if !allowedEoris.contains(eori) then Future.successful(Left("EORI not allowed"))
    else
      userRepository
        .getOrCreateUser(eori)
        .flatMap { case Some(user) =>
          customsDataStoreConnector
            .getVerifiedEmail(user.eori)
            .flatMap {
              case Left(error)  =>
                Future.successful(Right(user))
              case Right(email) =>
                user.notificationEmail.address = email.address
                Future.successful(Right(user))
            }
        }

  def update(user: User): Future[Boolean] =
    userRepository.update(user)

  def updateEori(oldEori: String, newEori: String): Future[Boolean] =
    userRepository.updateEori(oldEori, newEori)

  def deleteByEori(eori: String): Future[Boolean] =
    userRepository.deleteByEori(eori)

  def getAuthorisedEoris(eori: String): Future[Seq[String]] =
    userRepository.getAuthorisedEoris(eori)
