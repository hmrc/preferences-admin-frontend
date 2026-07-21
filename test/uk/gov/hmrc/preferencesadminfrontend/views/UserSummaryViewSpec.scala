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

package uk.gov.hmrc.preferencesadminfrontend.views

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{ Messages, MessagesApi }
import play.api.test.FakeRequest
import uk.gov.hmrc.govukfrontend.views.html.components.GovukSummaryList
import uk.gov.hmrc.preferencesadminfrontend.config.FrontendAppConfig
import uk.gov.hmrc.preferencesadminfrontend.controllers.model.{ Event, User }
import uk.gov.hmrc.preferencesadminfrontend.services.model.*
import uk.gov.hmrc.preferencesadminfrontend.views.html.user_summary

import java.time.ZonedDateTime

class UserSummaryViewSpec extends PlaySpec with GuiceOneAppPerSuite {

  implicit val messagesApi: MessagesApi =
    app.injector.instanceOf[MessagesApi]

  implicit val messages: Messages =
    messagesApi.preferred(FakeRequest())

  val summaryList: GovukSummaryList =
    app.injector.instanceOf[GovukSummaryList]

  val appConfig: FrontendAppConfig =
    app.injector.instanceOf[FrontendAppConfig]

  def asDocument(html: play.twirl.api.Html): Document =
    Jsoup.parse(html.toString())

  private val genericUpdatedAt: ZonedDateTime =
    ZonedDateTime.parse("2026-07-20T14:30:45Z")

  private val emailVerifiedOn: ZonedDateTime =
    ZonedDateTime.parse("2026-07-19T10:15:30Z")

  private val eventTimestamp: ZonedDateTime =
    ZonedDateTime.parse("2026-07-18T09:20:10Z")

  private val preferenceEvent =
    Event(
      eventType = "opt-in",
      emailAddress = Some("user@example.com"),
      timestamp = eventTimestamp
    )

  private val preferenceWithAllDetails =
    Preference(
      entityId = Some(EntityId("entityId123")),
      genericPaperless = true,
      genericUpdatedAt = Some(genericUpdatedAt),
      email = Some(
        Email(
          address = "user@example.com",
          verified = true,
          verifiedOn = Some(emailVerifiedOn),
          language = Some("English"),
          hasBounces = false,
          pendingEmail = None
        )
      ),
      taxIdentifiers = Seq(
        TaxIdentifier("nino", "AB123456C"),
        TaxIdentifier("sautr", "1234567890"),
        TaxIdentifier("HMRC-MTD-IT", "12345678901")
      ),
      eventType = "opt-in",
      events = List(preferenceEvent),
      route = PrefRoute.Online
    )

  private val preferenceWithNoOptionalDetails =
    Preference(
      entityId = None,
      genericPaperless = false,
      genericUpdatedAt = None,
      email = None,
      taxIdentifiers = Seq.empty,
      eventType = "opt-in",
      events = List.empty,
      route = PrefRoute.Online
    )

