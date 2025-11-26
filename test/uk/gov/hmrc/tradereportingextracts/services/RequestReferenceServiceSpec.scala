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

import org.mockito.ArgumentMatchers.{any, anyString}
import org.mockito.Mockito.*
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.crypto.Sensitive.SensitiveString
import uk.gov.hmrc.tradereportingextracts.models.eis.EisReportStatusRequest
import uk.gov.hmrc.tradereportingextracts.models.{EoriRole, ReportRequest, ReportTypeName, StatusCode}
import uk.gov.hmrc.tradereportingextracts.repositories.ReportRequestRepository

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

class RequestReferenceServiceSpec extends AsyncFreeSpec with Matchers with MockitoSugar {

  implicit val ec: ExecutionContext = ExecutionContext.global

  "RequestReferenceService" - {
    "generate a unique reference with 8 digits" in {
      val mockRepository = mock[ReportRequestRepository]
      when(mockRepository.findByReportRequestId(anyString())(any[ExecutionContext]))
        .thenReturn(Future.successful(None))

      val service = new RequestReferenceService(mockRepository)

      service.generateUnique().map { reference =>
        reference must startWith("RE")
        reference.length mustBe 10

        val digitsPart = reference.stripPrefix("RE")
        all(digitsPart.toList) must (be >= '0' and be <= '9')
      }
    }
  }
}
