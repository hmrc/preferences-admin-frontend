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

package uk.gov.hmrc.preferencesadminfrontend.services.model

import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{ JsError, JsString, Json }

import java.time.{ ZoneId, ZonedDateTime }

class PreferencesSpec extends PlaySpec {

  "PrefRoute" must {
    "create correct enum from boolean" in {
      PrefRoute.from(viaMobile = true) mustBe PrefRoute.MobileApp
      PrefRoute.from(viaMobile = false) mustBe PrefRoute.Online
    }

    "read from JSON string" in {
      JsString("MobileApp").as[PrefRoute] mustBe PrefRoute.MobileApp
      JsString("Online").as[PrefRoute] mustBe PrefRoute.Online
    }

    "fail to read invalid JSON string" in {
      JsString("Invalid").validate[PrefRoute] mustBe a[JsError]
    }

    "write to JSON string" in {
      Json.toJson(PrefRoute.MobileApp) mustBe JsString("MobileApp")
      Json.toJson(PrefRoute.Online) mustBe JsString("Online")
    }
  }

  "Preference" must {
    val fixedTime = ZonedDateTime.of(2023, 1, 1, 12, 0, 0, 0, ZoneId.of("UTC"))
    val epochMillis = fixedTime.toInstant.toEpochMilli
    val preference = Preference(
      entityId = None,
      genericPaperless = true,
      genericUpdatedAt = Some(fixedTime),
      email = None,
      taxIdentifiers = Seq.empty,
      eventType = "test-event",
      events = List.empty,
      route = PrefRoute.Online
    )

    "serialize genericUpdatedAt as Epoch Millis (custom writes)" in {
      val json = Json.toJson(preference)

      (json \ "genericUpdatedAt").as[Long] mustBe epochMillis
      (json \ "route").as[String] mustBe "Online"
    }

    "deserialize genericUpdatedAt from ISO-8601 String (default reads)" in {
      val jsonString =
        s"""{
           |  "genericPaperless": true,
           |  "genericUpdatedAt": "2023-01-01T12:00:00Z",
           |  "taxIdentifiers": [],
           |  "eventType": "test-event",
           |  "events": [],
           |  "route": "MobileApp"
           |}""".stripMargin

      val result = Json.parse(jsonString).as[Preference]

      result.genericUpdatedAt.value.toInstant mustBe fixedTime.toInstant
      result.route mustBe PrefRoute.MobileApp
    }
  }
}
