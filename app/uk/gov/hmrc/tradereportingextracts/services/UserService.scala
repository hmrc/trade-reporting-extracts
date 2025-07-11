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

import uk.gov.hmrc.tradereportingextracts.connectors.CustomsDataStoreConnector
import uk.gov.hmrc.tradereportingextracts.models.etmp.EoriUpdate
import uk.gov.hmrc.tradereportingextracts.models.{NotificationEmail, User, UserDetails}
import uk.gov.hmrc.tradereportingextracts.repositories.UserRepository

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UserService @Inject() (
  userRepository: UserRepository,
  customsDataStoreConnector: CustomsDataStoreConnector
)(using ec: ExecutionContext):

  def insert(user: User): Future[Boolean] =
    userRepository.insert(user)

  def update(user: User): Future[Boolean] =
    userRepository.update(user)

  def updateEori(eoriUpdate: EoriUpdate): Future[Boolean] =
    userRepository.updateEori(eoriUpdate)

  def deleteByEori(eori: String): Future[Boolean] =
    userRepository.deleteByEori(eori)

  def getOrCreateUser(eori: String): Future[UserDetails] =
    for {
      user               <- userRepository.getOrCreateUser(eori)
      companyInformation <- customsDataStoreConnector.getCompanyInformation(eori)
    } yield UserDetails(
      eori = user.eori,
      additionalEmails = user.additionalEmails,
      authorisedUsers = user.authorisedUsers,
      companyInformation = companyInformation,
      notificationEmail = NotificationEmail()
    )

  def getAuthorisedEoris(eori: String): Future[Seq[String]] =
    userRepository.getAuthorisedEoris(eori)

  def getNotificationEmail(eori: String): Future[NotificationEmail] =
    customsDataStoreConnector.getNotificationEmail(eori)
