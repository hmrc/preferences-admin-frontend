import play.core.PlayVersion
import play.sbt.PlayImport._
import sbt._

object FrontendBuild extends Build with MicroService {

  val appName = "preferences-admin-frontend"

  override lazy val appDependencies: Seq[ModuleID] = compile ++ test()

  val compile = Seq(
    ws,
    "uk.gov.hmrc"       %% "bootstrap-play-26" % "4.0.0",
    "uk.gov.hmrc"       %% "auth-client"       % "5.6.0-play-26",
    "uk.gov.hmrc"       %% "govuk-template"    % "5.60.0-play-26",
    "uk.gov.hmrc"       %% "play-partials"     % "6.9.0-play-26",
    "com.typesafe.play" %% "play-json-joda"    % "2.6.13",
    "uk.gov.hmrc"       %% "play-ui"           % "8.18.0-play-26"
  )

  def test(scope: String = "test") = Seq(
    "uk.gov.hmrc"            %% "hmrctest"                    % "3.9.0-play-26"     % scope,
    "org.scalatest"          %% "scalatest"                   % "3.0.1"             % scope,
    "org.scalactic"          %% "scalactic"                   % "3.0.1"             % scope,
    "org.pegdown"            % "pegdown"                      % "1.6.0"             % scope,
    "org.jsoup"              % "jsoup"                        % "1.8.1"             % scope,
    "com.typesafe.play"      %% "play-test"                   % PlayVersion.current % scope,
    "org.scalatestplus.play" %% "scalatestplus-play"          % "3.1.0"             % scope,
    "org.mockito"            % "mockito-core"                 % "2.7.20"            % scope,
    "org.scalamock"          %% "scalamock-scalatest-support" % "3.5.0"             % scope
  )
}
