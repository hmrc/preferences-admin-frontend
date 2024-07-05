import sbt._

object AppDependencies {

  val bootstrapVersion = "9.0.0"
  
  val compile = Seq(
    "uk.gov.hmrc"   %% "bootstrap-frontend-play-30" % bootstrapVersion,
    "uk.gov.hmrc"   %% "play-frontend-hmrc-play-30" % "10.3.0",
    "uk.gov.hmrc"   %% "play-partials-play-30"      % "10.0.0",
    "org.typelevel" %% "cats-core"                  % "2.6.1"
  )

  val test = Seq(
    "uk.gov.hmrc"   %% "bootstrap-test-play-30" % bootstrapVersion % Test,
    "org.scalactic" %% "scalactic"              % "3.2.16"         % Test,
    "org.jsoup"      % "jsoup"                  % "1.8.1"          % Test
  )
}
