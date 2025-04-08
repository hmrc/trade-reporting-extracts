package uk.gov.hmrc.tradereportingextracts.models

import play.api.libs.json.{Format, Json}

import java.time.Instant

case class AuthorisedAgent(
  arn: String,
  accessStart: Instant,
  accessEnd: Instant,
  reportDataStart: Instant,
  reportDataEnd: Instant,
  accessType: AccessType
)

object AuthorisedAgent:
  given Format[AuthorisedAgent] = Json.format[AuthorisedAgent]