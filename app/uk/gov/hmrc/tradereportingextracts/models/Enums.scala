package uk.gov.hmrc.tradereportingextracts.models

import play.api.libs.json.*

enum UserType:
  case Trader, Agent

object UserType:
  given Format[UserType] with
    def writes(userType: UserType): JsValue = JsString(userType.toString)

    def reads(json: JsValue): JsResult[UserType] = json match
      case JsString(value) =>
        values.find(_.toString == value) match
          case Some(userType) => JsSuccess(userType)
          case None => JsError(s"Unknown UserType: $value")
      case _ => JsError("UserType must be a string")

enum AccessType:
  case IMPORTS, EXPORTS, DECLARATIONS

object AccessType:
  given Format[AccessType] with
    def writes(accessType: AccessType): JsValue = JsString(accessType.toString)

    def reads(json: JsValue): JsResult[AccessType] = json match
      case JsString(value) =>
        values.find(_.toString == value) match
          case Some(accessType) => JsSuccess(accessType)
          case None => JsError(s"Unknown AccessType: $value")
      case _ => JsError("AccessType must be a string")

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
            case None => play.api.libs.json.JsError(s"Unknown EoriRole: $value")
        case _ => play.api.libs.json.JsError("EoriRole must be a string")

enum ReportTypeName:
  case IMPORTS_ITEM_REPORT, IMPORTS_HEADER_REPORT, IMPORTS_TAXLINE_REPORT, EXPORTS_ITEM_REPORT

object ReportTypeName:
  given Format[ReportTypeName] with
    def writes(reportTypeName: ReportTypeName): JsValue = JsString(reportTypeName.toString)

    def reads(json: JsValue): JsResult[ReportTypeName] = json match
      case JsString(value) =>
        values.find(_.toString == value) match
          case Some(reportTypeName) => JsSuccess(reportTypeName)
          case None => JsError(s"Unknown ReportTypeName: $value")
      case _ => JsError("ReportTypeName must be a string")

enum Component:
  case CDAP, EIS, SDES

object Component:
  given Format[Component] with
    def writes(component: Component): JsValue = JsString(component.toString)

    def reads(json: JsValue): JsResult[Component] = json match
      case JsString(value) =>
        Component.values.find(_.toString == value) match
          case Some(component) => JsSuccess(component)
          case None => JsError(s"Unknown Component: $value")
      case _ => JsError("Component must be a string")

enum StatusType:
  case INFORMATION, ERROR

object StatusType:
  given Format[StatusType] with
    def writes(statusType: StatusType): JsValue = JsString(statusType.toString)

    def reads(json: JsValue): JsResult[StatusType] = json match
      case JsString(value) =>
        StatusType.values.find(_.toString == value) match
          case Some(statusType) => JsSuccess(statusType)
          case None => JsError(s"Unknown StatusType: $value")
      case _ => JsError("StatusType must be a string")


enum StatusCode:
  case FILENOREC, FILEEXTFAIL, FILESENT

object StatusCode:
  given Format[StatusCode] with
    def writes(statusCode: StatusCode): JsValue = JsString(statusCode.toString)

    def reads(json: JsValue): JsResult[StatusCode] = json match
      case JsString(value) =>
        StatusCode.values.find(_.toString == value) match
          case Some(statusCode) => JsSuccess(statusCode)
          case None => JsError(s"Unknown StatusCode: $value")
      case _ => JsError("StatusCode must be a string")