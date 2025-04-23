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

package uk.gov.hmrc.tradereportingextracts.repositories

import org.scalatest.matchers.must.Matchers.{must, mustEqual}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import uk.gov.hmrc.mongo.test.CleanMongoCollectionSupport
import uk.gov.hmrc.tradereportingextracts.config.AppConfig
import uk.gov.hmrc.tradereportingextracts.models.{Component, EoriRole, Notification, ReportRequest, ReportTypeName, StatusCode, StatusType}

import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global

class ReportRequestRepositorySpec
    extends AnyWordSpec,
      MockitoSugar,
      GuiceOneAppPerSuite,
      CleanMongoCollectionSupport,
      Matchers:

  private val reportRequest = ReportRequest(
    reportRequestId = "REQ00001",
    correlationId = "ABCD-DEFG",
    reportName = "Jan Report",
    requesterEORI = "GB0019",
    eoriRole = EoriRole.TRADER,
    reportEORIs = Array("EORI1", "EORI2"),
    recipientEmails = Array("email1@example.com", "email2@example.com"),
    reportTypeName = ReportTypeName.IMPORTS_ITEM_REPORT,
    reportStart = Instant.parse("2023-01-01T00:00:00Z"),
    reportEnd = Instant.parse("2023-12-31T23:59:59Z"),
    createDate = Instant.parse("2023-01-01T10:00:00Z"),
    notifications = Seq(
      Notification(
        component = Component.CDAP,
        statusType = StatusType.INFORMATION,
        statusCode = StatusCode.FILESENT,
        statusMessage = "Message1"
      )
    ),
    fileAvailableTime = Instant.parse("2023-01-02T10:00:00Z"),
    linkAvailableTime = Instant.parse("2023-01-03T10:00:00Z")
  )

  val reportRequestRepository: ReportRequestRepository = new ReportRequestRepository(mongoComponent)

  "insertReportRequest" should {
    "must insert a report successfully" in {
      val insertResult = reportRequestRepository.insert(reportRequest).futureValue
      insertResult mustEqual true
    }
  }

  "findByReportId"   should {
    "must be able to retrieve a report successfully using a reportId" in {
      val insertResult  = reportRequestRepository.insert(reportRequest).futureValue
      val fetchedRecord = reportRequestRepository.findByReportRequestId(reportRequest.reportRequestId).futureValue
      insertResult mustEqual true
      fetchedRecord.get mustEqual reportRequest
    }

    "must return none if reportId not found" in {
      val insertResult  = reportRequestRepository.insert(reportRequest).futureValue
      val fetchedRecord = reportRequestRepository.findByReportRequestId("nonExistentReportId").futureValue
      insertResult mustEqual true
      fetchedRecord must be(None)
    }
  }
  "updateByReportId" should {

    "must be able to update an existing report" in {
      val insertResult              = reportRequestRepository.insert(reportRequest).futureValue
      val fetchedBeforeUpdateRecord =
        reportRequestRepository.findByReportRequestId(reportRequest.reportRequestId).futureValue
      insertResult mustEqual true
      fetchedBeforeUpdateRecord.get mustEqual reportRequest

      val updatedRecord =
        reportRequestRepository.update(reportRequest.copy(reportRequestId = reportRequest.reportRequestId)).futureValue
      val fetchedRecord = reportRequestRepository.findByReportRequestId(reportRequest.reportRequestId).futureValue
      updatedRecord mustEqual true
      fetchedRecord.get mustEqual reportRequest.copy(reportRequestId = reportRequest.reportRequestId)
    }
  }

  "deleteByReportId" should {

    "must be able to delete an existing report" in {
      val insertResult              = reportRequestRepository.insert(reportRequest).futureValue
      val fetchedBeforeDeleteRecord =
        reportRequestRepository.findByReportRequestId(reportRequest.reportRequestId).futureValue
      val deletedRecord             = reportRequestRepository.delete(reportRequest).futureValue
      insertResult mustEqual true
      fetchedBeforeDeleteRecord.get mustEqual reportRequest
      deletedRecord mustEqual true
    }
  }
