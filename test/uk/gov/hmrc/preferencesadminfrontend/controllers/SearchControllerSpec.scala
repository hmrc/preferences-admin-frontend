/*
 * Copyright 2023 HM Revenue & Customs
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

import org.apache.pekko.stream.Materializer
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{ times, verify, when }
import org.mockito.{ ArgumentMatcher, ArgumentMatchers }
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Configuration
import play.api.http.Status
import play.api.i18n.MessagesApi
import play.api.mvc.AnyContentAsEmpty
import play.api.test.CSRFTokenHelper._
import play.api.test.Helpers.{ headers, _ }
import play.api.test.{ FakeRequest, Helpers }
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.model.MergedDataEvent
import uk.gov.hmrc.preferencesadminfrontend.config.AppConfig
import uk.gov.hmrc.preferencesadminfrontend.connectors.{ AlreadyOptedOut, OptedOut }
import uk.gov.hmrc.preferencesadminfrontend.controllers
import uk.gov.hmrc.preferencesadminfrontend.controllers.model.User
import uk.gov.hmrc.preferencesadminfrontend.services._
import uk.gov.hmrc.preferencesadminfrontend.services.model.{ Email, EntityId, Preference, TaxIdentifier }
import uk.gov.hmrc.preferencesadminfrontend.utils.SpecBase
import uk.gov.hmrc.preferencesadminfrontend.views.html.{ confirmed, customer_identification, failed, user_opt_out }

import java.time.{ ZoneOffset, ZonedDateTime }
import scala.concurrent.{ ExecutionContext, Future }

class SearchControllerSpec extends PlaySpec with ScalaFutures with GuiceOneAppPerSuite {
  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  implicit val materializer: Materializer = app.injector.instanceOf[Materializer]

  val playConfiguration = app.injector.instanceOf[Configuration]

  val genericUpdatedAt = Some(ZonedDateTime.of(2018, 2, 15, 0, 0, 0, 0, ZoneOffset.UTC))
  val verifiedOn = genericUpdatedAt

  "showSearchPage" should {

    "return ok if session is authorised" in new SearchControllerTestCase {
      val result = searchController.showSearchPage()(FakeRequest().withSession(User.sessionKey -> "user").withCSRFToken)

      status(result) mustBe Status.OK
    }

    "redirect to login page if not authorised" in new SearchControllerTestCase {
      val result = searchController.showSearchPage()(FakeRequest().withSession().withCSRFToken)

      status(result) mustBe Status.SEE_OTHER
      headers(result) must contain("Location" -> "/paperless/admin")
    }
  }

  "search(taxIdentifier)" should {

    "return a preference if tax identifier exists" in new SearchControllerTestCase {

      val preference = Preference(
        entityId = Some(EntityId.generate()),
        genericPaperless = true,
        genericUpdatedAt = genericUpdatedAt,
        Some(Email("john.doe@digital.hmrc.gov.uk", verified = true, verifiedOn = verifiedOn, language = Some("cy"), hasBounces = false, None)),
        Seq(TaxIdentifier("email", "john.doe@digital.hmrc.gov.uk"))
      )
      when(searchServiceMock.searchPreference(any[TaxIdentifier])(any[User], any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(List(preference)))

      val postRequest = FakeRequest("POST", "/search/q")
        .withFormUrlEncodedBody(Seq(("name", "nino"), ("value", "CE067583D")): _*)

      val result = searchController.search()(postRequest.withSession(User.sessionKey -> "user").withCSRFToken)

      status(result) mustBe Status.OK
      val body: String = contentAsString(result)
      body must include("john.doe@digital.hmrc.gov.uk")
      body must include("15 February 2018 12:00:00 AM")
    }

    "return a preference if email address exists" in new SearchControllerTestCase {
      val preference = Preference(
        entityId = Some(EntityId.generate()),
        genericPaperless = true,
        genericUpdatedAt = genericUpdatedAt,
        Some(Email("test@test.com", verified = true, verifiedOn = verifiedOn, language = None, false, None)),
        Seq(TaxIdentifier("email", "test@test.com"))
      )
      when(searchServiceMock.searchPreference(any[TaxIdentifier])(any[User], any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(List(preference)))

      val postRequest = FakeRequest("POST", "/search/q")
        .withFormUrlEncodedBody(Seq(("name", "email"), ("value", "test@test.com")): _*)

      val result = searchController.search()(postRequest.withSession(User.sessionKey -> "user").withCSRFToken)

      status(result) mustBe Status.OK
      val body: String = contentAsString(result)
      body must include("test@test.com")
      body must include("15 February 2018 12:00:00 AM")
    }

    "return a not found error message if the preference associated with that email is not found" in new SearchControllerTestCase {
      when(searchServiceMock.searchPreference(any[TaxIdentifier])(any[User], any[HeaderCarrier], any[ExecutionContext])).thenReturn(Future.successful(Nil))

      val postRequest = FakeRequest("POST", "/search/q")
        .withFormUrlEncodedBody(Seq(("name", "email"), ("value", "test@test.com")): _*)

      val result = searchController.search()(postRequest.withSession(User.sessionKey -> "user").withCSRFToken)

      status(result) mustBe Status.OK
      val body: String = contentAsString(result)
      body must include("No paperless preference found for that identifier.")
    }

    "include a hidden form to opt the user out" in new SearchControllerTestCase {

      val preference = Preference(
        entityId = Some(EntityId.generate()),
        genericPaperless = true,
        genericUpdatedAt = genericUpdatedAt,
        Some(Email("john.doe@digital.hmrc.gov.uk", verified = true, verifiedOn = verifiedOn, language = Some("en"), hasBounces = false, None)),
        Seq(TaxIdentifier("nino", "CE067583D"))
      )
      when(searchServiceMock.searchPreference(any[TaxIdentifier])(any[User], any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(List(preference)))

      val postRequest = FakeRequest("POST", "/search/q")
        .withFormUrlEncodedBody(Seq(("name", "nino"), ("value", "CE067583D")): _*)

      val result = searchController.search()(postRequest.withSession(User.sessionKey -> "user").withCSRFToken)

      status(result) mustBe Status.OK
      private val document = Jsoup.parse(contentAsString(result))
      document.body().getElementById("confirm").getElementsByTag("form").attr("action") mustBe
        "/paperless/admin/search/opt-out"
    }

    "return a not found error message if the preference is not found" in new SearchControllerTestCase {
      when(searchServiceMock.searchPreference(any[TaxIdentifier])(any[User], any[HeaderCarrier], any[ExecutionContext])).thenReturn(Future.successful(Nil))

      val postRequest = FakeRequest("POST", "/search/q")
        .withFormUrlEncodedBody(Seq(("name", "nino"), ("value", "CE067583D")): _*)

      val result = searchController.search()(postRequest.withSession(User.sessionKey -> "user").withCSRFToken)

      status(result) mustBe Status.OK
      contentAsString(result) must include("No paperless preference found for that identifier.")
    }

    "call the search service with an uppercase taxIdentifier if a lowercase taxIdentifier is provided through the Form" in new SearchControllerTestCase {
      val preference = Preference(
        entityId = Some(EntityId.generate()),
        genericPaperless = true,
        genericUpdatedAt = genericUpdatedAt,
        Some(Email("john.doe@digital.hmrc.gov.uk", verified = true, verifiedOn = verifiedOn, language = Some("cy"), false, None)),
        Seq(TaxIdentifier("nino", "CE067583D"))
      )
      when(searchServiceMock.searchPreference(any[TaxIdentifier])(any[User], any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(List(preference)))

      val postRequest = FakeRequest("POST", "/search/q")
        .withFormUrlEncodedBody(Seq(("name", "nino"), ("value", "ce067583d")): _*)

      val result = searchController.search()(postRequest.withSession(User.sessionKey -> "user").withCSRFToken)

      verify(searchServiceMock, times(1))
        .searchPreference(ArgumentMatchers.eq(TaxIdentifier("nino", "CE067583D")))(any[User], any[HeaderCarrier], any[ExecutionContext])
      verify(searchServiceMock, times(0))
        .searchPreference(ArgumentMatchers.eq(TaxIdentifier("nino", "ce067583d")))(any[User], any[HeaderCarrier], any[ExecutionContext])
      status(result) mustBe Status.OK
      private val document = Jsoup.parse(contentAsString(result))
      document.body().getElementById("confirm").getElementsByTag("form").attr("action") mustBe
        "/paperless/admin/search/opt-out"
    }
  }

  "submit opt out request" should {

    "show the search confirmed page" in new SearchControllerTestCase with ScalaFutures {

      val preference: Preference = Preference(
        entityId = Some(EntityId.generate()),
        genericPaperless = true,
        genericUpdatedAt = genericUpdatedAt,
        Some(Email("john.doe@digital.hmrc.gov.uk", verified = true, verifiedOn = verifiedOn, language = Some("cy"), hasBounces = false, None)),
        Seq(TaxIdentifier("email", "john.doe@digital.hmrc.gov.uk"))
      )

      when(searchServiceMock.optOut(ArgumentMatchers.eq(TaxIdentifier("nino", "CE067583D")), any[String])(any[User], any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(OptedOut))

      when(searchServiceMock.getPreference(ArgumentMatchers.eq(TaxIdentifier("nino", "CE067583D")))(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(List(preference)))

      private val request = FakeRequest(Helpers.POST, controllers.routes.SearchController.optOut().url)
        .withFormUrlEncodedBody("reason" -> "my optOut reason", "identifierName" -> "nino", "identifierValue" -> "CE067583D")
        .withSession(User.sessionKey -> "user")

      val result = searchController.optOut()(request.withCSRFToken)

      status(result) mustBe OK
      val body: String = contentAsString(result)
      body must include("john.doe@digital.hmrc.gov.uk")
      body must include("15 February 2018 12:00:00 AM")
    }
  }

  "search confirmed or failed" should {

    "show the search confirmed page" in new SearchControllerTestCase with ScalaFutures {

      val preference: Preference = Preference(
        entityId = Some(EntityId.generate()),
        genericPaperless = true,
        genericUpdatedAt = genericUpdatedAt,
        Some(Email("john.doe@digital.hmrc.gov.uk", verified = true, verifiedOn = verifiedOn, language = Some("cy"), hasBounces = false, None)),
        Seq(TaxIdentifier("email", "john.doe@digital.hmrc.gov.uk"))
      )
      when(searchServiceMock.getPreference(ArgumentMatchers.eq(TaxIdentifier("nino", "CE067583D")))(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(List(preference)))

      val taxIdentifier = TaxIdentifier("nino", "CE067583D")
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(Helpers.GET, "/").withSession(User.sessionKey -> "user")

      val result = searchController.searchConfirmed(taxIdentifier)

      status(result) mustBe Status.OK
      val body: String = contentAsString(result)
      body must include("john.doe@digital.hmrc.gov.uk")
      body must include("15 February 2018 12:00:00 AM")
    }

    "show the search failed page" in new SearchControllerTestCase with ScalaFutures {

      val preference: Preference = Preference(
        entityId = Some(EntityId.generate()),
        genericPaperless = true,
        genericUpdatedAt = genericUpdatedAt,
        Some(Email("john.doe@digital.hmrc.gov.uk", verified = true, verifiedOn = verifiedOn, language = Some("cy"), hasBounces = false, None)),
        Seq(TaxIdentifier("email", "john.doe@digital.hmrc.gov.uk"))
      )
      when(searchServiceMock.getPreference(ArgumentMatchers.eq(TaxIdentifier("nino", "CE067583D")))(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(List(preference)))

      val taxIdentifier = TaxIdentifier("nino", "CE067583D")
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(Helpers.GET, "/").withSession(User.sessionKey -> "user")

      val result = searchController.searchFailed(taxIdentifier, AlreadyOptedOut.errorCode)

      status(result) mustBe Status.OK
      val body: String = contentAsString(result)
      body must include("john.doe@digital.hmrc.gov.uk")
      body must include("15 February 2018 12:00:00 AM")
    }
  }

  class SearchControllerTestCase extends SpecBase {

    implicit val ecc: ExecutionContext = stubbedMCC.executionContext
    implicit val appConfig: AppConfig = app.injector.instanceOf[AppConfig]
    val authorisedAction: AuthorisedAction = app.injector.instanceOf[AuthorisedAction]
    val confirmedView: confirmed = app.injector.instanceOf[confirmed]
    val customerIdentificationView: customer_identification = app.injector.instanceOf[customer_identification]
    val failedView: failed = app.injector.instanceOf[failed]
    val userOptOutView: user_opt_out = app.injector.instanceOf[user_opt_out]
    val searchServiceMock: SearchService = mock[SearchService]

    val searchController: SearchController =
      new SearchController(
        authorisedAction,
        searchServiceMock,
        stubbedMCC,
        confirmedView,
        customerIdentificationView,
        failedView,
        userOptOutView
      )

    override def isSimilar(expected: MergedDataEvent): ArgumentMatcher[MergedDataEvent] =
      new ArgumentMatcher[MergedDataEvent]() {
        def matches(t: MergedDataEvent): Boolean = this.matches(t) && {
          t.request.generatedAt == expected.request.generatedAt && t.response.generatedAt == expected.response.generatedAt
        }
      }
  }
}
