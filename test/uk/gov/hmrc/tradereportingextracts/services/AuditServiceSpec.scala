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

import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, anyString}
import org.mockito.Mockito.{doNothing, verify}
import org.scalatest.matchers.should.Matchers.*
import play.api.test.Helpers.running
import play.api.{Application, inject}
import uk.gov.hmrc.crypto.Sensitive.SensitiveString
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.tradereportingextracts.models.*
import uk.gov.hmrc.tradereportingextracts.models.StatusCode.INITIATED
import uk.gov.hmrc.tradereportingextracts.models.audit.{AuditEvent, ReportRequestSubmittedEvent}
import uk.gov.hmrc.tradereportingextracts.models.eis.EisReportStatusRequest
import uk.gov.hmrc.tradereportingextracts.utils.SpecBase

import java.time.{Instant, LocalDate}
import scala.concurrent.ExecutionContext

class AuditServiceSpec extends SpecBase {

  "AuditService" should {

    "send audit event when reportRequests is non-empty" in new Setup {
      val reportRequest: ReportRequest = ReportRequest(
        reportRequestId = "RR123",
        correlationId = "CORR123",
        requesterEORI = "GB123456789000",
        reportEORIs = Seq("GB987654321000"),
        reportTypeName = ReportTypeName.IMPORTS_ITEM_REPORT,
        reportName = "Trader Report",
        reportStart = Instant.parse("2025-07-01T00:00:00Z"),
        reportEnd = Instant.parse("2025-07-31T23:59:59Z"),
        createDate = Instant.now(),
        updateDate = Instant.now(),
        eoriRole = EoriRole.TRADER,
        recipientEmails = Seq(SensitiveString("user@example.com")),
        userEmail = Some(SensitiveString("user@example.com")),
        notifications = Seq(
          EisReportStatusRequest(
            applicationComponent = EisReportStatusRequest.ApplicationComponent.TRE,
            statusCode = INITIATED.toString,
            statusMessage = "Report sent to EIS successfully",
            statusTimestamp = LocalDate.now().toString,
            statusType = EisReportStatusRequest.StatusType.INFORMATION
          )
        ),
        fileNotifications = None
      )

      val extendedDataEventCaptor: ArgumentCaptor[AuditEvent] =
        ArgumentCaptor.forClass(classOf[AuditEvent])

      running(app) {

        doNothing()
          .when(mockAuditConnector)
          .sendExplicitAudit(anyString(), any[ReportRequestSubmittedEvent])(any(), any(), any())

        val result = service.auditReportRequestSubmitted(Seq(reportRequest), Set("importer"))

        whenReady(result) { _ =>

          val captor = ArgumentCaptor.forClass(classOf[ReportRequestSubmittedEvent])
          verify(mockAuditConnector).sendExplicitAudit(anyString(), captor.capture())(any(), any(), any())

          val event = captor.getValue
          event.reports.head.requestId shouldBe "RR123"
        }
      }
    }

    "return Future.unit when reportRequests is empty" in new Setup {
      running(app) {
        val result = service.auditReportRequestSubmitted(Seq.empty, Set.empty)
        whenReady(result)(_ shouldBe ())
      }
    }
  }

  trait Setup {
    implicit val hc: HeaderCarrier                  = HeaderCarrier()
    implicit val executionContext: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

    val mockAuditConnector: AuditConnector = mock[AuditConnector]

    val app: Application = application
      .overrides(
        inject.bind[AuditConnector].toInstance(mockAuditConnector)
      )
      .build()

    val service: AuditService = app.injector.instanceOf[AuditService]
  }
}
