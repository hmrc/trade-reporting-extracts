package uk.gov.hmrc.tradereportingextracts.utils

import play.api.test.FakeRequest

class HeaderUtilsSpec extends SpecBase {

  "HeaderUtils" should {

    "return no missing headers when all required headers are present" in {
      val req = FakeRequest().withHeaders(
        "X-Header-1" -> "value1",
        "X-Header-2" -> "value2"
      )

      val required = Seq("X-Header-1", "X-Header-2")
      HeaderUtils.missingHeaders(req, required) shouldBe Seq.empty
    }

    "return the missing headers when some required headers are absent" in {
      val req = FakeRequest().withHeaders(
        "X-Header-1" -> "value1"
      )

      val required = Seq("X-Header-1", "X-Header-2", "X-Header-3")
      HeaderUtils.missingHeaders(req, required) shouldBe Seq("X-Header-2", "X-Header-3")
    }

    "authorize when header contains default Bearer prefix and expected token" in {
      val token = "token123"
      val req = FakeRequest().withHeaders(
        "Authorization" -> s"Bearer $token"
      )

      HeaderUtils.isAuthorized(req, expectedToken = token, authHeaderName = "Authorization") shouldBe true
    }

    "not authorize when authorization header is missing" in {
      val token = "token123"
      val req = FakeRequest()

      HeaderUtils.isAuthorized(req, expectedToken = token, authHeaderName = "Authorization") shouldBe false
    }

    "not authorize when token does not match" in {
      val req = FakeRequest().withHeaders(
        "Authorization" -> s"Bearer wrong-token"
      )

      HeaderUtils.isAuthorized(req, expectedToken = "expected-token", authHeaderName = "Authorization") shouldBe false
    }

    "authorize with a custom bearer prefix" in {
      val token = "customToken"
      val prefix = "Token "
      val req = FakeRequest().withHeaders(
        "X-Custom-Auth" -> s"$prefix$token"
      )

      HeaderUtils.isAuthorized(req, expectedToken = token, authHeaderName = "X-Custom-Auth", bearerPrefix = prefix) shouldBe true
    }

    "not authorize when prefix matches but token missing after prefix" in {
      val req = FakeRequest().withHeaders(
        "Authorization" -> "Bearer "
      )

      HeaderUtils.isAuthorized(req, expectedToken = "", authHeaderName = "Authorization") shouldBe true
      HeaderUtils.isAuthorized(req, expectedToken = "non-empty", authHeaderName = "Authorization") shouldBe false
    }
  }
}