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
import play.api.data.validation.{ Invalid, Valid }

class MigrationEntriesSpec extends PlaySpec {

  "sizeConstraint" should {
    "return Invalid for empty string" in {
      MigrationEntries.sizeConstraint("") mustBe Invalid("Can't send empty content")
    }

    "return Invalid for whitespace only string" in {
      MigrationEntries.sizeConstraint("   ") mustBe Invalid("Can't send empty content")
    }

    "return Valid for non empty string" in {
      MigrationEntries.sizeConstraint("abc") mustBe Valid
    }
  }

  "form" should {
    "bind valid data" in {
      val form = MigrationEntries().bind(Map("identifiers" -> "some-id"))
      form.value mustBe Some(MigrationEntries("some-id"))
    }

    "fail to bind empty data" in {
      val form = MigrationEntries().bind(Map("identifiers" -> ""))
      form.errors.head.message mustBe "Can't send empty content"
    }

    "unbind MigrationEntries" in {
      val filled = MigrationEntries().fill(MigrationEntries("test-entry"))
      filled.data mustBe Map("identifiers" -> "test-entry")
    }
  }
}
