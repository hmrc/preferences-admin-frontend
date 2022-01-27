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

package uk.gov.hmrc.preferencesadminfrontend.controllers

import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import uk.gov.hmrc.preferencesadminfrontend.services.Identifier

class MessageControllerspec extends PlaySpec with GuiceOneAppPerSuite {
  val controller = app.injector.instanceOf[MessageController]

  "parseEntries function" must {
    "convert to Identifier" in {
      val entries = "XFIT00000004173,123456789\nXFIT00000004173,123456789\nXFIT00000004173,123456789"
      controller.parse(entries) mustBe (Right(
        List(Identifier("XFIT00000004173", "123456789"), Identifier("XFIT00000004173", "123456789"), Identifier("XFIT00000004173", "123456789"))))
    }

    "convert to 1 Identifier" in {
      val entries = "XFIT00000004173,123456789"
      controller.parse(entries) mustBe Right(List(Identifier("XFIT00000004173", "123456789")))
    }

    "handle white space" in {
      val entries = "XFIT00000004173,123456789\nXFIT00000004173,"
      controller.parse(entries) mustBe (Left("whitespace"))
    }

    "error if itsaId is empty" in {
      val entries = "XFIT00000004173,123456789\n,123456789"
      controller.parse(entries) mustBe (Left("ItsaId is missing for 123456789"))
    }
    "error for empty line" in {
      val entries = "XFIT00000004173,123456789\n,"
      controller.parse(entries) mustBe (Left("empty line"))
    }
    "error if 3 values is supplied" in {
      val entries = "XFIT00000004173,123456789,whatever"
      controller.parse(entries) mustBe (Left("only itsaId and utr is required"))
    }

  }

}
