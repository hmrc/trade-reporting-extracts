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

package uk.gov.hmrc.tradereportingextracts.models

import play.api.libs.json.*

enum AccessType:
  case IMPORTS, EXPORTS, DECLARATIONS

object AccessType:
  given Format[AccessType] with
    def writes(accessType: AccessType): JsValue = JsString(accessType.toString)

    def reads(json: JsValue): JsResult[AccessType] = json match
      case JsString(value) =>
        values.find(_.toString == value) match
          case Some(accessType) => JsSuccess(accessType)
          case None             => JsError(s"Unknown AccessType: $value")
      case _               => JsError("AccessType must be a string")

enum EoriRole:
  case TRADER, DECLARANT, TRADER_DECLARANT

object EoriRole:
  given Format[EoriRole] = new Format[EoriRole]:
    def writes(role: EoriRole): play.api.libs.json.JsValue = play.api.libs.json.JsString(role.toString)

    def reads(json: play.api.libs.json.JsValue): play.api.libs.json.JsResult[EoriRole] =
      json match
        case play.api.libs.json.JsString(value) =>
          values.find(_.toString == value) match
            case Some(role) => play.api.libs.json.JsSuccess(role)
            case None       => play.api.libs.json.JsError(s"Unknown EoriRole: $value")
        case _                                  => play.api.libs.json.JsError("EoriRole must be a string")

enum ReportTypeName:
  case IMPORTS_ITEM_REPORT, IMPORTS_HEADER_REPORT, IMPORTS_TAXLINE_REPORT, EXPORTS_ITEM_REPORT

object ReportTypeName:
  given Format[ReportTypeName] with
    def writes(reportTypeName: ReportTypeName): JsValue = JsString(reportTypeName.toString)

    def reads(json: JsValue): JsResult[ReportTypeName] = json match
      case JsString(value) =>
        values.find(_.toString == value) match
          case Some(reportTypeName) => JsSuccess(reportTypeName)
          case None                 => JsError(s"Unknown ReportTypeName: $value")
      case _               => JsError("ReportTypeName must be a string")

enum Component:
  case CDAP, EIS, SDES, TRE

object Component:
  given Format[Component] with
    def writes(component: Component): JsValue = JsString(component.toString)

    def reads(json: JsValue): JsResult[Component] = json match
      case JsString(value) =>
        Component.values.find(_.toString == value) match
          case Some(component) => JsSuccess(component)
          case None            => JsError(s"Unknown Component: $value")
      case _               => JsError("Component must be a string")

enum StatusType:
  case INFORMATION, ERROR

object StatusType:
  given Format[StatusType] with
    def writes(statusType: StatusType): JsValue = JsString(statusType.toString)

    def reads(json: JsValue): JsResult[StatusType] = json match
      case JsString(value) =>
        StatusType.values.find(_.toString == value) match
          case Some(statusType) => JsSuccess(statusType)
          case None             => JsError(s"Unknown StatusType: $value")
      case _               => JsError("StatusType must be a string")

enum StatusCode:
  case FILENOREC, FILEEXTFAIL, FILESENT, FAILED, INITIATED

object StatusCode:
  given Format[StatusCode] with
    def writes(statusCode: StatusCode): JsValue = JsString(statusCode.toString)

    def reads(json: JsValue): JsResult[StatusCode] = json match
      case JsString(value) =>
        StatusCode.values.find(_.toString == value) match
          case Some(statusCode) => JsSuccess(statusCode)
          case None             => JsError(s"Unknown StatusCode: $value")
      case _               => JsError("StatusCode must be a string")

enum FileType:
  case CSV, XML

object FileType:
  given Format[FileType] with
    def writes(fileType: FileType): JsValue = JsString(fileType.toString)

    def reads(json: JsValue): JsResult[FileType] = json match
      case JsString(value) =>
        FileType.values.find(_.toString == value) match
          case Some(fileType) => JsSuccess(fileType)
          case None           => JsError(s"Unknown FileType: $value")
      case _               => JsError("FileType must be a string")

enum ReportStatus:
  case COMPLETE, ERROR, IN_PROGRESS, NO_DATA_AVAILABLE

object ReportStatus:
  given Format[ReportStatus] with
    def writes(status: ReportStatus): JsValue = JsString(status.toString)

    def reads(json: JsValue): JsResult[ReportStatus] = json match
      case JsString(value) =>
        ReportStatus.values.find(_.toString == value) match
          case Some(status) => JsSuccess(status)
          case None         => JsError(s"Unknown ReportStatus: $value")
      case _               => JsError("ReportStatus must be a string")
