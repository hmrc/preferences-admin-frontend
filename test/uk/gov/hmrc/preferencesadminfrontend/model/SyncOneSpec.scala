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

class SyncOneSpec extends PlaySpec {

  "SyncOne.apply" must {
    "bind valid data correctly" in {
      val formData = Map("entries" -> "test-entry")
      val boundForm = SyncOne().bind(formData)

      boundForm.value mustBe Some(Sync("test-entry"))
    }

    "unbind data correctly" in {
      val sync = Sync("test-entry")
      val filledForm = SyncOne().fill(sync)

      filledForm.data mustBe Map("entries" -> "test-entry")
    }
  }
}
