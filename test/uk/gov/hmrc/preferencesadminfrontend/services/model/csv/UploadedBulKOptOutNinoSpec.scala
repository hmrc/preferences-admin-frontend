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

package uk.gov.hmrc.preferencesadminfrontend.services.model.csv

import org.scalatestplus.play.PlaySpec

class UploadedBulKOptOutNinoSpec extends PlaySpec {

  "apply when distinctly grouping with preserved order" should {

    "handle empty" in {
      val ninos = UploadedBulKOptOutNinos(List.empty)

      val expectedDistinct = List.empty
      val expectedDuplicateValid = List.empty

      (ninos.distinctErrorOrOptOuts, ninos.duplicateValid) mustBe (expectedDistinct, expectedDuplicateValid)
    }

    "not mark as duplicate when the same value is on the left and right" in {
      val ninos = UploadedBulKOptOutNinos(List(Right("a"), Left("a")))

      val expectedDistinct = List(Right("a"), Left("a"))
      val expectedDuplicateValid = List.empty[String]

      (ninos.distinctErrorOrOptOuts, ninos.duplicateValid) mustBe (expectedDistinct, expectedDuplicateValid)
    }

    "group distinct when there is multiple of the same value on the right and left" in {
      val ninos = UploadedBulKOptOutNinos(List(Right("a"), Left("a"), Right("a")))

      val expectedDistinct = List(Right("a"), Left("a"))
      val expectedDuplicateValid = List("a")

      (ninos.distinctErrorOrOptOuts, ninos.duplicateValid) mustBe (expectedDistinct, expectedDuplicateValid)
    }

    "group distinct when there is multiple of the same value and only add duplicates once" in {
      val ninos = UploadedBulKOptOutNinos(List(Right("a"), Left("a"), Right("a"), Right("a")))

      val expectedDistinct = List(Right("a"), Left("a"))
      val expectedDuplicateValid = List("a")

      (ninos.distinctErrorOrOptOuts, ninos.duplicateValid) mustBe (expectedDistinct, expectedDuplicateValid)
    }

    "group distinct when there are multiple combinations, some with duplicates" in {
      val ninos = UploadedBulKOptOutNinos(
        List(
          Right("a"),
          Left("a"),
          Right("a"),
          Left("b"),
          Right("b"),
          Left("c"),
          Left("c"),
          Right("c"),
          Right("d"),
          Right("d")
        )
      )

      val expectedDistinct = List(Right("a"), Left("a"), Left("b"), Right("b"), Left("c"), Right("c"), Right("d"))
      val expectedDuplicateValid = List("a", "d")

      (ninos.distinctErrorOrOptOuts, ninos.duplicateValid) mustBe (expectedDistinct, expectedDuplicateValid)
    }

  }
}
