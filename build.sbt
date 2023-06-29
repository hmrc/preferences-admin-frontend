import com.lucidchart.sbt.scalafmt.ScalafmtCorePlugin.autoImport._
import sbt.Keys._
import sbt._
import uk.gov.hmrc._
import DefaultBuildSettings._
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion
import uk.gov.hmrc.DefaultBuildSettings.oneForkedJvmPerTest

val appName: String = "preferences-admin-frontend"

lazy val playSettings: Seq[Setting[_]] = Seq.empty
lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .settings(majorVersion := 1)
  .settings(playSettings: _*)
  .settings(scalaSettings: _*)
  .settings(defaultSettings(): _*)
  .settings(
    scalaVersion := "2.13.8",
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
    retrieveManaged := true,
    evictionWarningOptions in update := EvictionWarningOptions.default.withWarnScalaVersionEviction(false)
  )
  .settings(
    // suppress warnings in generated routes files & html for unused-imports
    scalacOptions += "-Wconf:src=routes/.*:s",
    scalacOptions += "-Wconf:src=html/.*:s"
  )
  .settings(
    TwirlKeys.templateImports ++= Seq(
      "uk.gov.hmrc.preferencesadminfrontend.config.AppConfig",
      "uk.gov.hmrc.govukfrontend.views.html.components._",
      "uk.gov.hmrc.hmrcfrontend.views.html.components._",
      "controllers.routes._"
    )
  )
  .settings(
    scalafmtTestOnCompile in ThisBuild := true
  )
  .settings(resolvers ++= Seq(
    Resolver.jcenterRepo
  ))
  .settings(ScoverageSettings())
