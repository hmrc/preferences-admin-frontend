import play.sbt.PlayImport._
import sbt._

object AppDependencies {

  val bootstrapVersion = "7.15.0"
  val compile = Seq(
    ws,
    "uk.gov.hmrc"       %% "bootstrap-frontend-play-28" % bootstrapVersion,
    "uk.gov.hmrc"       %% "play-frontend-hmrc"         % "7.10.0-play-28",
    "uk.gov.hmrc"       %% "auth-client"                % "6.1.0-play-28",
    "uk.gov.hmrc"       %% "play-partials"              % "8.4.0-play-28",
    "com.typesafe.play" %% "play-json-joda"             % "2.8.2",
    "org.typelevel"     %% "cats-core"                  % "2.6.1"
  )

  val test = Seq(
    "uk.gov.hmrc"          %% "bootstrap-test-play-28" % bootstrapVersion % Test,
    "org.scalactic"        %% "scalactic"              % "3.2.16"         % Test,
    "org.pegdown"          % "pegdown"                 % "1.6.0"          % Test,
    "org.jsoup"            % "jsoup"                   % "1.8.1"          % Test,
    "org.mockito"          % "mockito-core"            % "3.9.0"          % Test,
    "org.scalatestplus"    %% "mockito-3-4"            % "3.2.8.0"        % Test,
    "com.vladsch.flexmark" % "flexmark-all"            % "0.36.8"         % Test
  )
}
