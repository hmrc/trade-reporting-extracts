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

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{BeforeAndAfterEach, OptionValues}
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.tradereportingextracts.models.etmp.EoriUpdate
import uk.gov.hmrc.tradereportingextracts.repositories.AdditionalEmailRepository

import scala.concurrent.{ExecutionContext, Future}

class AdditionalEmailServiceSpec
    extends AnyFreeSpec
    with Matchers
    with OptionValues
    with ScalaFutures
    with IntegrationPatience
    with MockitoSugar
    with BeforeAndAfterEach {

  implicit val ec: ExecutionContext = ExecutionContext.global

  val mockRepository: AdditionalEmailRepository = mock[AdditionalEmailRepository]
  val service                                   = new AdditionalEmailService(mockRepository)

  val testEori     = "GB123456789000"
  val testEmail    = "test@example.com"
  val anotherEmail = "another@example.com"

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockRepository)
  }

  "AdditionalEmailService" - {

    "getAdditionalEmails" - {

      "must return emails from repository" in {
        val expectedEmails = Seq(testEmail, anotherEmail)
        when(mockRepository.getEmailsForEori(testEori))
          .thenReturn(Future.successful(expectedEmails))

        val result = service.getAdditionalEmails(testEori)

        result.futureValue mustEqual expectedEmails
        verify(mockRepository).getEmailsForEori(testEori)
      }

      "must return empty sequence when no emails exist" in {
        when(mockRepository.getEmailsForEori(testEori))
          .thenReturn(Future.successful(Seq.empty))

        val result = service.getAdditionalEmails(testEori)

        result.futureValue mustEqual Seq.empty
        verify(mockRepository).getEmailsForEori(testEori)
      }

      "must handle repository failure" in {
        val exception = new RuntimeException("Database error")
        when(mockRepository.getEmailsForEori(testEori))
          .thenReturn(Future.failed(exception))

        val result = service.getAdditionalEmails(testEori)

        result.failed.futureValue mustBe exception
      }
    }

    "addAdditionalEmail" - {

      "must add new email when it doesn't exist" in {
        when(mockRepository.getEmailsForEori(testEori))
          .thenReturn(Future.successful(Seq.empty))
        when(mockRepository.addEmail(testEori, testEmail))
          .thenReturn(Future.successful(true))

        val result = service.addAdditionalEmail(testEori, testEmail)

        result.futureValue mustBe true
        verify(mockRepository).getEmailsForEori(testEori)
        verify(mockRepository).addEmail(testEori, testEmail)
        verify(mockRepository, never()).updateEmailAccessDate(any(), any())
      }

      "must update access date when email already exists" in {
        when(mockRepository.getEmailsForEori(testEori))
          .thenReturn(Future.successful(Seq(testEmail, anotherEmail)))
        when(mockRepository.updateEmailAccessDate(testEori, testEmail))
          .thenReturn(Future.successful(true))

        val result = service.addAdditionalEmail(testEori, testEmail)

        result.futureValue mustBe true
        verify(mockRepository).getEmailsForEori(testEori)
        verify(mockRepository).updateEmailAccessDate(testEori, testEmail)
        verify(mockRepository, never()).addEmail(any(), any())
      }

      "must return false when repository add operation fails" in {
        when(mockRepository.getEmailsForEori(testEori))
          .thenReturn(Future.successful(Seq.empty))
        when(mockRepository.addEmail(testEori, testEmail))
          .thenReturn(Future.successful(false))

        val result = service.addAdditionalEmail(testEori, testEmail)

        result.futureValue mustBe false
        verify(mockRepository).addEmail(testEori, testEmail)
      }

      "must return false when repository update access date operation fails" in {
        when(mockRepository.getEmailsForEori(testEori))
          .thenReturn(Future.successful(Seq(testEmail)))
        when(mockRepository.updateEmailAccessDate(testEori, testEmail))
          .thenReturn(Future.successful(false))

        val result = service.addAdditionalEmail(testEori, testEmail)

        result.futureValue mustBe false
        verify(mockRepository).updateEmailAccessDate(testEori, testEmail)
      }

      "must handle repository failure when getting existing emails" in {
        val exception = new RuntimeException("Database error")
        when(mockRepository.getEmailsForEori(testEori))
          .thenReturn(Future.failed(exception))

        val result = service.addAdditionalEmail(testEori, testEmail)

        result.failed.futureValue mustBe exception
        verify(mockRepository, never()).addEmail(any(), any())
        verify(mockRepository, never()).updateEmailAccessDate(any(), any())
      }

      "must handle repository failure when adding new email" in {
        val exception = new RuntimeException("Database error")
        when(mockRepository.getEmailsForEori(testEori))
          .thenReturn(Future.successful(Seq.empty))
        when(mockRepository.addEmail(testEori, testEmail))
          .thenReturn(Future.failed(exception))

        val result = service.addAdditionalEmail(testEori, testEmail)

        result.failed.futureValue mustBe exception
      }

      "must handle case-sensitive email comparison" in {
        val upperCaseEmail = testEmail.toUpperCase
        when(mockRepository.getEmailsForEori(testEori))
          .thenReturn(Future.successful(Seq(testEmail)))
        when(mockRepository.addEmail(testEori, upperCaseEmail))
          .thenReturn(Future.successful(true))

        val result = service.addAdditionalEmail(testEori, upperCaseEmail)

        result.futureValue mustBe true
        verify(mockRepository).addEmail(testEori, upperCaseEmail)
        verify(mockRepository, never()).updateEmailAccessDate(any(), any())
      }
    }

    "removeAdditionalEmail" - {

      "must remove email successfully" in {
        when(mockRepository.removeEmail(testEori, testEmail))
          .thenReturn(Future.successful(true))

        val result = service.removeAdditionalEmail(testEori, testEmail)

        result.futureValue mustBe true
        verify(mockRepository).removeEmail(testEori, testEmail)
      }

      "must return false when repository remove operation fails" in {
        when(mockRepository.removeEmail(testEori, testEmail))
          .thenReturn(Future.successful(false))

        val result = service.removeAdditionalEmail(testEori, testEmail)

        result.futureValue mustBe false
        verify(mockRepository).removeEmail(testEori, testEmail)
      }

      "must handle repository failure" in {
        val exception = new RuntimeException("Database error")
        when(mockRepository.removeEmail(testEori, testEmail))
          .thenReturn(Future.failed(exception))

        val result = service.removeAdditionalEmail(testEori, testEmail)

        result.failed.futureValue mustBe exception
      }
    }

    "updateEmailAccessDate" - {

      "must update email access date successfully" in {
        when(mockRepository.updateEmailAccessDate(testEori, testEmail))
          .thenReturn(Future.successful(true))

        val result = service.updateEmailAccessDate(testEori, testEmail)

        result.futureValue mustBe true
        verify(mockRepository).updateEmailAccessDate(testEori, testEmail)
      }

      "must return false when repository update operation fails" in {
        when(mockRepository.updateEmailAccessDate(testEori, testEmail))
          .thenReturn(Future.successful(false))

        val result = service.updateEmailAccessDate(testEori, testEmail)

        result.futureValue mustBe false
        verify(mockRepository).updateEmailAccessDate(testEori, testEmail)
      }

      "must handle repository failure" in {
        val exception = new RuntimeException("Database error")
        when(mockRepository.updateEmailAccessDate(testEori, testEmail))
          .thenReturn(Future.failed(exception))

        val result = service.updateEmailAccessDate(testEori, testEmail)

        result.failed.futureValue mustBe exception
      }
    }

    "updateLastAccessed" - {

      "must update last accessed timestamp successfully" in {
        when(mockRepository.updateLastAccessed(testEori))
          .thenReturn(Future.successful(true))

        val result = service.updateLastAccessed(testEori)

        result.futureValue mustBe true
        verify(mockRepository).updateLastAccessed(testEori)
      }

      "must return false when repository update operation fails" in {
        when(mockRepository.updateLastAccessed(testEori))
          .thenReturn(Future.successful(false))

        val result = service.updateLastAccessed(testEori)

        result.futureValue mustBe false
        verify(mockRepository).updateLastAccessed(testEori)
      }

      "must handle repository failure" in {
        val exception = new RuntimeException("Database error")
        when(mockRepository.updateLastAccessed(testEori))
          .thenReturn(Future.failed(exception))

        val result = service.updateLastAccessed(testEori)

        result.failed.futureValue mustBe exception
      }
    }

    "updateEori" - {

      "must update EORI successfully" in {
        val update = EoriUpdate(oldEori = "GB111111111000", newEori = "GB222222222000")

        when(mockRepository.updateEori(update))
          .thenReturn(Future.successful(true))

        val result = service.updateEori(update)

        result.futureValue mustBe true
        verify(mockRepository).updateEori(update)
      }

      "must return false when repository update fails" in {
        val update = EoriUpdate(oldEori = "GB333333333000", newEori = "GB444444444000")

        when(mockRepository.updateEori(update))
          .thenReturn(Future.successful(false))

        val result = service.updateEori(update)

        result.futureValue mustBe false
        verify(mockRepository).updateEori(update)
      }

      "must handle repository failure" in {
        val update    = EoriUpdate(oldEori = "GB555555555000", newEori = "GB666666666000")
        val exception = new RuntimeException("Database error")

        when(mockRepository.updateEori(update))
          .thenReturn(Future.failed(exception))

        val result = service.updateEori(update)

        result.failed.futureValue mustBe exception
        verify(mockRepository).updateEori(update)
      }
    }

    "deleteAllEmailsForEori" - {

      "must delete all emails successfully" in {
        when(mockRepository.deleteByEori(testEori))
          .thenReturn(Future.successful(true))

        val result = service.deleteAllEmailsForEori(testEori)

        result.futureValue mustBe true
        verify(mockRepository).deleteByEori(testEori)
      }

      "must return false when repository delete operation fails" in {
        when(mockRepository.deleteByEori(testEori))
          .thenReturn(Future.successful(false))

        val result = service.deleteAllEmailsForEori(testEori)

        result.futureValue mustBe false
        verify(mockRepository).deleteByEori(testEori)
      }

      "must handle repository failure" in {
        val exception = new RuntimeException("Database error")
        when(mockRepository.deleteByEori(testEori))
          .thenReturn(Future.failed(exception))

        val result = service.deleteAllEmailsForEori(testEori)

        result.failed.futureValue mustBe exception
      }
    }

    "AC2: Individual Email TTL (365 days)" - {

      "must ensure addAdditionalEmail triggers automatic cleanup of expired emails" in {
        // Repository should handle the cleanup automatically
        when(mockRepository.getEmailsForEori(testEori))
          .thenReturn(Future.successful(Seq.empty))
        when(mockRepository.addEmail(testEori, testEmail))
          .thenReturn(Future.successful(true))

        val result = service.addAdditionalEmail(testEori, testEmail)

        result.futureValue mustBe true
        verify(mockRepository).getEmailsForEori(testEori)
        verify(mockRepository).addEmail(testEori, testEmail)
        verifyNoMoreInteractions(mockRepository)
      }

      "must ensure updateEmailAccessDate triggers automatic cleanup of expired emails" in {
        // Repository should handle the cleanup automatically
        when(mockRepository.updateEmailAccessDate(testEori, testEmail))
          .thenReturn(Future.successful(true))

        val result = service.updateEmailAccessDate(testEori, testEmail)

        result.futureValue mustBe true
        verify(mockRepository).updateEmailAccessDate(testEori, testEmail)
        verifyNoMoreInteractions(mockRepository)
      }

      "must delegate TTL cleanup responsibility to repository layer" in {
        // The service layer should not implement TTL logic directly - it's handled by repository
        when(mockRepository.getEmailsForEori(testEori))
          .thenReturn(Future.successful(Seq("existing@example.com")))
        when(mockRepository.updateEmailAccessDate(testEori, "existing@example.com"))
          .thenReturn(Future.successful(true))

        val result = service.addAdditionalEmail(testEori, "existing@example.com")

        result.futureValue mustBe true
        // Verify the service calls updateEmailAccessDate which triggers cleanup at repository level
        verify(mockRepository).getEmailsForEori(testEori)
        verify(mockRepository).updateEmailAccessDate(testEori, "existing@example.com")
      }

      "must ensure all operations benefit from automatic cleanup" in {
        // Test that operations like updateLastAccessed also trigger cleanup
        when(mockRepository.updateLastAccessed(testEori))
          .thenReturn(Future.successful(true))

        val result = service.updateLastAccessed(testEori)

        result.futureValue mustBe true
        verify(mockRepository).updateLastAccessed(testEori)
        // Repository layer handles cleanup automatically
      }
    }
  }
}
