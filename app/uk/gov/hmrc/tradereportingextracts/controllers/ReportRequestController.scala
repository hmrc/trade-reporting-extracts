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

import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.tradereportingextracts.config.AppConfig
import uk.gov.hmrc.tradereportingextracts.connectors.CustomsDataStoreConnector
import uk.gov.hmrc.tradereportingextracts.models.{ReportConfirmation, ReportRequestUserAnswersModel}
import uk.gov.hmrc.tradereportingextracts.services.*

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ReportRequestController @Inject() (
  cc: ControllerComponents,
  customsDataStoreConnector: CustomsDataStoreConnector,
  reportRequestService: ReportRequestService,
  reportRequestTransformationService: ReportRequestTransformationService,
  eisService: EisService,
  auditService: AuditService,
  userService: UserService,
  additionalEmailService: AdditionalEmailService,
  appConfig: AppConfig
)(implicit executionContext: ExecutionContext)
    extends BackendController(cc) {

  def createReportRequest: Action[JsValue] = Action.async(parse.json) { implicit request =>
    request.body.validate[ReportRequestUserAnswersModel] match {
      case JsSuccess(value, _) =>
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val startDate = LocalDate.parse(value.reportStartDate, formatter)
        val endDate   = LocalDate.parse(value.reportEndDate, formatter)

        // Update TTL for trader's EORI when accessed by third-party
        val traderTtlUpdate = if (value.whichEori != value.eori) {
          userService.keepAlive(value.whichEori).recover(_ => false)
        } else {
          Future.successful(true)
        }

        (for {
          _              <- traderTtlUpdate
          userEmail      <- customsDataStoreConnector.getNotificationEmail(value.eori).map(_.address)
          _              <- value.additionalEmail
                              .map { emails =>
                                Future.sequence(emails.map(email => additionalEmailService.updateEmailAccessDate(value.eori, email)))
                              }
                              .getOrElse(Future.successful(Seq.empty))
          eoriHistory    <- customsDataStoreConnector
                              .getEoriHistory(value.whichEori)
                              .map(_.filterByDateRange(startDate, endDate).map(_.eori))
          reportRequests <- Future.sequence {
                              value.reportType.toSeq.map { reportTypeName =>
                                reportRequestTransformationService.transformReportRequest(
                                  value.eori,
                                  value.copy(reportType = Set(reportTypeName)),
                                  eoriHistory,
                                  userEmail
                                )
                              }
                            }
          persisted      <- reportRequestService.createAll(reportRequests)
        } yield (persisted, reportRequests)).flatMap { case (persisted, reportRequests) =>
          if (persisted) {
            Future
              .sequence(
                reportRequests.map { reportRequest =>
                  val eisRequest = reportRequestTransformationService.toEisReportRequest(reportRequest)
                  eisService.requestTraderReport(eisRequest, reportRequest)
                }
              )
              .map { updatedReports =>
                auditService.auditReportRequestSubmitted(updatedReports, value.eoriRole).recover
                Ok(Json.toJson(reportRequestTransformationService.reportConfirmationTransformer(updatedReports)))
              }
          } else {
            Future.successful(InternalServerError(Json.obj("error" -> "Failed to create report requests")))
          }
        }
      case JsError(_)          =>
        Future.successful(BadRequest(Json.obj("error" -> "Invalid request format")))
    }
  }

  def hasReachedSubmissionLimit(eori: String): Action[AnyContent] = Action.async {
    val limit = appConfig.dailySubmissionLimit
    reportRequestService.countReportSubmissionsForEoriOnDate(eori, limit).map { reached =>
      if (reached) {
        TooManyRequests
      } else {
        NoContent
      }
    }
  }

  def getReportRequestLimitNumber: Action[AnyContent] = Action.async {
    Future.successful(Ok(Json.toJson(appConfig.dailySubmissionLimit.toString)))
  }
}
