import sbt._

object AppDependencies {

  val bootstrapVersion = "8.5.0"
  val compile = Seq(
    "uk.gov.hmrc"   %% "bootstrap-frontend-play-30" % bootstrapVersion,
    "uk.gov.hmrc"   %% "play-frontend-hmrc-play-30" % "9.0.0",
    "uk.gov.hmrc"   %% "play-partials-play-30"      % "9.1.0",
    "org.typelevel" %% "cats-core"                  % "2.6.1"
  )

  val test = Seq(
    "uk.gov.hmrc"   %% "bootstrap-test-play-30" % bootstrapVersion % Test,
    "org.scalactic" %% "scalactic"              % "3.2.16"         % Test,
    "org.jsoup"      % "jsoup"                  % "1.8.1"          % Test,
    "org.mockito"    % "mockito-core"           % "3.9.0"          % Test
//    "org.scalatestplus" %% "mockito-3-4"            % "3.2.8.0"        % Test
  )
}
