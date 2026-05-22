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
import play.api.data.Form
import play.api.i18n.{ Messages, MessagesApi }
import play.api.test.CSRFTokenHelper.*
import play.api.test.FakeRequest
import uk.gov.hmrc.preferencesadminfrontend.config.FrontendAppConfig
import uk.gov.hmrc.preferencesadminfrontend.controllers.model.{ Search, User }
import uk.gov.hmrc.preferencesadminfrontend.services.model.TaxIdentifier
import uk.gov.hmrc.preferencesadminfrontend.views.html.customer_identification

class CustomerIdentificationViewSpec extends PlaySpec with GuiceOneAppPerSuite {
  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  implicit val messages: Messages = messagesApi.preferred(FakeRequest())
  val form: Form[TaxIdentifier] = Search().bind(Map("name" -> "nino", "value" -> "AB123456C"))
  val view: customer_identification = app.injector.instanceOf[customer_identification]
  val appConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]
  def asDocument(html: play.twirl.api.Html): Document = Jsoup.parse(html.toString())

  "Customer Identification View" should {
    "display the search by SaUtr, Nino, ITSA ID for all users" in {
      val request = FakeRequest().withCSRFToken
      val html = view(form)(request, messages, appConfig)
      val doc = asDocument(html)
      doc.title() must include("Customer Identification")
      doc.select("input[value=sautr]").isEmpty mustBe false
      doc.select("input[value=nino]").isEmpty mustBe false
      doc.select("input[value=HMRC-MTD-IT]").isEmpty mustBe false
      doc.select("input[value=email]").isEmpty mustBe true
    }

    "display the search by Email address, when user is an Admin" in {
      val adminRequest = FakeRequest()
        .withSession(
          User.sessionKey -> "admin",
          "isAdmin"       -> "true"
        )
        .withCSRFToken

      val html = view(form)(adminRequest, messages, appConfig)
      val doc = asDocument(html)
      doc.select("input[value=email]").isEmpty mustBe false
    }

    "display the search by Email address, when user is Sols" in {
      val solsRequest = FakeRequest()
        .withSession(
          User.sessionKey -> "sols-user",
          "isSols"        -> "true"
        )
        .withCSRFToken

      val html = view(form)(solsRequest, messages, appConfig)
      val doc = asDocument(html)
      doc.select("input[value=email]").isEmpty mustBe false
    }
  }
}
