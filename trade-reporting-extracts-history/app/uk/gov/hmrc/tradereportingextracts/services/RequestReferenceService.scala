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

import uk.gov.hmrc.tradereportingextracts.repositories.ReportRequestRepository

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

@Singleton
class RequestReferenceService @Inject() (
  reportRequestRepository: ReportRequestRepository
)(implicit ec: ExecutionContext) {

  private val prefix = "RE"

  def generateUnique(): Future[String] = {
    def generate(): String = prefix + f"${Random.nextInt(100000000)}%08d"

    def tryGenerate(): Future[String] = {
      val candidate = generate()
      reportRequestRepository.findByReportRequestId(candidate).flatMap {
        case Some(_) => tryGenerate() // ID exists, try again
        case None    => Future.successful(candidate) // Unique ID found
      }
    }

    tryGenerate()
  }
}
