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
import uk.gov.hmrc.tradereportingextracts.models.*
import uk.gov.hmrc.tradereportingextracts.models.etmp.EoriUpdate
import uk.gov.hmrc.tradereportingextracts.models.thirdParty.{EoriBusinessInfo, ThirdPartyAddedConfirmation}
import uk.gov.hmrc.tradereportingextracts.repositories.{ReportRequestRepository, UserRepository}

import java.time.{Instant, LocalDate, ZoneOffset}
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UserService @Inject() (
  userRepository: UserRepository,
  reportRequestRepository: ReportRequestRepository,
  customsDataStoreConnector: CustomsDataStoreConnector,
  additionalEmailService: AdditionalEmailService
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

  def keepAlive(eori: String): Future[Boolean] =
    userRepository.keepAlive(eori)

  def deleteAuthorisedUser(eori: String, authorisedEori: String): Future[Boolean] =
    userRepository.deleteAuthorisedUser(eori, authorisedEori)

  def getOrCreateUser(eori: String): Future[UserDetails] =
    for {
      (user, isExist)    <- userRepository.getOrCreateUser(eori)
      companyInfoFuture   = customsDataStoreConnector.getCompanyInformation(eori)
      companyInformation <- companyInfoFuture
      _                  <- if (isExist) {
                              additionalEmailService
                                .updateLastAccessed(eori)
                                .flatMap(_ => cleanExpiredAccesses(user))
                            } else {
                              additionalEmailService.getAdditionalEmails(eori).map(_ => ())
                            }
    } yield UserDetails(
      eori = user.eori,
      additionalEmails = Seq.empty,
      authorisedUsers = user.authorisedUsers,
      companyInformation = companyInformation,
      notificationEmail = NotificationEmail()
    )

  def getUserDetailsAll(eori: String): Future[UserDetails] =
    for {
      (user, isExist)    <- userRepository.getOrCreateUser(eori)
      companyInformation <- customsDataStoreConnector.getCompanyInformation(eori)
      additionalEmails   <- additionalEmailService.getAdditionalEmails(eori)
      _                  <- if (isExist) {
                              additionalEmailService
                                .updateLastAccessed(eori)
                                .flatMap(_ => cleanExpiredAccesses(user))
                            } else {
                              Future.successful(())
                            }
    } yield UserDetails(
      eori = user.eori,
      additionalEmails = additionalEmails,
      authorisedUsers = user.authorisedUsers,
      companyInformation = companyInformation,
      notificationEmail = NotificationEmail()
    )

  def cleanExpiredAccesses(user: User): Future[Unit] = {
    val now = Instant.now()

    def deleteForAuthorisedUser(trader: User, authorisedUser: AuthorisedUser): Future[Unit] = {
      val deleteReportsFut = reportRequestRepository.deleteReportsForThirdPartyRemoval(trader.eori, authorisedUser.eori)
      val deleteUserFut    = userRepository.deleteAuthorisedUser(trader.eori, authorisedUser.eori)
      for {
        _ <- deleteReportsFut
        _ <- deleteUserFut
      } yield ()
    }

    val expired                          = user.authorisedUsers.filter(au => au.accessEnd.exists(_.isBefore(now)))
    val deleteFutures: Seq[Future[Unit]] = expired.map(au => deleteForAuthorisedUser(user, au))
    val deletesFut: Future[Unit]         = Future.sequence(deleteFutures).map(_ => ())

    val traderDeletesFut = userRepository.getUsersByAuthorisedEori(user.eori).flatMap { traders =>
      val deleteTraderFutures = for {
        trader <- traders
        au     <- trader.authorisedUsers
        if au.eori == user.eori && au.accessEnd.exists(_.isBefore(now))
      } yield deleteForAuthorisedUser(trader, au)
      Future.sequence(deleteTraderFutures).map(_ => ())
    }

    for {
      _ <- deletesFut
      _ <- traderDeletesFut
    } yield ()
  }

  def getUserAdditionalEmails(eori: String): Future[Seq[String]] =
    additionalEmailService.getAdditionalEmails(eori)

  def getAuthorisedEoris(eori: String): Future[Seq[String]] =
    userRepository.getAuthorisedEoris(eori)

  def getNotificationEmail(eori: String): Future[NotificationEmail] =
    customsDataStoreConnector.getNotificationEmail(eori)

  def getUserAndEmailDetails(eori: String): Future[UserDetails] =
    for {
      (user, isExist)    <- userRepository.getOrCreateUser(eori)
      companyInformation <- customsDataStoreConnector.getCompanyInformation(eori)
      notificationEmail  <- customsDataStoreConnector.getNotificationEmail(eori)
      additionalEmails   <- additionalEmailService.getAdditionalEmails(eori)
    } yield UserDetails(
      eori = user.eori,
      additionalEmails = additionalEmails,
      authorisedUsers = user.authorisedUsers,
      companyInformation = companyInformation.copy(
        address = AddressInformation()
      ),
      notificationEmail = notificationEmail
    )

  def addAuthorisedUser(eori: String, authorisedUser: AuthorisedUser): Future[ThirdPartyAddedConfirmation] =
    userRepository.addAuthorisedUser(eori, authorisedUser)

  def updateAuthorisedUser(eori: String, authorisedUser: AuthorisedUser): Future[ThirdPartyAddedConfirmation] =
    userRepository.updateAuthorisedUser(eori, authorisedUser)

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

  def getUsersByAuthorisedEoriWithStatus(thirdPartyEori: String): Future[Seq[EoriBusinessInfo]] =
    for {
      usersWithStatus <- userRepository.getUsersByAuthorisedEoriWithStatus(thirdPartyEori)
      eoriInfos       <- Future.traverse(usersWithStatus) { case UserWithStatus(user, status) =>
                           customsDataStoreConnector.getCompanyInformation(user.eori).map { companyInfo =>
                             val businessInfo =
                               if (companyInfo.consent == "1") Some(companyInfo.name)
                               else None

                             EoriBusinessInfo(
                               eori = user.eori,
                               businessInfo = businessInfo,
                               status = Some(status)
                             )
                           }
                         }
    } yield eoriInfos

  def getUsersByAuthorisedEoriWithDateFilter(thirdPartyEori: String): Future[Seq[EoriBusinessInfo]] =
    for {
      users     <- userRepository.getUsersByAuthorisedEoriWithDateFilter(thirdPartyEori)
      eoriInfos <- Future.sequence(
                     users.map { user =>
                       customsDataStoreConnector.getCompanyInformation(user.eori).map { companyInfo =>
                         val businessInfo =
                           if (companyInfo.consent == "1") Some(companyInfo.name)
                           else None

                         EoriBusinessInfo(
                           eori = user.eori,
                           businessInfo = businessInfo,
                           status = None
                         )
                       }
                     }
                   )
    } yield eoriInfos

  def addAdditionalEmail(eori: String, emailAddress: String): Future[Boolean] =
    additionalEmailService.addAdditionalEmail(eori, emailAddress)

  def removeAdditionalEmail(eori: String, emailAddress: String): Future[Boolean] =
    additionalEmailService.removeAdditionalEmail(eori, emailAddress)

  def updateEmailLastUsed(eori: String, emailAddress: String): Future[Boolean] =
    additionalEmailService.updateEmailAccessDate(eori, emailAddress)
