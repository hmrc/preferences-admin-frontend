/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.preferencesadminfrontend.controllers.model

import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.{ JsError, Json }
import uk.gov.hmrc.preferencesadminfrontend.utils.SpecBase

class SearchSpec extends PlaySpec with GuiceOneAppPerSuite with SpecBase {

  "MultiSearch" must {

    "Serialize correctly in " in {

      val multiSearch = MultiSearch(
        identifiers = "identifier",
        batch = "batch"
      )

      val json = Json.toJson(multiSearch)

      val expectedJson = Json.parse(
        """
                {
                  "identifiers": "identifier",
                  "batch": "batch"
                }
                """
      )

      json mustBe expectedJson

    }

    "fail to deserialize when missing fields in " in {

      val invalidJson = Json.parse(
        """
                {
                  "batch": "batch"
                }
                """
      )

      invalidJson.validate[MultiSearch] mustBe a[JsError]

    }

  }

}
