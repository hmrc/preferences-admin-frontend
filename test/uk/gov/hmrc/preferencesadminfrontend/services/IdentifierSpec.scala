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

package uk.gov.hmrc.preferencesadminfrontend.services

import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{ JsSuccess, Json }

class IdentifierSpec extends PlaySpec {

  "Identifier" must {

    "deserialize from JSON (Reads)" in {
      // Use the exact field names defined in your case class
      val jsonString = """{
        "itsaId": "ITSA-123",
        "utr": "UTR-456"
      }"""

      val result = Json.parse(jsonString).validate[Identifier]

      result mustBe JsSuccess(Identifier("ITSA-123", "UTR-456"))
    }

    "serialize to JSON (Writes)" in {
      val identifier = Identifier("ITSA-123", "UTR-456")

      val result = Json.toJson(identifier)

      // Verify expected JSON structure
      (result \ "itsaId").as[String] mustBe "ITSA-123"
      (result \ "utr").as[String] mustBe "UTR-456"
    }
  }
}
