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

package uk.gov.hmrc.tradereportingextracts.models.eis

import uk.gov.hmrc.tradereportingextracts.models.sdes.FileNotificationHeaders.Value

object EisReportStatusHeaders extends Enumeration {
  type ReportStatusHeaders = Value

  val Authorization: Value       = Value("authorization")
  val ContentType: Value         = Value("content-type")
  val Date: Value                = Value("date")
  val XCorrelationID: Value      = Value("x-correlation-id")
  val XTransmittingSystem: Value = Value("x-transmitting-system")
  val SourceSystem: Value        = Value("source-system")

  def allHeaders: List[String] = values.map(_.toString).toList
}
