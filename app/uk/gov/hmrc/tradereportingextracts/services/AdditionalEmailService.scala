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

package uk.gov.hmrc.tradereportingextracts.services

import uk.gov.hmrc.tradereportingextracts.models.etmp.EoriUpdate
import uk.gov.hmrc.tradereportingextracts.repositories.AdditionalEmailRepository

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AdditionalEmailService @Inject() (
  additionalEmailRepository: AdditionalEmailRepository
)(using ec: ExecutionContext):

  def getAdditionalEmails(eori: String): Future[Seq[String]] =
    additionalEmailRepository.getEmailsForEori(eori)

  def addAdditionalEmail(eori: String, email: String): Future[Boolean] =
    getAdditionalEmails(eori).flatMap { existingEmails =>
      if (existingEmails.contains(email)) {
        additionalEmailRepository.updateEmailAccessDate(eori, email)
      } else {
        additionalEmailRepository.addEmail(eori, email)
      }
    }

  def removeAdditionalEmail(eori: String, email: String): Future[Boolean] =
    additionalEmailRepository.removeEmail(eori, email)

  def updateEmailAccessDate(eori: String, email: String): Future[Boolean] =
    additionalEmailRepository.updateEmailAccessDate(eori, email)

  def updateLastAccessed(eori: String): Future[Boolean] =
    additionalEmailRepository.updateLastAccessed(eori)

  def deleteAllEmailsForEori(eori: String): Future[Boolean] =
    additionalEmailRepository.deleteByEori(eori)

  def updateEori(eoriUpdate: EoriUpdate): Future[Boolean] =
    for {
      userEoriUpdate <- additionalEmailRepository.updateEori(eoriUpdate)
    } yield userEoriUpdate
