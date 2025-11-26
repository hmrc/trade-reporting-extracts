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

import org.scalactic.Equality
import org.scalatest.concurrent.IntegrationPatience
import org.scalatest.matchers.must.Matchers.{must, mustBe, mustEqual}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import uk.gov.hmrc.crypto.Sensitive.SensitiveString
import uk.gov.hmrc.crypto.{Decrypter, Encrypter}
import uk.gov.hmrc.mongo.test.CleanMongoCollectionSupport
import uk.gov.hmrc.tradereportingextracts.config.{AppConfig, CryptoProvider}
import uk.gov.hmrc.tradereportingextracts.models.*
import uk.gov.hmrc.tradereportingextracts.models.eis.EisReportStatusRequest

import java.time.{Instant, LocalDate, ZoneOffset}
import scala.concurrent.ExecutionContext.Implicits.global

implicit val eqReportRequest: Equality[ReportRequest] = (a, b) => a == b

class ReportRequestRepositorySpec
    extends AnyWordSpec,
      MockitoSugar,
      GuiceOneAppPerSuite,
      CleanMongoCollectionSupport,
      IntegrationPatience,
      Matchers:

  private val reportRequest               = ReportRequest(
    reportRequestId = "REQ00001",
    correlationId = "ABCD-DEFG",
    reportName = "Jan Report",
    requesterEORI = "GB0019",
    eoriRole = EoriRole.TRADER,
    reportEORIs = Array("EORI1", "EORI2").toIndexedSeq,
    userEmail = Some(SensitiveString("test@example.com")),
    recipientEmails = Seq(SensitiveString("email1@example.com"), SensitiveString("email2@example.com")),
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
          fileType = "CSV",
          mDTPReportXCorrelationID = "X-Correlation-ID",
          mDTPReportRequestID = "Request-ID",
          mDTPReportTypeName = "IMPORTS-ITEM-REPORT",
          reportFilesParts = "1",
          reportLastFile = "true",
          fileCreationTimestamp = "2023-01-01T10:00:00Z"
        )
      )
    ),
    updateDate = Instant.parse("2023-01-03T10:00:00Z")
  )
  val appConfig: AppConfig                = app.injector.instanceOf[AppConfig]
  lazy val cryptoProvider: CryptoProvider = app.injector.instanceOf[CryptoProvider]

  implicit val crypto: Encrypter with Decrypter = cryptoProvider.get

  val reportRequestRepository: ReportRequestRepository = new ReportRequestRepository(appConfig, mongoComponent)

  "insertReportRequest" should {
    "must insert a report successfully" in {
      val insertResult = reportRequestRepository.insert(reportRequest).futureValue
      insertResult mustEqual true
    }
  }

  "findByRequesterEORI" should {
    "must be able to retrieve a report successfully using a requester EORI" in {
      val insertResult  = reportRequestRepository.insert(reportRequest).futureValue
      val fetchedRecord = reportRequestRepository.findByRequesterEORI(reportRequest.requesterEORI).futureValue
      insertResult mustEqual true
      fetchedRecord contains reportRequest
    }
  }

  "findByRequesterEoriHistory" should {
    "must be able to retrieve a report successfully using a requester EORI history" in {
      val insertResult  = reportRequestRepository.insert(reportRequest).futureValue
      val fetchedRecord =
        reportRequestRepository.findByRequesterEoriHistory(Seq("EORI-UNKNOWN", reportRequest.requesterEORI)).futureValue
      insertResult mustEqual true
      fetchedRecord contains reportRequest
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
          reportEORIs = Array("EORI-1").toIndexedSeq,
          userEmail = Some(SensitiveString("test@example.com")),
          recipientEmails = Seq(SensitiveString("a@b.com")),
          reportTypeName = ReportTypeName.IMPORTS_ITEM_REPORT,
          reportStart = Instant.now,
          reportEnd = Instant.now,
          createDate = Instant.now,
          notifications = Seq.empty,
          fileNotifications = Some(
            Seq(
              FileNotification("f1", 1, 1, "CSV", "x", "y", "IMPORTS-ITEM-REPORT", "1", "false", ""),
              FileNotification("f2", 1, 1, "CSV", "x", "y", "IMPORTS-ITEM-REPORT", "2", "false", ""),
              FileNotification("f3", 1, 1, "CSV", "x", "y", "IMPORTS-ITEM-REPORT", "3", "true", "")
            )
          ),
          updateDate = Instant.now
        ),
        // Incomplete set: only 1Of2
        ReportRequest(
          reportRequestId = "REQ124",
          correlationId = "C2",
          reportName = "Report2",
          requesterEORI = "EORI-1",
          eoriRole = EoriRole.TRADER,
          reportEORIs = Array("EORI-1").toIndexedSeq,
          userEmail = Some(SensitiveString("test@example.com")),
          recipientEmails = Seq(SensitiveString("a@b.com")),
          reportTypeName = ReportTypeName.IMPORTS_ITEM_REPORT,
          reportStart = Instant.now,
          reportEnd = Instant.now,
          createDate = Instant.now,
          notifications = Seq.empty,
          fileNotifications = Some(
            Seq(
              FileNotification("f4", 1, 1, "CSV", "x", "y", "IMPORTS-ITEM-REPORT", "1", "false", "")
            )
          ),
          updateDate = Instant.now
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
        reportEORIs = Array("EORI-2").toIndexedSeq,
        userEmail = Some(SensitiveString("test@example.com")),
        recipientEmails = Seq(SensitiveString("a@b.com")),
        reportTypeName = ReportTypeName.IMPORTS_ITEM_REPORT,
        reportStart = Instant.now,
        reportEnd = Instant.now,
        createDate = Instant.now,
        notifications = Seq.empty,
        fileNotifications = Some(
          Seq(
            FileNotification("f5", 1, 1, "CSV", "x", "y", "IMPORTS-ITEM-REPORT", "1", "false", "")
            // Missing 2Of2
          )
        ),
        updateDate = Instant.now
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
          reportEORIs = Array("EORI-3").toIndexedSeq,
          userEmail = Some(SensitiveString("test@example.com")),
          recipientEmails = Seq(SensitiveString("a@b.com")),
          reportTypeName = ReportTypeName.IMPORTS_ITEM_REPORT,
          reportStart = Instant.now,
          reportEnd = Instant.now,
          createDate = Instant.now,
          notifications = Seq.empty,
          fileNotifications = Some(
            Seq(
              FileNotification("f6", 1, 1, "CSV", "x", "y", "IMPORTS-ITEM-REPORT", "1", "false", ""),
              FileNotification("f7", 1, 1, "CSV", "x", "y", "IMPORTS-ITEM-REPORT", "2", "false", ""),
              FileNotification("f8", 1, 1, "CSV", "x", "y", "IMPORTS-ITEM-REPORT", "3", "true", "")
            )
          ),
          updateDate = Instant.now
        ),
        // Incomplete set: only 1Of2
        ReportRequest(
          reportRequestId = "REQ127",
          correlationId = "C5",
          reportName = "Report5",
          requesterEORI = "EORI-3",
          eoriRole = EoriRole.TRADER,
          reportEORIs = Array("EORI-3").toIndexedSeq,
          userEmail = Some(SensitiveString("test@example.com")),
          recipientEmails = Seq(SensitiveString("a@b.com")),
          reportTypeName = ReportTypeName.IMPORTS_ITEM_REPORT,
          reportStart = Instant.now,
          reportEnd = Instant.now,
          createDate = Instant.now,
          notifications = Seq.empty,
          fileNotifications = Some(
            Seq(
              FileNotification("f9", 1, 1, "CSV", "x", "y", "IMPORTS-ITEM-REPORT", "1", "false", "")
            )
          ),
          updateDate = Instant.now
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
        reportEORIs = Array("EORI-4").toIndexedSeq,
        userEmail = Some(SensitiveString("test@example.com")),
        recipientEmails = Seq(SensitiveString("a@b.com")),
        reportTypeName = ReportTypeName.IMPORTS_ITEM_REPORT,
        reportStart = Instant.now,
        reportEnd = Instant.now,
        createDate = Instant.now,
        notifications = Seq.empty,
        fileNotifications = Some(
          Seq(
            FileNotification("f10", 1, 1, "CSV", "x", "y", "IMPORTS-ITEM-REPORT", "1", "false", "")
            // Missing 2Of2
          )
        ),
        updateDate = Instant.now
      )

      reportRequestRepository.insert(incompleteRequest).futureValue

      val count = reportRequestRepository.countAvailableReports("EORI-4").futureValue
      count mustEqual 0
    }
  }

  "countReportSubmissionsForEoriOnDate" should {
    "return correct count of reports submitted on a specific date" in {
      val targetDate = LocalDate.of(2023, 7, 15)
      val startOfDay = targetDate.atStartOfDay().toInstant(ZoneOffset.UTC)
      val endOfDay   = targetDate.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)

      val eori = "EORI-TEST-1"

      val report1             = reportRequest.copy(
        reportRequestId = "REQ-DATE-1",
        requesterEORI = eori,
        createDate = startOfDay.plusSeconds(1)
      )
      val report2             = reportRequest.copy(
        reportRequestId = "REQ-DATE-2",
        requesterEORI = eori,
        createDate = startOfDay.plusSeconds(10)
      )
      val reportOutsideWindow = reportRequest.copy(
        reportRequestId = "REQ-DATE-OUT",
        requesterEORI = eori,
        createDate = endOfDay.plusSeconds(1)
      )

      reportRequestRepository.insertAll(Seq(report1, report2, reportOutsideWindow)).futureValue

      val count = reportRequestRepository.countReportSubmissionsForEoriOnDate(eori, targetDate).futureValue
      count mustEqual 2
    }

    "return 0 when no reports exist for the given date and EORI" in {
      val eori       = "EORI-NONE"
      val targetDate = LocalDate.of(2023, 7, 16)

      val count = reportRequestRepository.countReportSubmissionsForEoriOnDate(eori, targetDate).futureValue
      count mustEqual 0
    }

    "not count reports from a different EORI" in {
      val eori1 = "EORI-A"
      val eori2 = "EORI-B"
      val date  = LocalDate.of(2023, 7, 17)

      val validReport = reportRequest.copy(
        reportRequestId = "REQ-EORI-A",
        requesterEORI = eori1,
        createDate = date.atStartOfDay(ZoneOffset.UTC).plusSeconds(5).toInstant()
      )

      val otherEoriReport = reportRequest.copy(
        reportRequestId = "REQ-EORI-B",
        requesterEORI = eori2,
        createDate = date.atStartOfDay(ZoneOffset.UTC).plusSeconds(5).toInstant()
      )

      reportRequestRepository.insertAll(Seq(validReport, otherEoriReport)).futureValue

      val count = reportRequestRepository.countReportSubmissionsForEoriOnDate(eori1, date).futureValue
      count mustEqual 1
    }
  }

  "deleteReportsForThirdPartyRemoval" should {

    val traderEori          = "traderEori"
    val differentTraderEori = "differentTraderEori"
    val thirdPartyEori      = "thirdPartyEori"

    "must delete third party reports for specific trader eori only" in {

      val userReportForThirdParty = reportRequest.copy(
        reportRequestId = "userRequest",
        requesterEORI = thirdPartyEori,
        reportEORIs = Seq(thirdPartyEori)
      )

      val thirdPartyRequestForTrader = reportRequest.copy(
        reportRequestId = "thirdPartyRequest",
        requesterEORI = thirdPartyEori,
        reportEORIs = Seq(traderEori)
      )

      reportRequestRepository.insertAll(Seq(userReportForThirdParty, thirdPartyRequestForTrader)).futureValue

      val result = reportRequestRepository.deleteReportsForThirdPartyRemoval(traderEori, thirdPartyEori).futureValue
      result mustEqual true

      val remaining = reportRequestRepository.getAvailableReports(thirdPartyEori).futureValue
      remaining.map(_.reportRequestId) must contain only "userRequest"

    }

    "must not delete third party reports for other third parties" in {

      val thirdPartyRequestForTrader = reportRequest.copy(
        reportRequestId = "thirdPartyRequest",
        requesterEORI = thirdPartyEori,
        reportEORIs = Seq(traderEori)
      )

      val thirdPartyRequestForDifferentTrader = reportRequest.copy(
        reportRequestId = "thirdPartyRequestDifferentTrader",
        requesterEORI = thirdPartyEori,
        reportEORIs = Seq(differentTraderEori)
      )

      reportRequestRepository
        .insertAll(Seq(thirdPartyRequestForTrader, thirdPartyRequestForDifferentTrader))
        .futureValue

      val result    = reportRequestRepository.deleteReportsForThirdPartyRemoval(traderEori, thirdPartyEori).futureValue
      result mustEqual true
      val remaining = reportRequestRepository.getAvailableReports(thirdPartyEori).futureValue
      remaining.map(_.reportRequestId) must contain only "thirdPartyRequestDifferentTrader"

    }
  }

  "getRequestedReportsByHistory" should {
    "must be able to retrieve a report successfully using a requester EORI history" in {
      val insertResult  = reportRequestRepository.insert(reportRequest).futureValue
      val fetchedRecord =
        reportRequestRepository
          .getRequestedReportsByHistory(Seq("EORI-UNKNOWN", reportRequest.requesterEORI))
          .futureValue
      insertResult mustEqual true
      fetchedRecord contains reportRequest
    }
  }

  "getAvailableReportsByHistory" should {
    "return only ReportRequests where all parts are present using EORI history" in {
      val reportRequestWithEoriHistory = reportRequest.copy(requesterEORI = "historicalEori")
      val insertResult                 = reportRequestRepository.insert(reportRequestWithEoriHistory).futureValue

      insertResult mustEqual true
      reportRequestRepository.getAvailableReportsByHistory(Seq("historicalEori", "anotherEori")).futureValue mustBe Seq(
        reportRequestWithEoriHistory
      )
    }
  }
