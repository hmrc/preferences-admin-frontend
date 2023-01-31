import play.core.PlayVersion.current
import play.sbt.PlayImport._
import sbt._

object AppDependencies {

  val compile = Seq(
    ws,
    "uk.gov.hmrc"                  %% "bootstrap-frontend-play-28" % "5.16.0",
    "uk.gov.hmrc"                  %% "play-frontend-hmrc"         % "6.2.0-play-28",
    "uk.gov.hmrc"                  %% "auth-client"                % "5.7.0-play-28",
    "uk.gov.hmrc"                  %% "play-partials"              % "8.2.0-play-28",
    "com.typesafe.play"            %% "play-json-joda"             % "2.8.2",
    "com.fasterxml.jackson.module" %% "jackson-module-scala"       % "2.12.2",
    "org.typelevel"                %% "cats-core"                  % "2.6.1"
  )

  val test = Seq(
    "org.scalatest"          %% "scalatest"                   % "3.0.7"   % Test,
    "org.scalactic"          %% "scalactic"                   % "3.0.1"   % Test,
    "org.pegdown"            % "pegdown"                      % "1.6.0"   % Test,
    "org.jsoup"              % "jsoup"                        % "1.8.1"   % Test,
    "com.typesafe.play"      %% "play-test"                   % "2.8.11"  % Test,
    "org.scalatestplus.play" %% "scalatestplus-play"          % "5.1.0"   % Test,
    "org.mockito"            % "mockito-core"                 % "3.9.0"   % Test,
    "org.scalatestplus"      %% "mockito-3-4"                 % "3.2.8.0" % Test,
    "org.scalamock"          %% "scalamock-scalatest-support" % "3.5.0"   % Test,
    "com.vladsch.flexmark"   % "flexmark-all"                 % "0.36.8"  % Test
  )
}
