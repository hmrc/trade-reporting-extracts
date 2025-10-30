import sbt.*

object AppDependencies {

  private val bootstrapVersion = "10.3.0"
  private val hmrcMongoVersion = "2.10.0"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"       %% "bootstrap-backend-play-30"    % bootstrapVersion,
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-30"           % hmrcMongoVersion,
    "uk.gov.hmrc"       %% "crypto-json-play-30"          % "8.4.0",
    "uk.gov.hmrc"       %% "internal-auth-client-play-30" % "4.3.0"
  )

  val test: Seq[ModuleID] = Seq(
    "org.scalatestplus" %% "mockito-4-11"            % "3.2.18.0",
    "uk.gov.hmrc"       %% "bootstrap-test-play-30"  % bootstrapVersion % Test,
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-test-play-30" % hmrcMongoVersion % Test
  )

  val it: Seq[ModuleID] = Seq.empty
}
