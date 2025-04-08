package uk.gov.hmrc.tradereportingextracts.models

import play.api.libs.json.{Format, JsError, JsResult, JsString, JsSuccess, JsValue}

enum AccessType:
  case Importer, Exporter, Declarant

object AccessType:
  given Format[AccessType] with
    def writes(accessType: AccessType): JsValue = JsString(accessType.toString)

    def reads(json: JsValue): JsResult[AccessType] = json match
      case JsString(value) =>
        values.find(_.toString == value) match
          case Some(accessType) => JsSuccess(accessType)
          case None             => JsError(s"Unknown AccessType: $value")
      case _ => JsError("AccessType must be a string")