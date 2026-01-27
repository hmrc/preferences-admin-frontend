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
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.*
import play.api.i18n.MessagesApi
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import play.api.{ Application, inject }
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.preferencesadminfrontend.controllers.model.User
import uk.gov.hmrc.preferencesadminfrontend.services.SearchService
import uk.gov.hmrc.preferencesadminfrontend.utils.SpecBase

import scala.concurrent.{ ExecutionContext, Future }

class MultiSearchControllerSpec
    extends PlaySpec with GuiceOneAppPerSuite with SpecBase with ScalaFutures with MockitoSugar {

  lazy val mockSearchService: SearchService = mock[SearchService]

  override implicit lazy val app: Application = GuiceApplicationBuilder()
    .overrides(inject.bind[SearchService].toInstance(mockSearchService))
    .build()

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit lazy val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]
  implicit lazy val materializer: Materializer = app.materializer
  implicit lazy val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  lazy val controller: MultiSearchController = app.injector.instanceOf[MultiSearchController]

  "GET /decode" should {
    "return 200" in {
      val result = controller.showDecodePage()(FakeRequest("GET", "/decode").withSession(User.sessionKey -> "admin"))
      status(result) mustBe Status.OK
    }

    "return HTML" in {
      val result = controller.showDecodePage()(FakeRequest("GET", "/decode").withSession(User.sessionKey -> "admin"))
      contentType(result) mustBe Some("text/html")
      charset(result) mustBe Some("utf-8")
    }

    "redirect to login page for non-admin user" in {
      val result = controller.showDecodePage()(FakeRequest("GET", "/decode").withSession(User.sessionKey -> "user"))
      status(result) mustBe Status.SEE_OTHER
    }
  }

  "GET /multi-search" should {
    "return 200" in {
      val result =
        controller.showMultiSearchPage()(FakeRequest("GET", "/multi-search").withSession(User.sessionKey -> "admin"))
      status(result) mustBe Status.OK
    }

    "return HTML" in {
      val result =
        controller.showMultiSearchPage()(FakeRequest("GET", "/multi-search").withSession(User.sessionKey -> "admin"))
      contentType(result) mustBe Some("text/html")
      charset(result) mustBe Some("utf-8")
    }

    "redirect to login page for non-admin user" in {
      val result =
        controller.showMultiSearchPage()(FakeRequest("GET", "/multi-search").withSession(User.sessionKey -> "user"))
      status(result) mustBe Status.SEE_OTHER
    }
  }

  "showResultsPage - POST /multi-search/results" should {
    "return 400 when form binding fails" in {
      val request = FakeRequest("POST", "/multi-search/results")
        .withSession(User.sessionKey -> "admin")

      val result = controller.showResultsPage()(request)

      status(result) mustBe BAD_REQUEST
    }

    "return 200 when service returns nil " in {
      when(mockSearchService.searchPreferences(any())(any(), any(), any()))
        .thenReturn(Future.successful(Nil))

      val request = FakeRequest("POST", "/multi-search/results")
        .withSession(User.sessionKey -> "admin")
        .withFormUrlEncodedBody(
          "search-ninos" -> "AB123456C",
          "batch"        -> ""
        )

      val result = controller.showResultsPage()(request)

      status(result) mustBe OK
    }

    "return 200 when service returns a list of preferences" in {
      when(mockSearchService.searchPreferences(any())(any(), any(), any()))
        .thenReturn(Future.successful(List(("AB123456C", "test@example.com"))))

      val request = FakeRequest("POST", "/multi-search/results")
        .withSession(User.sessionKey -> "admin")
        .withFormUrlEncodedBody(
          "search-ninos" -> "AB123456C",
          "batch"        -> ""
        )

      val result = controller.showResultsPage()(request)

      status(result) mustBe OK
    }
  }
}
