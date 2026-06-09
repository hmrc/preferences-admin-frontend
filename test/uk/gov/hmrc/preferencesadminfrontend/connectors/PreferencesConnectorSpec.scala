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

package uk.gov.hmrc.preferencesadminfrontend.connectors

import com.github.tomakehurst.wiremock.client.WireMock
import play.api.http.Status
import play.api.libs.json.Json
import uk.gov.hmrc.http.{ HeaderCarrier, UpstreamErrorResponse }
import uk.gov.hmrc.preferencesadminfrontend.controllers.model
import uk.gov.hmrc.preferencesadminfrontend.controllers.model.EmailRequest
import uk.gov.hmrc.preferencesadminfrontend.utils.ConnectorBaseSpec

import java.time.ZonedDateTime
import scala.concurrent.ExecutionContext.Implicits.global

class PreferencesConnectorSpec extends ConnectorBaseSpec(PreferencesConnector.configKey) {

  trait TestCase {
    implicit val hc: HeaderCarrier = HeaderCarrier()
    val preferencesConnector: PreferencesConnector = app.injector.instanceOf[PreferencesConnector]
  }

  "getPreferencesByEmail" must {
    val email = "test@example.com"

    def stubGetPreferencesByEmail(email: String, statusCode: Int, response: String): Unit =
      wireMockServer.stubFor(
        WireMock
          .post(WireMock.urlPathEqualTo(s"/preferences/find-by-email"))
          .withRequestBody(WireMock.equalToJson(Json.toJson(EmailRequest(email)).toString))
          .willReturn(
            WireMock
              .aResponse()
              .withStatus(statusCode)
              .withBody(response)
          )
      )

    "return a list of PreferenceDetails on success" in new TestCase {
      val mockResponseList: Seq[PreferenceDetails] = List(
        PreferenceDetails(genericPaperless = true, genericUpdatedAt = None, isPaperless = None, email = None)
      )

      val mockResponseListJson: String =
        """[
          | {
          |    "termsAndConditions" : {
          |      "generic" : {
          |         "accepted" : true
          |      }
          |    }
          | }
          |]
          |""".stripMargin.trim

      stubGetPreferencesByEmail(email, 200, mockResponseListJson)

      val result: Seq[PreferenceDetails] = preferencesConnector.getPreferencesByEmail(email).futureValue

      verifyNoUnexpectedInteractions
      result mustBe mockResponseList
    }

    "return an empty list if the service returns a BAD_REQUEST" in new TestCase {
      stubGetPreferencesByEmail(email, Status.BAD_REQUEST, "")
      val result: Seq[PreferenceDetails] = preferencesConnector.getPreferencesByEmail(email).futureValue

      result mustBe Nil
      verifyNoUnexpectedInteractions
    }

    "returns an error if the server returns a non success non BAD_REQUEST response" in new TestCase {
      stubGetPreferencesByEmail(email, Status.INTERNAL_SERVER_ERROR, "")
      val throwable = preferencesConnector.getPreferencesByEmail(email).failed.futureValue

      verifyNoUnexpectedInteractions
      throwable mustBe a[UpstreamErrorResponse]
    }

  }

  "getPreferencesEvents" must {
    val entityId = "some-entity-id"
    def stubGetPreferencesEvents(entityId: String, statusCode: Int, response: String): Unit =
      wireMockServer.stubFor(
        WireMock
          .get(WireMock.urlEqualTo(s"/preferences-admin/events/$entityId"))
          .willReturn(
            WireMock
              .aResponse()
              .withStatus(statusCode)
              .withBody(response)
          )
      )

    "return a list of events when successful" in new TestCase {
      val zonedDateTime: ZonedDateTime = ZonedDateTime.parse("2026-06-25T16:10:33.203539+01:00[Europe/London]")
      val mockEvents: Seq[model.Event] = List(
        uk.gov.hmrc.preferencesadminfrontend.controllers.model
          .Event(eventType = "testEvent", emailAddress = None, timestamp = zonedDateTime, viaMobileApp = true)
      )

      val eventsJson = s"""[
                          | {
                          |   "eventType": "testEvent",
                          |   "timestamp": "${zonedDateTime.toString}",
                          |   "viaMobileApp": true
                          | }
                          |]""".stripMargin

      stubGetPreferencesEvents(entityId, 200, eventsJson)

      val result = preferencesConnector.getPreferencesEvents(entityId).futureValue

      verifyNoUnexpectedInteractions
      result mustBe mockEvents
    }

    "return an error on a non success status code" in new TestCase {
      stubGetPreferencesEvents(entityId, Status.BAD_REQUEST, "[]")
      val throwable = preferencesConnector.getPreferencesEvents(entityId).failed.futureValue

      verifyNoUnexpectedInteractions
      throwable mustBe a[UpstreamErrorResponse]
    }
  }

}
