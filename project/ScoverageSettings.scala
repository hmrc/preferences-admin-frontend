/*
 * Copyright 2022 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import sbt.Keys.parallelExecution
import sbt.{Def, *}
import scoverage.ScoverageKeys

object ScoverageSettings {
  def apply(): Seq[Def.Setting[? >: String & Double & Boolean]] =
    Seq(
      // Semicolon-separated list of regexes matching classes to exclude
      ScoverageKeys.coverageExcludedPackages := "<empty>;.*Reverse.*;.*(config|testonly).*;.*(BuildInfo|Routes).*;.*\\$anon.*",
      ScoverageKeys.coverageMinimumStmtTotal := 73.00,
      ScoverageKeys.coverageFailOnMinimum := true,
      ScoverageKeys.coverageHighlighting := true,
      ConfigKey.configurationToKey(Test) / parallelExecution := false
    )
}
