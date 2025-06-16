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

import org.scalatest.matchers.must.Matchers.{must, mustBe, mustEqual}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import uk.gov.hmrc.mongo.test.CleanMongoCollectionSupport
import uk.gov.hmrc.tradereportingextracts.config.AppConfig
import uk.gov.hmrc.tradereportingextracts.models.eis.EisReportStatusRequest
import uk.gov.hmrc.tradereportingextracts.models.{Component, EoriRole, FileNotification, FileType, ReportRequest, ReportTypeName, StatusCode, StatusType}

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
      EisReportStatusRequest(
        applicationComponent = EisReportStatusRequest.ApplicationComponent.CDAP,
        statusCode = StatusCode.FILESENT.toString,
        statusMessage = "Message1",
        statusTimestamp = "2023-01-01T10:00:00Z",
        statusType = EisReportStatusRequest.StatusType.INFORMATION
      )
    ),
    fileNotifications = Some(
      Seq(
        FileNotification(
          fileName = "example.txt",
          fileSize = 1024,
          retentionDays = 30,
          fileType = FileType.CSV,
          mDTPReportXCorrelationID = "X-Correlation-ID",
          mDTPReportRequestID = "Request-ID",
          mDTPReportTypeName = ReportTypeName.IMPORTS_ITEM_REPORT,
          reportFilesParts = "Part1"
        )
      )
    ),
    linkAvailableTime = Some(Instant.parse("2023-01-03T10:00:00Z"))
  )
  val appConfig: AppConfig  = app.injector.instanceOf[AppConfig]

  val reportRequestRepository: ReportRequestRepository = new ReportRequestRepository(appConfig, mongoComponent)

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

  "getAvailableReports" should {
    "return only ReportRequests where all parts are present" in {
      val reqId          = "REQ123"
      val reportRequests = Seq(
        // Complete set: 1Of3, 2Of3, 3Of3
        ReportRequest(
          reportRequestId = reqId,
          correlationId = "C1",
          reportName = "Report1",
          requesterEORI = "EORI-1",
          eoriRole = EoriRole.TRADER,
          reportEORIs = Array("EORI-1"),
          recipientEmails = Array("a@b.com"),
          reportTypeName = ReportTypeName.IMPORTS_ITEM_REPORT,
          reportStart = Instant.now,
          reportEnd = Instant.now,
          createDate = Instant.now,
          notifications = Seq.empty,
          fileNotifications = Some(
            Seq(
              FileNotification("f1", 1, 1, FileType.CSV, "x", "y", ReportTypeName.IMPORTS_ITEM_REPORT, "1Of3"),
              FileNotification("f2", 1, 1, FileType.CSV, "x", "y", ReportTypeName.IMPORTS_ITEM_REPORT, "2Of3"),
              FileNotification("f3", 1, 1, FileType.CSV, "x", "y", ReportTypeName.IMPORTS_ITEM_REPORT, "3Of3")
            )
          ),
          linkAvailableTime = Some(Instant.now)
        ),
        // Incomplete set: only 1Of2
        ReportRequest(
          reportRequestId = "REQ124",
          correlationId = "C2",
          reportName = "Report2",
          requesterEORI = "EORI-1",
          eoriRole = EoriRole.TRADER,
          reportEORIs = Array("EORI-1"),
          recipientEmails = Array("a@b.com"),
          reportTypeName = ReportTypeName.IMPORTS_ITEM_REPORT,
          reportStart = Instant.now,
          reportEnd = Instant.now,
          createDate = Instant.now,
          notifications = Seq.empty,
          fileNotifications = Some(
            Seq(
              FileNotification("f4", 1, 1, FileType.CSV, "x", "y", ReportTypeName.IMPORTS_ITEM_REPORT, "1Of2")
            )
          ),
          linkAvailableTime = Some(Instant.now)
        )
      )

      reportRequests.foreach(r => reportRequestRepository.insert(r).futureValue)

      val result = reportRequestRepository.getAvailableReports("EORI-1").futureValue
      result.map(_.reportRequestId) must contain only reqId
    }

    "not return ReportRequest if not all parts are present" in {
      val reqId             = "REQ125"
      val incompleteRequest = ReportRequest(
        reportRequestId = reqId,
        correlationId = "C3",
        reportName = "Report3",
        requesterEORI = "EORI-2",
        eoriRole = EoriRole.TRADER,
        reportEORIs = Array("EORI-2"),
        recipientEmails = Array("a@b.com"),
        reportTypeName = ReportTypeName.IMPORTS_ITEM_REPORT,
        reportStart = Instant.now,
        reportEnd = Instant.now,
        createDate = Instant.now,
        notifications = Seq.empty,
        fileNotifications = Some(
          Seq(
            FileNotification("f5", 1, 1, FileType.CSV, "x", "y", ReportTypeName.IMPORTS_ITEM_REPORT, "1Of2")
            // Missing 2Of2
          )
        ),
        linkAvailableTime = Some(Instant.now)
      )

      reportRequestRepository.insert(incompleteRequest).futureValue

      val result = reportRequestRepository.getAvailableReports("EORI-2").futureValue
      result mustBe empty
    }
  }

  "countAvailableReports" should {
    "return the correct count of ReportRequests where all parts are present" in {
      val reqId          = "REQ126"
      val reportRequests = Seq(
        // Complete set: 1Of3, 2Of3, 3Of3
        ReportRequest(
          reportRequestId = reqId,
          correlationId = "C4",
          reportName = "Report4",
          requesterEORI = "EORI-3",
          eoriRole = EoriRole.TRADER,
          reportEORIs = Array("EORI-3"),
          recipientEmails = Array("a@b.com"),
          reportTypeName = ReportTypeName.IMPORTS_ITEM_REPORT,
          reportStart = Instant.now,
          reportEnd = Instant.now,
          createDate = Instant.now,
          notifications = Seq.empty,
          fileNotifications = Some(
            Seq(
              FileNotification("f6", 1, 1, FileType.CSV, "x", "y", ReportTypeName.IMPORTS_ITEM_REPORT, "1Of3"),
              FileNotification("f7", 1, 1, FileType.CSV, "x", "y", ReportTypeName.IMPORTS_ITEM_REPORT, "2Of3"),
              FileNotification("f8", 1, 1, FileType.CSV, "x", "y", ReportTypeName.IMPORTS_ITEM_REPORT, "3Of3")
            )
          ),
          linkAvailableTime = Some(Instant.now)
        ),
        // Incomplete set: only 1Of2
        ReportRequest(
          reportRequestId = "REQ127",
          correlationId = "C5",
          reportName = "Report5",
          requesterEORI = "EORI-3",
          eoriRole = EoriRole.TRADER,
          reportEORIs = Array("EORI-3"),
          recipientEmails = Array("a@b.com"),
          reportTypeName = ReportTypeName.IMPORTS_ITEM_REPORT,
          reportStart = Instant.now,
          reportEnd = Instant.now,
          createDate = Instant.now,
          notifications = Seq.empty,
          fileNotifications = Some(
            Seq(
              FileNotification("f9", 1, 1, FileType.CSV, "x", "y", ReportTypeName.IMPORTS_ITEM_REPORT, "1Of2")
            )
          ),
          linkAvailableTime = Some(Instant.now)
        )
      )

      reportRequests.foreach(r => reportRequestRepository.insert(r).futureValue)

      val count = reportRequestRepository.countAvailableReports("EORI-3").futureValue
      count mustEqual 1
    }

    "return zero if not all parts are present" in {
      val reqId             = "REQ128"
      val incompleteRequest = ReportRequest(
        reportRequestId = reqId,
        correlationId = "C6",
        reportName = "Report6",
        requesterEORI = "EORI-4",
        eoriRole = EoriRole.TRADER,
        reportEORIs = Array("EORI-4"),
        recipientEmails = Array("a@b.com"),
        reportTypeName = ReportTypeName.IMPORTS_ITEM_REPORT,
        reportStart = Instant.now,
        reportEnd = Instant.now,
        createDate = Instant.now,
        notifications = Seq.empty,
        fileNotifications = Some(
          Seq(
            FileNotification("f10", 1, 1, FileType.CSV, "x", "y", ReportTypeName.IMPORTS_ITEM_REPORT, "1Of2")
            // Missing 2Of2
          )
        ),
        linkAvailableTime = Some(Instant.now)
      )

      reportRequestRepository.insert(incompleteRequest).futureValue

      val count = reportRequestRepository.countAvailableReports("EORI-4").futureValue
      count mustEqual 0
    }
  }
