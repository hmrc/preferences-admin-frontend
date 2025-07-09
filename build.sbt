import sbt.Keys.*
import sbt.*
import uk.gov.hmrc.*
import DefaultBuildSettings.*
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion

val appName: String = "preferences-admin-frontend"

lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin) // Required to prevent https://github.com/scalatest/scalatest/issues/1427
  .settings(majorVersion := 2)
  .settings(scalaSettings *)
  .settings(defaultSettings() *)
  .settings(
    scalaVersion := "3.3.6",
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
    Test / parallelExecution := false,
    Test / fork := false,
    retrieveManaged := true,
    update / evictionWarningOptions := EvictionWarningOptions.default.withWarnScalaVersionEviction(false),
    routesGenerator := InjectedRoutesGenerator,
    scalacOptions ++= List(
      "-feature",
      "-language:postfixOps",
      "-language:reflectiveCalls"
    )
  )
  .settings(
    TwirlKeys.templateImports ++= Seq(
      "uk.gov.hmrc.preferencesadminfrontend.config.AppConfig",
      "uk.gov.hmrc.govukfrontend.views.html.components._",
      "uk.gov.hmrc.hmrcfrontend.views.html.components._",
      "controllers.routes._"
    )
  )
  .settings(ScoverageSettings())

Test / test := (Test / test)
  .dependsOn(scalafmtCheckAll)
  .value
