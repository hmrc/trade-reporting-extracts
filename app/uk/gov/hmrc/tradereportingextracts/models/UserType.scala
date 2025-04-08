package uk.gov.hmrc.tradereportingextracts.models
  
import play.api.libs.json.{Format, JsError, JsResult, JsString, JsSuccess, JsValue}
  
enum UserType:
  case Trader, Agent

object UserType:
  given Format[UserType] with
    def writes(userType: UserType): JsValue = JsString(userType.toString)

    def reads(json: JsValue): JsResult[UserType] = json match
      case JsString(value) =>
        values.find(_.toString == value) match
          case Some(userType) => JsSuccess(userType)
          case None           => JsError(s"Unknown UserType: $value")
      case _ => JsError("UserType must be a string")