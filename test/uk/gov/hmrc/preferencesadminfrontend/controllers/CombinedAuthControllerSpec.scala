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

package uk.gov.hmrc.preferencesadminfrontend.controllers

import org.apache.pekko.stream.Materializer
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.*
import play.api.i18n.MessagesApi
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{ DefaultActionBuilder, MessagesControllerComponents }
import play.api.test.CSRFTokenHelper.CSRFFRequestHeader
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.preferencesadminfrontend.auth.FakeStrideAuth
import uk.gov.hmrc.preferencesadminfrontend.config.AppConfig
import uk.gov.hmrc.preferencesadminfrontend.utils.SpecBase
import uk.gov.hmrc.preferencesadminfrontend.views.html.home

import scala.concurrent.ExecutionContext.Implicits.global

class CombinedAuthControllerSpec extends PlaySpec with GuiceOneAppPerSuite with SpecBase with ScalaFutures {

  implicit lazy val materializer: Materializer = app.materializer
  override implicit lazy val app: Application = GuiceApplicationBuilder().build()
  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  implicit val servicesConfig: ServicesConfig = app.injector.instanceOf[ServicesConfig]
  protected lazy val authConnector: AuthConnector = MockitoSugar.mock[AuthConnector]
  val defaultActionBuilder: DefaultActionBuilder = app.injector.instanceOf[DefaultActionBuilder]
  val strideAuth = new FakeStrideAuth(authConnector, servicesConfig, defaultActionBuilder)
  val view: home = app.injector.instanceOf[home]
  val mcc: MessagesControllerComponents = stubMessagesControllerComponents()
  implicit val appConfig: AppConfig = app.injector.instanceOf[AppConfig]

  val testController: CombinedAuthController = new CombinedAuthController(view, strideAuth, mcc)

  "GET /combinedAuth" should {
    "return 200" in {
      val result = testController.showPage()(FakeRequest("GET", "/combinedAuth").withCSRFToken)
      status(result) mustBe Status.OK
    }

    "return HTML" in {
      val result = testController.showPage()(FakeRequest("GET", "/combinedAuth").withCSRFToken)
      contentType(result) mustBe Some("text/html")
      charset(result) mustBe Some("utf-8")
    }
  }
}
