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
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http._
import play.api.i18n.MessagesApi
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.CSRFTokenHelper._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import uk.gov.hmrc.play.audit.model.DataEvent
import uk.gov.hmrc.preferencesadminfrontend.utils.SpecBase

import scala.concurrent.{ ExecutionContext, Future }

class LoginControllerSpec extends PlaySpec with GuiceOneAppPerSuite with SpecBase with ScalaFutures {

  implicit lazy val materializer: Materializer = app.materializer
  override implicit lazy val app: Application = GuiceApplicationBuilder().build()
  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  "GET /" should {
    "return 200" in new MessageBrakeControllerTestCase {
      val result = loginController.showLoginPage()(FakeRequest("GET", "/").withCSRFToken)
      status(result) mustBe Status.OK
    }

    "return HTML" in new MessageBrakeControllerTestCase {
      val result = loginController.showLoginPage()(FakeRequest("GET", "/").withCSRFToken)
      contentType(result) mustBe Some("text/html")
      charset(result) mustBe Some("utf-8")
    }
  }

  "POST to login" should {
    "Redirect to the next page if credentials are correct" in new MessageBrakeControllerTestCase {
      val fakeRequest =
        FakeRequest(routes.LoginController.loginAction())
          .withFormUrlEncodedBody("username" -> "user", "password" -> "pwd")
          .withCSRFToken
      val result = loginController.loginAction()(fakeRequest)

      status(result) mustBe Status.SEE_OTHER
      headers(result) must contain("Location" -> "/paperless/admin/home")
      session(result).data must contain("userId" -> "user")
    }

    "Return unauthorised if credentials are not correct" in new MessageBrakeControllerTestCase {
      val fakeRequest =
        FakeRequest(routes.LoginController.loginAction())
          .withFormUrlEncodedBody("username" -> "user", "password" -> "wrongPassword")
          .withCSRFToken
      val result = loginController.loginAction()(fakeRequest)

      status(result) mustBe Status.UNAUTHORIZED
    }

    "Return bad request if credentials are missing" in new MessageBrakeControllerTestCase {
      val fakeRequest =
        FakeRequest(routes.LoginController.loginAction())
          .withFormUrlEncodedBody()
          .withCSRFToken
      val result = loginController.loginAction()(fakeRequest)

      status(result) mustBe Status.BAD_REQUEST
    }
  }

  "POST to logout" should {
    "Destroy existing session and redirect to login page" in new MessageBrakeControllerTestCase {
      val result = loginController.logoutAction()(FakeRequest().withSession("userId" -> "user").withCSRFToken)

      session(result).data must not contain ("userId" -> "user")
      status(result) mustBe Status.SEE_OTHER
      headers(result) must contain("Location" -> "/paperless/admin")
    }
  }

  class MessageBrakeControllerTestCase extends SpecBase {
    when(auditConnectorMock.sendEvent(any[DataEvent])(any[HeaderCarrier], any[ExecutionContext]))
      .thenReturn(Future.successful(AuditResult.Success))
    val loginController = app.injector.instanceOf[LoginController]
  }
}
