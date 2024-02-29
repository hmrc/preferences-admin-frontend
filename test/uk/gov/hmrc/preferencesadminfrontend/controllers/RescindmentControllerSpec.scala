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
import org.mockito.Mockito.when
import org.mockito.{ ArgumentMatcher, ArgumentMatchers }
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Configuration
import play.api.http.Status
import play.api.i18n.MessagesApi
import play.api.test.CSRFTokenHelper._
import play.api.test.FakeRequest
import play.api.test.Helpers.{ contentAsString, defaultAwaitTimeout, headers, status }
import uk.gov.hmrc.http.{ HeaderCarrier, HttpClient }
import uk.gov.hmrc.play.audit.model.MergedDataEvent
import uk.gov.hmrc.preferencesadminfrontend.config.AppConfig
import uk.gov.hmrc.preferencesadminfrontend.controllers.model.User
import uk.gov.hmrc.preferencesadminfrontend.model.{ RescindmentAlertsResult, RescindmentRequest, RescindmentUpdateResult }
import uk.gov.hmrc.preferencesadminfrontend.services._
import uk.gov.hmrc.preferencesadminfrontend.utils.SpecBase
import uk.gov.hmrc.preferencesadminfrontend.views.html.{ rescindment, rescindment_send }

import scala.concurrent.{ ExecutionContext, Future }

class RescindmentControllerSpec extends PlaySpec with ScalaFutures with GuiceOneAppPerSuite {
  implicit val hc = HeaderCarrier()
  implicit val messagesApi = app.injector.instanceOf[MessagesApi]
  implicit val materializer = app.injector.instanceOf[Materializer]

  val playConfiguration = app.injector.instanceOf[Configuration]

  "showRescindmentPage" should {
    "return ok if session is authorised" in new RescindmentTestCase {
      val result = rescindmentController.showRescindmentPage()(FakeRequest().withSession(User.sessionKey -> "user").withCSRFToken)

      status(result) mustBe Status.OK
    }

    "redirect to login page if not authorised" in new RescindmentTestCase {
      val result = rescindmentController.showRescindmentPage()(FakeRequest().withSession().withCSRFToken)

      status(result) mustBe Status.SEE_OTHER
      headers(result) must contain("Location" -> "/paperless/admin")
    }
  }

  "showRescindmentAlertsPage" should {
    "return ok if session is authorised" in new RescindmentTestCase {
      val result = rescindmentController.showRescindmentAlertsPage()(FakeRequest().withSession(User.sessionKey -> "user").withCSRFToken)

      status(result) mustBe Status.OK
    }

    "redirect to login page if not authorised" in new RescindmentTestCase {
      val result = rescindmentController.showRescindmentAlertsPage()(FakeRequest().withSession().withCSRFToken)

      status(result) mustBe Status.SEE_OTHER
      headers(result) must contain("Location" -> "/paperless/admin")
    }
  }

  "rescindment" should {
    "return ok if session is authorised and form data payload is valid" in new RescindmentTestCase {
      val fakeRequestWithForm = FakeRequest(routes.RescindmentController.rescindmentAction())
        .withFormUrlEncodedBody(
          "batchId"         -> "1234567",
          "formId"          -> "SA316",
          "date"            -> "2017-03-16",
          "reference"       -> "ref-test",
          "emailTemplateId" -> "rescindedMessageAlert"
        )
        .withSession(User.sessionKey -> "user")
        .withCSRFToken
      val rescindmentRequest = RescindmentRequest(
        batchId = "1234567",
        formId = "SA316",
        date = "2017-03-16",
        reference = "ref-test",
        emailTemplateId = "rescindedMessageAlert"
      )
      val rescindmentUpdateResult = RescindmentUpdateResult(
        tried = 1,
        succeeded = 1,
        alreadyUpdated = 0,
        invalidState = 0
      )
      when(rescindmentServiceMock.addRescindments(ArgumentMatchers.eq(rescindmentRequest))(any[HeaderCarrier](), any[ExecutionContext]()))
        .thenReturn(Future.successful(rescindmentUpdateResult))
      val result = rescindmentController.rescindmentAction()(fakeRequestWithForm)

      status(result) mustBe Status.OK
      val document = Jsoup.parse(contentAsString(result))
      document.body().getElementById("heading-succeeded").text() mustBe "Rescindment - Updated: 1"
      document.body().getElementById("heading-sent").text() mustBe "Sent: -"
      document.body().getElementById("heading-failed").text() mustBe "Failed: -"
    }

    "redirect to login page if not authorised" in new RescindmentTestCase {
      val result = rescindmentController.rescindmentAction()(FakeRequest().withSession().withCSRFToken)

      status(result) mustBe Status.SEE_OTHER
      headers(result) must contain("Location" -> "/paperless/admin")
    }

    "return a 400 BAD_REQUEST when not providing a correct body" in new RescindmentTestCase {
      val result = rescindmentController.rescindmentAction()(FakeRequest().withSession(User.sessionKey -> "user").withCSRFToken)

      status(result) mustBe Status.BAD_REQUEST
    }
  }

  "sendRescindmentAlerts" should {
    "return ok if session is authorised" in new RescindmentTestCase {
      val fakeRequestWithForm = FakeRequest(routes.RescindmentController.sendRescindmentAlerts()).withSession(User.sessionKey -> "user")
      val rescindmentAlertsResult = RescindmentAlertsResult(
        sent = 1,
        requeued = 1,
        failed = 0,
        hardCopyRequested = 0
      )
      when(rescindmentServiceMock.sendRescindmentAlerts()(any[HeaderCarrier](), any[ExecutionContext]()))
        .thenReturn(Future.successful(rescindmentAlertsResult))
      val result = rescindmentController.sendRescindmentAlerts()(fakeRequestWithForm.withCSRFToken)

      status(result) mustBe Status.OK
      val document = Jsoup.parse(contentAsString(result))
      document.body().getElementById("heading-succeeded").text() mustBe "Rescindment - Updated: -"
      document.body().getElementById("heading-sent").text() mustBe "Sent: 1"
      document.body().getElementById("heading-failed").text() mustBe "Failed: 0"
    }

    "redirect to login page if not authorised" in new RescindmentTestCase {
      val result = rescindmentController.sendRescindmentAlerts()(FakeRequest().withSession().withCSRFToken)

      status(result) mustBe Status.SEE_OTHER
      headers(result) must contain("Location" -> "/paperless/admin")
    }
  }

  class RescindmentTestCase extends SpecBase {
    implicit val ecc: ExecutionContext = stubbedMCC.executionContext
    implicit val appConfig: AppConfig = app.injector.instanceOf[AppConfig]
    val httpClient: HttpClient = app.injector.instanceOf[HttpClient]
    val authorisedAction: AuthorisedAction = app.injector.instanceOf[AuthorisedAction]
    val rescindmentView: rescindment = app.injector.instanceOf[rescindment]
    val rescindmentSendView: rescindment_send = app.injector.instanceOf[rescindment_send]
    val rescindmentServiceMock: RescindmentService = mock[RescindmentService]

    val rescindmentController: RescindmentController =
      new RescindmentController(
        authorisedAction,
        rescindmentServiceMock,
        stubbedMCC,
        rescindmentView,
        rescindmentSendView
      )

    override def isSimilar(expected: MergedDataEvent): ArgumentMatcher[MergedDataEvent] =
      new ArgumentMatcher[MergedDataEvent]() {
        def matches(t: MergedDataEvent): Boolean = this.matches(t) && {
          t.request.generatedAt == expected.request.generatedAt && t.response.generatedAt == expected.response.generatedAt
        }
      }
  }
}
