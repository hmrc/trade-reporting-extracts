package uk.gov.hmrc.tradereportingextracts.models.thirdParty

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.Json

import java.time.Instant

class EditThirdPartyRequestSpec extends AnyFreeSpec with Matchers {

  "EditThirdPartyRequest" - {

    "must serialise to JSON" in {
      val request = EditThirdPartyRequest(
        userEORI = "123",
        thirdPartyEORI = "456",
        accessStart = Some(Instant.parse("2024-01-01T00:00:00Z")),
        accessEnd = None,
        reportDateStart = None,
        reportDateEnd = None,
        accessType = Some(Set("IMPORTS")),
        referenceName = Some("newRef")
      )

      val json = Json.toJson(request)

      (json \ "userEORI").as[String] mustBe "123"
      (json \ "thirdPartyEORI").as[String] mustBe "456"
      (json \ "accessStart").as[String] mustBe "2024-01-01T00:00:00Z"
      (json \ "referenceName").as[String] mustBe "newRef"
      (json \ "accessType").as[Set[String]] mustBe Set("IMPORTS")
    }

    "must deserialise from JSON" in {
      val json = Json.parse(
        """
          |{
          |  "userEORI": "123",
          |  "thirdPartyEORI": "456",
          |  "accessStart": "2024-01-01T00:00:00Z",
          |  "accessEnd": null,
          |  "reportDateStart": null,
          |  "reportDateEnd": null,
          |  "accessType": ["IMPORTS"],
          |  "referenceName": "newRef"
          |}
          |""".stripMargin)

      val result = json.as[EditThirdPartyRequest]

      result.userEORI mustBe "123"
      result.thirdPartyEORI mustBe "456"
      result.accessStart mustBe Some(Instant.parse("2024-01-01T00:00:00Z"))
      result.referenceName mustBe Some("newRef")
      result.accessType mustBe Some(Set("IMPORTS"))
    }

    "must round trip" in {
      val request = EditThirdPartyRequest(
        "123",
        "456",
        None,
        None,
        None,
        None,
        None,
        Some("newRef")
      )

      val json = Json.toJson(request)
      val result = json.as[EditThirdPartyRequest]

      result mustBe request
    }
  }
}
