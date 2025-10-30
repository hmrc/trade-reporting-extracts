import sbt.Setting
import scoverage.ScoverageKeys

object CodeCoverageSettings {

  private val excludedPackages: Seq[String] = Seq(
    "<empty>",
    "Reverse.*",
    "uk.gov.hmrc.BuildInfo",
    "app.*",
    "prod.*",
    ".*Routes.*",
    ".*RoutesPrefix.*",
    "testOnly.*",
    "testonly",
    "testOnlyDoNotUseInAppConf.*",
    "uk.gov.hmrc.tradereportingextracts.config",
    ".*javascript.*"
  )

  private val excludedFiles: Seq[String] = Seq(
    "<empty>",
    ".*javascript.*",
    ".*Routes.*",
    ".*testonly.*"
  )

  val settings: Seq[Setting[_]] = Seq(
    ScoverageKeys.coverageExcludedPackages := excludedPackages.mkString(";"),
    ScoverageKeys.coverageExcludedFiles := excludedFiles.mkString(";"),
    ScoverageKeys.coverageMinimumStmtTotal := 79,
    ScoverageKeys.coverageFailOnMinimum := false,
    ScoverageKeys.coverageHighlighting := true
  )
}
