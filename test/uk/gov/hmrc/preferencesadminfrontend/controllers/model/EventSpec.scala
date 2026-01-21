
package uk.gov.hmrc.preferencesadminfrontend.controllers.model

import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.{JsError, Json}
import uk.gov.hmrc.preferencesadminfrontend.utils.SpecBase

import java.time.{ZoneId, ZonedDateTime}

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
        """
                {
                  "eventType": "testEvent",
                  "emailAddress": "test@email.com",
                  "timestamp": "2026-01-01T01:01:01Z[Europe/London]",
                  "viaMobileApp": true
                }
                """
      )
      json mustBe expectedJson

    }

    "fail to deserialize when missing fields in " in {

      val invalidJson = Json.parse(
        """
                      {
                        "emailAddress": "test@email.com",
                        "timestamp": "2026-01-01T01:01:01Z[Europe/London]",
                        "viaMobileApp": true
                      }
                      """
      )

      invalidJson.validate[Event] mustBe a[JsError]

    }
  }



}
