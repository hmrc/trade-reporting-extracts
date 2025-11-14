package uk.gov.hmrc.tradereportingextracts.models.audit

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.Json

class ReportGenerationFailureEventSpec extends AnyFreeSpec with Matchers {

  "ReportGenerationFailureEvent" - {

    "should serialise to the correct JSON structure" in {
      val event = ReportGenerationFailureEvent(
        xCorrelationId = "corr-123",
        statusNotificationCode = "FILENOREC"
      )

      Json.toJson(event) mustBe Json.obj(
        "xCorrelationId"         -> "corr-123",
        "statusNotificationCode" -> "FILENOREC"
      )
    }

    "should deserialise correctly from valid JSON" in {
      val json = Json.obj(
        "xCorrelationId"         -> "corr-123",
        "statusNotificationCode" -> "FILENOREC"
      )

      json.as[ReportGenerationFailureEvent] mustBe
        ReportGenerationFailureEvent("corr-123", "FILENOREC")
    }
  }
}
