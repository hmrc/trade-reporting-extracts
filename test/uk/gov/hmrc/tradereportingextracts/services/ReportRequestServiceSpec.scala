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

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers.{must, mustEqual}
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import uk.gov.hmrc.tradereportingextracts.models.ReportRequest
import uk.gov.hmrc.tradereportingextracts.repositories.ReportRequestRepository

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

class ReportRequestServiceSpec extends AnyWordSpec, GuiceOneAppPerSuite, ScalaFutures:

  given ExecutionContext = ExecutionContext.global

  lazy val mockReportRequestRepository: ReportRequestRepository = mock[ReportRequestRepository]

  private val reportRequestService = new ReportRequestService(mockReportRequestRepository)

  private val report = ReportRequest(
    reportId = "someReportId",
    correlationId = "someCorrelationId",
    reportName = "someReportName",
    requestorId = "GB0019",
    eoriRole = "someEORIRole",
    reportEORIs = Array("EORI1", "EORI2"),
    recipientEmails = Array("email1@example.com", "email2@example.com"),
    reportTypeName = "someReportType",
    reportStart = Instant.parse("2023-01-01T00:00:00Z"),
    reportEnd = Instant.parse("2023-12-31T23:59:59Z"),
    createDate = Instant.parse("2023-01-01T10:00:00Z"),
    status = "someStatus",
    statusDetails = "someStatusDetails",
    fileAvailableTime = Instant.parse("2023-01-02T10:00:00Z"),
    linkAvailableTime = Instant.parse("2023-01-03T10:00:00Z")
  )

  "ReportRequestService" should {

    "insertReportRequest" should {
      "must insert a report request successfully" in {
        when(mockReportRequestRepository.insertReportRequest(report)).thenReturn(Future.successful(true))

        val result = reportRequestService.create(report).futureValue

        result mustEqual true

        verify(mockReportRequestRepository, times(1)).insertReportRequest(any)(using any())
      }
    }

    "findByReportId" should {
      "must retrieve a report successfully using a reportId" in {
        when(mockReportRequestRepository.findByReportId(report.reportId)).thenReturn(Future.successful(Some(report)))

        val fetchedRecord = reportRequestService.get(report.reportId).futureValue

        fetchedRecord mustEqual Some(report)
      }

      "must return None if reportId not found" in {
        when(mockReportRequestRepository.findByReportId("nonExistentId")).thenReturn(Future.successful(None))

        val fetchedRecord = reportRequestService.get("nonExistentId").futureValue

        fetchedRecord mustEqual None
      }
    }

    "updateByReportId" should {
      "must update an existing report successfully" in {
        when(mockReportRequestRepository.updateByReportId(report)).thenReturn(Future.successful(true))

        val updatedRecord = reportRequestService.update(report).futureValue

        updatedRecord mustEqual true
      }
    }

    "deleteByReportId" should {
      "must delete an existing report successfully" in {
        when(mockReportRequestRepository.deleteByReportId(report.reportId)).thenReturn(Future.successful(true))

        val deletedRecord = reportRequestService.delete(report.reportId).futureValue

        deletedRecord mustEqual true
      }
    }
  }
