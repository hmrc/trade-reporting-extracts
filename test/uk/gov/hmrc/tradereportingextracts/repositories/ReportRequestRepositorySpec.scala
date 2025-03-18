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
import uk.gov.hmrc.tradereportingextracts.models.Report

import scala.concurrent.ExecutionContext.Implicits.global

class ReportRequestRepositorySpec extends AnyWordSpec,
MockitoSugar,
GuiceOneAppPerSuite,
CleanMongoCollectionSupport,
Matchers:

  private val report = Report(1L, "someReportId", "someTemplateId", Array("email1@example.com", "email2@example.com"), Array("EORI1", "EORI2"), "someReportType", "2023-01-01", "2023-12-31", "someStatus", "someStatusDetails")
  private val report2 = Report(1L, "someReportId2", "someTemplateId2", Array("email3@example.com", "email4@example.com"), Array("EORI3", "EORI4"), "someReportType2", "2023-02-01", "2023-11-30", "someStatus2", "someStatusDetails2")

  val reportRequestRepository: ReportRequestRepository = new ReportRequestRepository(mongoComponent, mock[AppConfig])

  "insertReportRequest" should {

    "must insert a report successfully" in {
      val insertResult = reportRequestRepository.insertReportRequest(report).futureValue
      insertResult mustEqual true
    }
  }

  "findByReportId" should {

    "must be able to retrieve a report successfully using a reportId" in {
      val insertResult = reportRequestRepository.insertReportRequest(report).futureValue
      val fetchedRecord = reportRequestRepository.findByReportId(report.reportId).futureValue

      insertResult mustEqual true
      fetchedRecord.get mustEqual report
    }

    "must return none if reportId not found" in {
      val insertResult = reportRequestRepository.insertReportRequest(report).futureValue
      val fetchedRecord = reportRequestRepository.findByReportId("nonExistentReportId").futureValue

      insertResult mustEqual true
      fetchedRecord must be(None)
    }
  }
  "updateByReportId" should {

    "must be able to update an existing report" in {
      val insertResult = reportRequestRepository.insertReportRequest(report).futureValue
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
      val insertResult = reportRequestRepository.insertReportRequest(report).futureValue
      val fetchedBeforeDeleteRecord = reportRequestRepository.findByReportId(report.reportId).futureValue
      val deletedRecord = reportRequestRepository.deleteByReportId(report.reportId).futureValue

      insertResult mustEqual true
      fetchedBeforeDeleteRecord.get mustEqual report
      deletedRecord mustEqual true
    }
  }
