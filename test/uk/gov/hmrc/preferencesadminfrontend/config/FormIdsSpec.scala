/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.preferencesadminfrontend.config

import org.scalatestplus.play.PlaySpec

class FormIdsSpec extends PlaySpec {

  "configList" should {

    "contain ITSA form ids" in {
      val formIdsConfig: Seq[String] = FormIds.configList

      val itsaFormIds: List[String] =
        List(
          "LPP1A_ITSA",
          "LPP1A_ITSA_cy",
          "LPP1B_ITSA",
          "LPP1B_ITSA_cy",
          "LPP2_ITSA",
          "LPP2_ITSA_cy",
          "LPP4_ITSA",
          "LPP4_ITSA_cy",
          "PAR1_ITSA",
          "PAR1_ITSA_cy",
          "ITSAORM1",
          "ITSAORM1_cy"
        )

      itsaFormIds.foreach { itsaFormId =>
        assert(formIdsConfig.contains(itsaFormId))
      }
    }
  }
}
