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

import play.api.Logging
import uk.gov.hmrc.tradereportingextracts.models.User
import uk.gov.hmrc.tradereportingextracts.repositories.UserRepository

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UserService @Inject() (
  userRepository: UserRepository
)(using ec: ExecutionContext)
    extends Logging:

  def insert(user: User)(using ec: ExecutionContext): Future[Boolean] =
    userRepository.insert(user)

  def findByEori(eori: String)(using ec: ExecutionContext): Future[Option[User]] =
    userRepository.findByEori(eori)

  def update(user: User): Future[Boolean] =
    userRepository.update(user)

  def updateEori(oldEori: String, newEori: String): Future[Boolean] =
    userRepository.updateEori(oldEori, newEori)

  def deleteByEori(eori: String): Future[Boolean] =
    userRepository.deleteByEori(eori)