  "User Summary View" should {

    "display the user summary heading" in {
      val html =
        user_summary(
          summaryList,
          List(preferenceWithAllDetails)
        )(FakeRequest(), messages, appConfig)

      val doc = asDocument(html)

      doc.select("#user-summary").isEmpty mustBe false
      doc.select("h2.govuk-heading-m").first().text mustBe "User Summary"
    }

    "display all available preference details" in {
      val html =
        user_summary(
          summaryList,
          List(preferenceWithAllDetails)
        )(FakeRequest(), messages, appConfig)

      val doc = asDocument(html)

      doc.text must include("AB123456C")
      doc.text must include("1234567890")
      doc.text must include("12345678901")
      doc.text must include("entityId123")
      doc.text must include("user@example.com")
      doc.text must include("English")
    }

    "display Not available when tax identifiers are missing" in {
      val html =
        user_summary(
          summaryList,
          List(preferenceWithNoOptionalDetails)
        )(FakeRequest(), messages, appConfig)

      val doc = asDocument(html)

      doc.text must include("Nino")
      doc.text must include("SA Utr")
      doc.text must include("ITSA ID")
      doc.text must include("Not available")
    }

    "display Not available when entity ID is missing" in {
      val preference =
        preferenceWithAllDetails.copy(
          entityId = None
        )

      val html =
        user_summary(
          summaryList,
          List(preference)
        )(FakeRequest(), messages, appConfig)

      val doc = asDocument(html)
      doc.text must include("Entity ID")
      doc.text must include("Not available")
    }

    "display Not available when email is missing" in {
      val preference =
        preferenceWithAllDetails.copy(
          email = None
        )

      val html =
        user_summary(
          summaryList,
          List(preference)
        )(FakeRequest(), messages, appConfig)

      val doc = asDocument(html)
      doc.text must include("Email address")
      doc.text must include("Not available")
    }

    "display the email address" in {
      val html =
        user_summary(
          summaryList,
          List(preferenceWithAllDetails)
        )(FakeRequest(), messages, appConfig)

      val doc = asDocument(html)
      doc.text must include("user@example.com")
    }

    "display the email language when available" in {
      val html =
        user_summary(
          summaryList,
          List(preferenceWithAllDetails)
        )(FakeRequest(), messages, appConfig)

      val doc = asDocument(html)
      doc.text must include("English")
    }

    "display the default English message when email language is unavailable" in {
      val preference =
        preferenceWithAllDetails.copy(
          email = Some(
            Email(
              address = "user@example.com",
              verified = true,
              verifiedOn = Some(emailVerifiedOn),
              language = None,
              hasBounces = false,
              pendingEmail = None
            )
          )
        )

      val html =
        user_summary(
          summaryList,
          List(preference)
        )(FakeRequest(), messages, appConfig)

      val doc = asDocument(html)
      doc.text must include(
        "Not available - defaulted to English"
      )
    }

    "display the verified email date" in {
      val html =
        user_summary(
          summaryList,
          List(preferenceWithAllDetails)
        )(FakeRequest(), messages, appConfig)

      val doc = asDocument(html)
      doc.text must include(
        "19 July 2026 10:15:30 AM"
      )
    }

    "display Not available when email verification date is missing" in {
      val preference =
        preferenceWithAllDetails.copy(
          email = Some(
            Email(
              address = "user@example.com",
              verified = false,
              verifiedOn = None,
              language = Some("English"),
              hasBounces = false,
              pendingEmail = None
            )
          )
        )

      val html =
        user_summary(
          summaryList,
          List(preference)
        )(FakeRequest(), messages, appConfig)

      val doc = asDocument(html)

      doc.text must include("Date time Email Verified")
      doc.text must include("Not available")
    }

    "display Opted In to paperless for generic terms and conditions" in {
      val preference =
        preferenceWithAllDetails.copy(
          eventType = "opt-in",
          genericPaperless = true,
          genericUpdatedAt = Some(genericUpdatedAt)
        )

      val html =
        user_summary(
          summaryList,
          List(preference)
        )(FakeRequest(), messages, appConfig)

      val doc = asDocument(html)

      doc.text must include(
        "Opted In to paperless for generic terms and conditions"
      )
    }

    "display Opted Out of paperless for generic terms and conditions" in {
      val preference =
        preferenceWithAllDetails.copy(
          eventType = "opt-in",
          genericPaperless = false,
          genericUpdatedAt = Some(genericUpdatedAt)
        )

      val html =
        user_summary(
          summaryList,
          List(preference)
        )(FakeRequest(), messages, appConfig)

      val doc = asDocument(html)

      doc.text must include(
        "Opted Out of paperless for generic terms and conditions"
      )
    }

    "display Opted In to paperless when generic paperless has never been updated" in {
      val preference =
        preferenceWithAllDetails.copy(
          eventType = "opt-in",
          genericPaperless = false,
          genericUpdatedAt = None
        )

      val html =
        user_summary(
          summaryList,
          List(preference)
        )(FakeRequest(), messages, appConfig)

      val doc = asDocument(html)

      doc.text must include(
        "Opted In to paperless for generic terms and conditions"
      )
    }

    "display Re Opted In when event type is re-opt-in" in {
      val preference =
        preferenceWithAllDetails.copy(
          eventType = "re-opt-in",
          genericPaperless = true,
          genericUpdatedAt = Some(genericUpdatedAt)
        )

      val html =
        user_summary(
          summaryList,
          List(preference)
        )(FakeRequest(), messages, appConfig)

      val doc = asDocument(html)

      doc.text must include("Re Opted In")
    }

    "display the generic updated date" in {
      val html =
        user_summary(
          summaryList,
          List(preferenceWithAllDetails)
        )(FakeRequest(), messages, appConfig)

      val doc = asDocument(html)
      doc.text must include(
        "20 July 2026 02:30:45 pm"
      )
    }

    "display Optin/Optout route for an admin user" in {
      val adminRequest =
        FakeRequest()
          .withSession(
            User.sessionKey -> "admin",
            "isAdmin"       -> "true"
          )

      val html =
        user_summary(
          summaryList,
          List(preferenceWithAllDetails)
        )(adminRequest, messages, appConfig)

      val doc = asDocument(html)
      doc.text must include("Optin/Optout route")
      doc.text must include("Online")
    }

    "display Optin/Optout route for a Sols user" in {
      val solsRequest =
        FakeRequest()
          .withSession(
            User.sessionKey -> "sols-user",
            "isSols"        -> "true"
          )

      val html =
        user_summary(
          summaryList,
          List(preferenceWithAllDetails)
        )(solsRequest, messages, appConfig)

      val doc = asDocument(html)
      doc.text must include("Optin/Optout route")
      doc.text must include("Online")
    }

    "not display Optin/Optout route for a normal user" in {
      val html =
        user_summary(
          summaryList,
          List(preferenceWithAllDetails)
        )(FakeRequest(), messages, appConfig)

      val doc = asDocument(html)
      doc.text must not include "Optin/Optout route"
    }

    "display MobileApp as the route for a mobile app preference" in {
      val preference =
        preferenceWithAllDetails.copy(
          route = PrefRoute.MobileApp
        )

      val adminRequest =
        FakeRequest()
          .withSession(
            User.sessionKey -> "admin",
            "isAdmin"       -> "true"
          )

      val html =
        user_summary(
          summaryList,
          List(preference)
        )(adminRequest, messages, appConfig)

      val doc = asDocument(html)
      doc.text must include("Optin/Optout route")
      doc.text must include("MobileApp")
    }

    "display preference history for an admin user" in {
      val adminRequest =
        FakeRequest()
          .withSession(
            User.sessionKey -> "admin",
            "isAdmin"       -> "true"
          )

      val html =
        user_summary(
          summaryList,
          List(preferenceWithAllDetails)
        )(adminRequest, messages, appConfig)

      val doc = asDocument(html)

      doc.text must include("Preference History")
      doc.text must include("opt-in")
      doc.text must include("user@example.com")
      doc.text must include(
        "18 July 2026 09:20:10 AM"
      )
    }

    "display preference history for a Sols user" in {
      val solsRequest =
        FakeRequest()
          .withSession(
            User.sessionKey -> "sols-user",
            "isSols"        -> "true"
          )

      val html =
        user_summary(
          summaryList,
          List(preferenceWithAllDetails)
        )(solsRequest, messages, appConfig)

      val doc = asDocument(html)

      doc.text must include("Preference History")
      doc.text must include("opt-in")
      doc.text must include("user@example.com")
    }

    "not display preference history for a normal user" in {
      val html =
        user_summary(
          summaryList,
          List(preferenceWithAllDetails)
        )(FakeRequest(), messages, appConfig)

      val doc = asDocument(html)
      doc.text must not include "Preference History"
    }

    "display an empty email address when an event has no email address" in {
      val preference =
        preferenceWithAllDetails.copy(
          events = List(
            Event(
              eventType = "opt-in",
              emailAddress = None,
              timestamp = eventTimestamp
            )
          )
        )

      val adminRequest =
        FakeRequest()
          .withSession(
            User.sessionKey -> "admin",
            "isAdmin"       -> "true"
          )

      val html =
        user_summary(
          summaryList,
          List(preference)
        )(adminRequest, messages, appConfig)

      val doc = asDocument(html)
      doc.text must include("Preference History")
      doc.text must include("opt-in")
    }

    "display details for multiple preferences" in {
      val secondPreference =
        preferenceWithAllDetails.copy(
          entityId = Some(EntityId("entityId456")),
          email = Some(
            Email(
              address = "second@example.com",
              verified = true,
              verifiedOn = Some(emailVerifiedOn),
              language = Some("Welsh"),
              hasBounces = false,
              pendingEmail = None
            )
          )
        )

      val html =
        user_summary(
          summaryList,
          List(
            preferenceWithAllDetails,
            secondPreference
          )
        )(FakeRequest(), messages, appConfig)

      val doc = asDocument(html)
      doc.text must include("entityId123")
      doc.text must include("entityId456")
      doc.text must include("user@example.com")
      doc.text must include("second@example.com")
      doc.text must include("English")
      doc.text must include("Welsh")
    }
  }
}
