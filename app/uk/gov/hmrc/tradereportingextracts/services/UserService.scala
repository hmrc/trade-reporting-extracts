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
import uk.gov.hmrc.tradereportingextracts.models.thirdParty.ThirdPartyAddedConfirmation
import uk.gov.hmrc.tradereportingextracts.models.*
import uk.gov.hmrc.tradereportingextracts.repositories.UserRepository

import java.time.{Clock, LocalDate, ZoneOffset}
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
    for {
      userEoriUpdate            <- userRepository.updateEori(eoriUpdate)
      authorisedUsersEoriUpdate <- userRepository.updateAuthorisedUserEori(eoriUpdate)
    } yield userEoriUpdate && authorisedUsersEoriUpdate

  def deleteByEori(eori: String): Future[Boolean] =
    userRepository.deleteByEori(eori)

  def deleteAuthorisedUser(eori: String, authorisedEori: String): Future[Boolean] =
    userRepository.deleteAuthorisedUser(eori, authorisedEori)

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

  def getUserAndEmailDetails(eori: String): Future[UserDetails] =
    for {
      user               <- userRepository.getOrCreateUser(eori)
      companyInformation <- customsDataStoreConnector.getCompanyInformation(eori)
      notificationEmail  <- customsDataStoreConnector.getNotificationEmail(eori)
    } yield UserDetails(
      eori = user.eori,
      additionalEmails = user.additionalEmails,
      authorisedUsers = user.authorisedUsers,
      companyInformation = companyInformation.copy(
        address = AddressInformation()
      ),
      notificationEmail = notificationEmail
    )

  def addAuthorisedUser(eori: String, authorisedUser: AuthorisedUser): Future[ThirdPartyAddedConfirmation] =
    userRepository.addAuthorisedUser(eori, authorisedUser)

  def getAuthorisedUser(eori: String, thirdPartyEori: String): Future[Option[AuthorisedUser]] =
    userRepository.getAuthorisedUser(eori, thirdPartyEori)

  def getAuthorisedBusiness(thirdPartyEori: String, traderEori: String): Future[Option[AuthorisedUser]] =
    userRepository.getAuthorisedUser(traderEori, thirdPartyEori)

  def transformToThirdPartyDetails(authorisedUser: AuthorisedUser): ThirdPartyDetails =
    ThirdPartyDetails(
      referenceName = authorisedUser.referenceName,
      accessStartDate = LocalDate.ofInstant(authorisedUser.accessStart, ZoneOffset.UTC),
      accessEndDate = authorisedUser.accessEnd match {
        case Some(value) => Some(LocalDate.ofInstant(value, ZoneOffset.UTC))
        case _           => None
      },
      dataTypes = authorisedUser.accessType.map(_.toString.toLowerCase),
      dataStartDate = authorisedUser.reportDataStart match {
        case Some(value) => Some(LocalDate.ofInstant(value, ZoneOffset.UTC))
        case _           => None
      },
      dataEndDate = authorisedUser.reportDataEnd match {
        case Some(value) => Some(LocalDate.ofInstant(value, ZoneOffset.UTC))
        case _           => None
      }
    )

  def getUsersByAuthorisedEori(thirdPartyEori: String): Future[Seq[UserDetails]] =
    for {
      users       <- userRepository.getUsersByAuthorisedEori(thirdPartyEori)
      userDetails <- Future.traverse(users) { user =>
                       customsDataStoreConnector.getCompanyInformation(user.eori).map { companyInformation =>
                         UserDetails(
                           eori = user.eori,
                           additionalEmails = user.additionalEmails,
                           authorisedUsers = user.authorisedUsers,
                           companyInformation = companyInformation,
                           notificationEmail = NotificationEmail()
                         )
                       }
                     }
    } yield userDetails

  def getUsersByAuthorisedEoriWithDateFilter(thirdPartyEori: String): Future[Seq[UserDetails]] =
    for {
      users       <- userRepository.getUsersByAuthorisedEoriWithDateFilter(thirdPartyEori)
      userDetails <- Future.traverse(users) { user =>
                       customsDataStoreConnector.getCompanyInformation(user.eori).map { companyInformation =>
                         UserDetails(
                           eori = user.eori,
                           additionalEmails = user.additionalEmails,
                           authorisedUsers = user.authorisedUsers,
                           companyInformation = companyInformation,
                           notificationEmail = NotificationEmail()
                         )
                       }
                     }
    } yield userDetails
