/*
 * Copyright 2021 HM Revenue & Customs
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

import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.Matchers._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http._
import play.api.i18n.MessagesApi
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.CSRFTokenHelper._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import uk.gov.hmrc.preferencesadminfrontend.utils.SpecBase

import scala.concurrent.Future

class LoginControllerSpec extends LoginControllerFixtures with ScalaFutures {

  "GET /" should {
    "return 200" in {
      val result = loginController.showLoginPage(FakeRequest("GET", "/").withCSRFToken)
      status(result) shouldBe Status.OK
    }

    "return HTML" in {
      val result = loginController.showLoginPage(FakeRequest("GET", "/").withCSRFToken)
      contentType(result) shouldBe Some("text/html")
      charset(result) shouldBe Some("utf-8")
    }
  }

  "POST to login" should {
    "Redirect to the next page if credentials are correct" in {
      val result = loginController.login(
        FakeRequest()
          .withFormUrlEncodedBody(
            "username" -> "user",
            "password" -> "pwd"
          )
          .withCSRFToken)

      session(result).data should contain("userId" -> "user")
      status(result) shouldBe Status.SEE_OTHER
      headers(result) should contain("Location" -> "/paperless/admin/home")
    }

    "Return unauthorised if credentials are not correct" in {
      val result = loginController.login(
        FakeRequest()
          .withFormUrlEncodedBody(
            "username" -> "user",
            "password" -> "wrongPassword"
          )
          .withCSRFToken
      )

      result.futureValue.header.status shouldBe Status.UNAUTHORIZED
    }

    "Return bad request if credentials are missing" in {
      val result = loginController.login(
        FakeRequest().withFormUrlEncodedBody().withCSRFToken
      )

      result.futureValue.header.status shouldBe Status.BAD_REQUEST
    }
  }

  "POST to logout" should {
    "Destroy existing session and redirect to login page" in {
      val result = loginController.logout(FakeRequest().withSession("userId" -> "user").withCSRFToken)

      session(result).data should not contain ("userId" -> "user")
      status(result) shouldBe Status.SEE_OTHER
      headers(result) should contain("Location" -> "/paperless/admin")
    }
  }
}

trait LoginControllerFixtures extends PlaySpec with MockitoSugar with GuiceOneAppPerSuite with SpecBase {
  override implicit lazy val app = GuiceApplicationBuilder().build()
  implicit val messagesApi = app.injector.instanceOf[MessagesApi]
  when(auditConnectorMock.sendEvent(any())(any(), any())).thenReturn(Future.successful(AuditResult.Success))
  val loginController = app.injector.instanceOf[LoginController]
}
