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
import uk.gov.hmrc.tradereportingextracts.models.ReportRequest

import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global

class ReportRequestRepositorySpec
    extends AnyWordSpec,
      MockitoSugar,
      GuiceOneAppPerSuite,
      CleanMongoCollectionSupport,
      Matchers:

  private val report  = ReportRequest(
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
  private val report2 = ReportRequest(
    reportId = "uniqueReportId2",
    correlationId = "uniqueCorrelationId2",
    reportName = "anotherReportName",
    requestorId = "GB0020",
    eoriRole = "anotherEORIRole",
    reportEORIs = Array("EORI5", "EORI6"),
    recipientEmails = Array("email5@example.com", "email6@example.com"),
    reportTypeName = "anotherReportType",
    reportStart = Instant.parse("2023-03-01T00:00:00Z"),
    reportEnd = Instant.parse("2023-10-31T23:59:59Z"),
    createDate = Instant.parse("2023-03-01T10:00:00Z"),
    status = "anotherStatus",
    statusDetails = "anotherStatusDetails",
    fileAvailableTime = Instant.parse("2023-03-02T10:00:00Z"),
    linkAvailableTime = Instant.parse("2023-03-03T10:00:00Z")
  )

  val reportRequestRepository: ReportRequestRepository = new ReportRequestRepository(mongoComponent, mock[AppConfig])

  "insertReportRequest" should {

    "must insert a report successfully" in {
      val insertResult = reportRequestRepository.insertReportRequest(report).futureValue
      insertResult mustEqual true
    }
  }

  "findByReportId" should {

    "must be able to retrieve a report successfully using a reportId" in {
      val insertResult  = reportRequestRepository.insertReportRequest(report).futureValue
      val fetchedRecord = reportRequestRepository.findByReportId(report.reportId).futureValue

      insertResult mustEqual true
      fetchedRecord.get mustEqual report
    }

    "must return none if reportId not found" in {
      val insertResult  = reportRequestRepository.insertReportRequest(report).futureValue
      val fetchedRecord = reportRequestRepository.findByReportId("nonExistentReportId").futureValue

      insertResult mustEqual true
      fetchedRecord must be(None)
    }
  }
  "updateByReportId" should {

    "must be able to update an existing report" in {
      val insertResult              = reportRequestRepository.insertReportRequest(report).futureValue
      val fetchedBeforeUpdateRecord = reportRequestRepository.findByReportId(report.reportId).futureValue

      insertResult mustEqual true
      fetchedBeforeUpdateRecord.get mustEqual report

      val updatedRecord = reportRequestRepository.updateByReportId(report.copy(reportId = report.reportId)).futureValue
      val fetchedRecord = reportRequestRepository.findByReportId(report.reportId).futureValue

      updatedRecord mustEqual true
      fetchedRecord.get mustEqual report.copy(reportId = report.reportId)
    }
  }

  "deleteByReportId" should {

    "must be able to delete an existing report" in {
      val insertResult              = reportRequestRepository.insertReportRequest(report).futureValue
      val fetchedBeforeDeleteRecord = reportRequestRepository.findByReportId(report.reportId).futureValue
      val deletedRecord             = reportRequestRepository.deleteByReportId(report.reportId).futureValue

      insertResult mustEqual true
      fetchedBeforeDeleteRecord.get mustEqual report
      deletedRecord mustEqual true
    }
  }
