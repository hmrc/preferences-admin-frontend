/*
 * Copyright 2020 HM Revenue & Customs
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

import akka.stream.Materializer
import org.joda.time.{ DateTime, DateTimeZone }
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.mockito.{ ArgumentMatcher, ArgumentMatchers, Mockito }
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Configuration
import play.api.http.Status
import play.api.i18n.{ Lang, MessagesApi }
import play.api.mvc.MessagesControllerComponents
import play.api.test.CSRFTokenHelper._
import play.api.test.Helpers.{ headers, _ }
import play.api.test.{ FakeRequest, Helpers }
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import uk.gov.hmrc.play.audit.model.MergedDataEvent
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.preferencesadminfrontend.config.AppConfig
import uk.gov.hmrc.preferencesadminfrontend.connectors.OptedOut
import uk.gov.hmrc.preferencesadminfrontend.controllers
import uk.gov.hmrc.preferencesadminfrontend.controllers.model.User
import uk.gov.hmrc.preferencesadminfrontend.services._
import uk.gov.hmrc.preferencesadminfrontend.services.model.{ Email, EntityId, Preference, TaxIdentifier }
import uk.gov.hmrc.preferencesadminfrontend.utils.SpecBase

import scala.concurrent.{ ExecutionContext, Future }

class SearchControllerSpec extends UnitSpec with ScalaFutures with GuiceOneAppPerSuite {
  implicit val hc = HeaderCarrier()
  implicit val messagesApi = app.injector.instanceOf[MessagesApi]
  implicit val materializer = app.injector.instanceOf[Materializer]

  implicit lazy val appConfig: AppConfig = app.injector.instanceOf[AppConfig]
  val playConfiguration = app.injector.instanceOf[Configuration]

  "showSearchPage" should {

    "return ok if session is authorised" in new SearchControllerTestCase {
      val result = searchController.showSearchPage("", "")(FakeRequest().withSession(User.sessionKey -> "user").withCSRFToken)

      status(result) shouldBe Status.OK
    }

    "redirect to login page if not authorised" in new SearchControllerTestCase {
      val result = searchController.showSearchPage("", "")(FakeRequest().withSession().withCSRFToken)

      status(result) shouldBe Status.SEE_OTHER
      headers(result) should contain("Location" -> "/paperless/admin")
    }
  }

  "search(taxIdentifier)" should {

    val queryParamsForValidNino = "?name=nino&value=CE067583D"
    val queryParamsForEmailid = "?name=email&value=test@test.com"
    val queryParamsForValidLowercaseNino = "?name=nino&value=ce067583d"
    val queryParamsForInvalidNino = "?name=nino&value=1234567"
    val genericUpdatedAt = Some(new DateTime(2018, 2, 15, 0, 0, DateTimeZone.UTC))
    val taxCreditsUpdatedAt = Some(new DateTime(2018, 2, 15, 0, 0, DateTimeZone.UTC))
    val verifiedOn = Some(new DateTime(2018, 2, 15, 0, 0, DateTimeZone.UTC))

    "return a preference if tax identifier exists" in new SearchControllerTestCase {

      val preference = Preference(
        entityId = Some(EntityId.generate),
        genericPaperless = true,
        genericUpdatedAt = genericUpdatedAt,
        taxCreditsPaperless = true,
        taxCreditsUpdatedAt = taxCreditsUpdatedAt,
        Some(Email("john.doe@digital.hmrc.gov.uk", verified = true, verifiedOn = verifiedOn, language = Some("cy"))),
        Seq(TaxIdentifier("email", "john.doe@digital.hmrc.gov.uk"))
      )
      when(searchServiceMock.searchPreference(any())(any(), any(), any())).thenReturn(Future.successful(List(preference)))

      val result = searchController.search(FakeRequest("GET", queryParamsForValidNino).withSession(User.sessionKey -> "user").withCSRFToken)

      status(result) shouldBe Status.OK
      val body: String = bodyOf(result).futureValue
      body should include("john.doe@digital.hmrc.gov.uk")
      body should include("15 February 2018 AM 12:0:0s")
    }

    "return a preference if email address exists" in new SearchControllerTestCase {
      val preference = Preference(
        entityId = Some(EntityId.generate),
        genericPaperless = true,
        genericUpdatedAt = genericUpdatedAt,
        taxCreditsPaperless = true,
        taxCreditsUpdatedAt = taxCreditsUpdatedAt,
        Some(Email("test@test.com", verified = true, verifiedOn = verifiedOn, language = None)),
        Seq(TaxIdentifier("email", "test@test.com"))
      )
      when(searchServiceMock.searchPreference(any())(any(), any(), any())).thenReturn(Future.successful(List(preference)))

      val result = searchController.search(FakeRequest("GET", queryParamsForEmailid).withSession(User.sessionKey -> "user").withCSRFToken)

      status(result) shouldBe Status.OK
      val body: String = bodyOf(result).futureValue
      body should include("test@test.com")
      body should include("15 February 2018 AM 12:0:0s")
    }

    "return a not found error message if the preference associated with that emailid is not found" in new SearchControllerTestCase {
      when(searchServiceMock.searchPreference(any())(any(), any(), any())).thenReturn(Future.successful(Nil))
      Mockito.reset(auditConnectorMock)
      val result = searchController.search(FakeRequest("GET", queryParamsForEmailid).withSession(User.sessionKey -> "user").withCSRFToken)

      status(result) shouldBe Status.OK
      bodyOf(result).futureValue should include("No paperless preference found for that identifier.")
    }

    "include a hidden form to opt the user out" in new SearchControllerTestCase {

      val preference = Preference(
        entityId = Some(EntityId.generate),
        genericPaperless = true,
        genericUpdatedAt = genericUpdatedAt,
        taxCreditsPaperless = true,
        taxCreditsUpdatedAt = taxCreditsUpdatedAt,
        Some(Email("john.doe@digital.hmrc.gov.uk", verified = true, verifiedOn = verifiedOn, language = Some("en"))),
        Seq(TaxIdentifier("nino", "CE067583D"))
      )
      when(searchServiceMock.searchPreference(any())(any(), any(), any())).thenReturn(Future.successful(List(preference)))

      val result = searchController.search(FakeRequest("GET", queryParamsForValidNino).withSession(User.sessionKey -> "user").withCSRFToken)

      status(result) shouldBe Status.OK
      private val document = Jsoup.parse(bodyOf(result).futureValue)

      document.body().getElementById("confirm").getElementsByTag("form").attr("action") shouldBe
        "/paperless/admin/search/opt-out?taxIdentifierName=nino&taxIdentifierValue=CE067583D"
    }

    "return a not found error message if the preference is not found" in new SearchControllerTestCase {
      when(searchServiceMock.searchPreference(any())(any(), any(), any())).thenReturn(Future.successful(Nil))
      Mockito.reset(auditConnectorMock)
      val result = searchController.search(FakeRequest("GET", queryParamsForValidNino).withSession(User.sessionKey -> "user").withCSRFToken)

      status(result) shouldBe Status.OK
      bodyOf(result).futureValue should include("No paperless preference found for that identifier.")
    }

    "call the search service with an uppercase taxIdentifier if a lowercase taxIdentifier is provided through the Form" in new SearchControllerTestCase {
      val preference = Preference(
        entityId = Some(EntityId.generate),
        genericPaperless = true,
        genericUpdatedAt = genericUpdatedAt,
        taxCreditsPaperless = true,
        taxCreditsUpdatedAt = taxCreditsUpdatedAt,
        Some(Email("john.doe@digital.hmrc.gov.uk", verified = true, verifiedOn = verifiedOn, language = Some("cy"))),
        Seq(TaxIdentifier("nino", "CE067583D"))
      )
      when(searchServiceMock.searchPreference(any())(any(), any(), any())).thenReturn(Future.successful(List(preference)))

      val result = searchController.search(FakeRequest("GET", queryParamsForValidLowercaseNino).withSession(User.sessionKey -> "user").withCSRFToken)

      verify(searchServiceMock, times(1)).searchPreference(ArgumentMatchers.eq(TaxIdentifier("nino", "CE067583D")))(any(), any(), any())
      verify(searchServiceMock, times(0)).searchPreference(ArgumentMatchers.eq(TaxIdentifier("nino", "ce067583d")))(any(), any(), any())
      status(result) shouldBe Status.OK
      private val document = Jsoup.parse(bodyOf(result).futureValue)

      document.body().getElementById("confirm").getElementsByTag("form").attr("action") shouldBe
        "/paperless/admin/search/opt-out?taxIdentifierName=nino&taxIdentifierValue=CE067583D"
    }
  }

  "submit opt out request" should {
    val genericUpdatedAt = Some(new DateTime(2018, 2, 15, 0, 0, DateTimeZone.UTC))
    val taxCreditsUpdatedAt = Some(new DateTime(2018, 2, 15, 0, 0, DateTimeZone.UTC))
    val verifiedOn = Some(new DateTime(2018, 2, 15, 0, 0, DateTimeZone.UTC))
    "redirect to the confirm page" in new SearchControllerTestCase with ScalaFutures {
      val preference = Preference(
        entityId = Some(EntityId.generate),
        genericPaperless = true,
        genericUpdatedAt = genericUpdatedAt,
        taxCreditsPaperless = true,
        taxCreditsUpdatedAt = taxCreditsUpdatedAt,
        Some(Email("john.doe@digital.hmrc.gov.uk", verified = true, verifiedOn = verifiedOn, language = None)),
        Seq()
      )
      when(searchServiceMock.optOut(ArgumentMatchers.eq(TaxIdentifier("nino", "CE067583D")), any())(any(), any(), any()))
        .thenReturn(Future.successful(OptedOut))

      private val request = FakeRequest(Helpers.POST, controllers.routes.SearchController.optOut("nino", "CE067583D").url)
        .withFormUrlEncodedBody("reason" -> "my optOut reason")
        .withSession(User.sessionKey -> "user")

      val result = searchController.optOut("nino", "CE067583D")(request.withCSRFToken)

      status(result) shouldBe SEE_OTHER
      header("Location", result) shouldBe Some(controllers.routes.SearchController.confirmed("nino", "CE067583D").url)
    }
  }
}

trait SearchControllerTestCase extends SpecBase with MockitoSugar {

  import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

  implicit val stubbedMCC: MessagesControllerComponents =
    stubMessagesControllerComponents(
      langs = stubLangs(Seq(Lang("en"))),
      messagesApi = stubMessagesApi(
        messages = Map("en" ->
          Map("error.preference_not_found" -> "No paperless preference found for that identifier.")))
    )
  implicit val ecc: ExecutionContext = stubbedMCC.executionContext

  val searchServiceMock = mock[SearchService]
  when(auditConnectorMock.sendEvent(any())(any(), any())).thenReturn(Future.successful(AuditResult.Success))

  def searchController()(implicit messages: MessagesApi, appConfig: AppConfig) = new SearchController(auditConnectorMock, searchServiceMock, stubbedMCC)

  override def isSimilar(expected: MergedDataEvent): ArgumentMatcher[MergedDataEvent] =
    new ArgumentMatcher[MergedDataEvent]() {
      def matches(t: MergedDataEvent): Boolean = this.matches(t) && {
        t.request.generatedAt == expected.request.generatedAt && t.response.generatedAt == expected.response.generatedAt
      }
    }
}
