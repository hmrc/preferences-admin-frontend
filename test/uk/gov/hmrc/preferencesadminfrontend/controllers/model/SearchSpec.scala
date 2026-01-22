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
      json.as[MultiSearch] mustBe multiSearch

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

  "SearchNinos form" must {
    "bind successfully when valid data is provided" in {
      val formData = Map(
        "search-ninos" -> "AA123456A, BB123456B",
        "batch"        -> "batch-001"
      )

      val form = SearchNinos().bind(formData)

      form.errors mustBe empty
      form.value mustBe Some(MultiSearch("AA123456A, BB123456B", "batch-001"))
    }

    "fail to bind when search-ninos is missing or empty" in {
      val formData = Map(
        "search-ninos" -> "",
        "batch"        -> "batch-001"
      )

      val form = SearchNinos().bind(formData)

      form.errors.size mustBe 1
      form.errors.head.key mustBe "search-ninos"
      form.errors.head.message mustBe "error.required"
    }

    "unbind correctly and fill form from model" in {
      val model = MultiSearch("AA123456A", "batch-001")
      val form = SearchNinos().fill(model)

      form.data must contain("search-ninos" -> "AA123456A")
      form.data must contain("batch" -> "batch-001")
    }
  }

}
