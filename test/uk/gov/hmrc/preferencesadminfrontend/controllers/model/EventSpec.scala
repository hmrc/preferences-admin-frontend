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

import java.time.{ ZoneId, ZonedDateTime }

class EventSpec extends PlaySpec with GuiceOneAppPerSuite with SpecBase {

  "Event" must {

    "Serialize correctly in " in {

      val event = Event(
        eventType = "testEvent",
        emailAddress = Some("test@email.com"),
        timestamp = ZonedDateTime.of(2026, 1, 1, 1, 1, 1, 0, ZoneId.of("Europe/London")),
        viaMobileApp = true
      )

      val json = Json.toJson(event)

      val expectedJson = Json.parse(
        """{
          |"eventType": "testEvent",
          |"emailAddress": "test@email.com",
          |"timestamp": "2026-01-01T01:01:01Z[Europe/London]",
          |"viaMobileApp": true
          |}""".stripMargin
      )

      json mustBe expectedJson
    }

    "fail to deserialize when missing fields in " in {

      val invalidJson = Json.parse(
        """{
          |"emailAddress": "test@email.com",
          |"timestamp": "2026-01-01T01:01:01Z[Europe/London]",
          |"viaMobileApp": true
          |}""".stripMargin
      )

      invalidJson.validate[Event] mustBe a[JsError]
    }
  }
}
