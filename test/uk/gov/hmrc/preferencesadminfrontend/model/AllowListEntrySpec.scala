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

package uk.gov.hmrc.preferencesadminfrontend.model

import org.scalatestplus.play.PlaySpec

class AllowListEntrySpec extends PlaySpec {

  val form = AllowlistEntry()

  "AllowlistEntry form" should {
    "bind successfully with valid data" in {
      val data = Map("formId" -> "ID123", "reasonText" -> "Valid Reason 123")
      form.bind(data).get mustBe AllowlistEntry("ID123", "Valid Reason 123")
    }

    "fail to bind" when {
      "formId is missing" in {
        val result = form.bind(Map("formId" -> "", "reasonText" -> "Reason"))
        result.errors.head.message mustBe "A form ID is required"
      }

      "reasonText is empty" in {
        val result = form.bind(Map("formId" -> "ID", "reasonText" -> ""))
        result.errors.head.message mustBe "A reason is required"
      }

      "reasonText contains invalid characters" in {
        val result = form.bind(Map("formId" -> "ID", "reasonText" -> "Reason <script>"))
        result.errors.head.message mustBe "Invalid characters entered"
      }
    }
  }
}
