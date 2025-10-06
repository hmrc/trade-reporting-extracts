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

package uk.gov.hmrc.tradereportingextracts.controllers

import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents, Result}
import play.api.{Environment, Mode}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.internalauth.client.*
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.tradereportingextracts.models.audit.AuditDownloadRequest
import uk.gov.hmrc.tradereportingextracts.models.{AvailableReportAction, AvailableReportResponse, FileType}
import uk.gov.hmrc.tradereportingextracts.services.AvailableReportService
import uk.gov.hmrc.tradereportingextracts.utils.ApplicationConstants.eori
import uk.gov.hmrc.tradereportingextracts.utils.PermissionsUtil.readPermission

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AvailableReportController @Inject() (
  cc: ControllerComponents,
  auth: BackendAuthComponents,
  availableReportService: AvailableReportService,
  environment: Environment
)(using
  executionContext: ExecutionContext
) extends BackendController(cc) {

  def getAvailableReports: Action[AnyContent] = auth.authorizedAction(readPermission).async { implicit request =>
    implicit val hc: HeaderCarrier = HeaderCarrier()
    request.body.asJson.flatMap(json => (json \ eori).asOpt[String]) match {
      case Some(eoriValue) =>
        availableReportService.getAvailableReports(eoriValue).map { reports =>
          val reportsWithLinks = if environment.mode != Mode.Prod then addDownLoadLinks(reports) else reports
          Ok(Json.toJson(reportsWithLinks))
        }
      case _               =>
        Future.successful(BadRequest("Missing or invalid EORI in request body"))
    }
  }

  private def addDownLoadLinks(reports: AvailableReportResponse): AvailableReportResponse =
    reports.copy(
      availableUserReports = reports.availableUserReports.map { userReports =>
        userReports.map { userReport =>
          userReport.copy(
            action = Seq(
              AvailableReportAction(
                fileURL =
                  "https://raw.githubusercontent.com/hmrc/trade-reporting-extracts/refs/heads/main/resources/payload/RE14648803_IMPORTS-ITEM-REPORT_20250910_1.csv",
                size = 100,
                fileType = FileType.CSV,
                fileName = "RE9999_IMPORTS-REPORT_20250728_1.csv"
              )
            )
          )
        }
      },
      availableThirdPartyReports = reports.availableThirdPartyReports.map { thirdPartyReports =>
        thirdPartyReports.map { report =>
          report.copy(
            action = Seq(
              AvailableReportAction(
                fileURL =
                  "https://raw.githubusercontent.com/hmrc/trade-reporting-extracts/refs/heads/main/resources/payload/RE14648803_IMPORTS-ITEM-REPORT_20250910_1.csv",
                size = 100,
                fileType = FileType.CSV,
                fileName = "RE9999_IMPORTS-REPORT_20250728_1.csv"
              )
            )
          )
        }
      }
    )

  def getAvailableReportsCount: Action[AnyContent] = auth.authorizedAction(readPermission).async { implicit request =>
    request.body.asJson.flatMap(json => (json \ eori).asOpt[String]) match {
      case Some(eoriValue) =>
        availableReportService.getAvailableReportsCount(eoriValue).map { count =>
          Ok(Json.toJson(count))
        }
      case _               =>
        Future.successful(BadRequest("Missing or invalid EORI in request body"))
    }
  }

  def auditReportDownload: Action[AnyContent] = auth.authorizedAction(readPermission).async { implicit request =>
    availableReportService
      .processReportDownloadAudit(
        request.body.asJson.flatMap(_.validate[AuditDownloadRequest].asOpt)
      )
      .map {
        case Right(_)    => NoContent
        case Left(error) => error
      }
  }
}
