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
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{ Matchers, WordSpec }
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Configuration
import play.api.http.Status
import play.api.i18n.MessagesApi
import play.api.libs.json.Json
import play.api.mvc.{ AnyContentAsEmpty, AnyContentAsJson, MessagesControllerComponents }
import play.api.test.CSRFTokenHelper._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.mvc.Http
import uk.gov.hmrc.http.{ HeaderCarrier, HttpResponse }
import uk.gov.hmrc.play.bootstrap.config.ControllerConfig
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents
import uk.gov.hmrc.preferencesadminfrontend.config.AppConfig
import uk.gov.hmrc.preferencesadminfrontend.connectors.MessageConnector
import uk.gov.hmrc.preferencesadminfrontend.controllers.model.User
import uk.gov.hmrc.preferencesadminfrontend.model.AllowlistEntry
import uk.gov.hmrc.preferencesadminfrontend.utils.SpecBase

import scala.concurrent.{ ExecutionContext, Future }

class AllowlistControllerSpec extends WordSpec with Matchers with MockitoSugar with GuiceOneAppPerSuite with SpecBase {
  ControllerConfig.fromConfig(Configuration())
  val injector = app.injector

  implicit lazy val materializer: Materializer = app.materializer
  implicit lazy val appConfig: AppConfig = app.injector.instanceOf[AppConfig]

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  implicit lazy val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  implicit val hc: HeaderCarrier = mock[HeaderCarrier]

  app.injector.instanceOf[Configuration] should not be (null)

  "showAllowlistPage" should {

    "return 200 (Ok) when a populated allowlist is successfully retrieved from the message service" in new AllowlistControllerTestCase {
      private val fakeRequestWithSession = FakeRequest(routes.AllowlistController.showAllowlistPage()).withSession(User.sessionKey -> "user")
      private val allowlistJson = Json.parse("""
                                               |{
                                               | "formIdList" : ["SA359 2018","SA251 2018","SA370 2018"]
                                               |}
        """.stripMargin)
      when(mockMessageConnector.getAllowlist()(any[HeaderCarrier])).thenReturn(
        Future.successful(
          HttpResponse(Http.Status.OK, Some(allowlistJson))
        )
      )
      private val result = allowlistController.showAllowlistPage()(fakeRequestWithSession.withCSRFToken)
      status(result) shouldBe Status.OK
    }

    "return 200 (Ok) when an empty allowlist is successfully retrieved from the message service" in new AllowlistControllerTestCase {
      private val fakeRequestWithSession = FakeRequest(routes.AllowlistController.showAllowlistPage()).withSession(User.sessionKey -> "user")
      private val allowlistJson = Json.parse("""
                                               |{
                                               | "formIdList" : []
                                               |}
        """.stripMargin)
      when(mockMessageConnector.getAllowlist()(any[HeaderCarrier])).thenReturn(
        Future.successful(
          HttpResponse(Http.Status.OK, Some(allowlistJson))
        )
      )
      private val result = allowlistController.showAllowlistPage()(fakeRequestWithSession.withCSRFToken)
      status(result) shouldBe Status.OK
    }

    "return 502 (Bad Gateway) when the message service returns an invalid allowlist" in new AllowlistControllerTestCase {
      private val fakeRequestWithSession = FakeRequest(routes.AllowlistController.showAllowlistPage()).withSession(User.sessionKey -> "user")
      private val allowlistJson = Json.parse("""
                                               |{
                                               | "blah" : "blah"
                                               |}
        """.stripMargin)
      when(mockMessageConnector.getAllowlist()(any[HeaderCarrier])).thenReturn(
        Future.successful(
          HttpResponse(Http.Status.OK, Some(allowlistJson))
        )
      )
      private val result = allowlistController.showAllowlistPage()(fakeRequestWithSession.withCSRFToken)
      status(result) shouldBe Status.BAD_GATEWAY
    }

    "return 502 (Bad Gateway) when the message service returns any other status" in new AllowlistControllerTestCase {
      private val fakeRequestWithForm = FakeRequest(routes.AllowlistController.showAllowlistPage()).withSession(User.sessionKey -> "user")
      when(mockMessageConnector.getAllowlist()(any[HeaderCarrier])).thenReturn(
        Future.successful(
          HttpResponse(Http.Status.NOT_FOUND)
        )
      )
      private val result = allowlistController.showAllowlistPage()(fakeRequestWithForm.withCSRFToken)
      status(result) shouldBe Status.BAD_GATEWAY
    }

  }

  "confirmAdd" should {

    "return 303 (Redirect) when a Form ID is successfully added via the message service" in new AllowlistControllerTestCase {
      private val fakeRequestWithSession = FakeRequest(routes.AllowlistController.showAllowlistPage()).withSession(User.sessionKey -> "user")
      private val fakeRequestWithBody: FakeRequest[AnyContentAsJson] = fakeRequestWithSession.withJsonBody(
        Json.parse("""
                     |{
                     | "formId" : "SA316 2015",
                     | "reasonText" : "some reason text"
                     |}
          """.stripMargin)
      )
      when(mockMessageConnector.addFormIdToAllowlist(any[AllowlistEntry])(any[HeaderCarrier])).thenReturn(
        Future.successful(
          HttpResponse(Http.Status.CREATED)
        )
      )
      private val result = allowlistController.confirmAdd()(fakeRequestWithBody.withCSRFToken)
      status(result) shouldBe Status.SEE_OTHER
    }

    "return 502 (Bad Gateway) when the message service returns any other status" in new AllowlistControllerTestCase {
      private val fakeRequestWithSession = FakeRequest(routes.AllowlistController.showAllowlistPage()).withSession(User.sessionKey -> "user")
      private val fakeRequestWithBody: FakeRequest[AnyContentAsJson] = fakeRequestWithSession.withJsonBody(
        Json.parse("""
                     |{
                     | "formId" : "SA316 2015",
                     | "reasonText" : "some reason text"
                     |}
          """.stripMargin)
      )
      when(mockMessageConnector.addFormIdToAllowlist(any[AllowlistEntry])(any[HeaderCarrier])).thenReturn(
        Future.successful(
          HttpResponse(Http.Status.NOT_FOUND)
        )
      )
      private val result = allowlistController.confirmAdd()(fakeRequestWithBody.withCSRFToken)
      status(result) shouldBe Status.BAD_GATEWAY
    }

    "return 400 (Bad Request) when the Form ID JSON is not valid" in new AllowlistControllerTestCase {
      private val fakeRequestWithSession = FakeRequest(routes.AllowlistController.showAllowlistPage()).withSession(User.sessionKey -> "user")
      val fakeRequestWithBody: FakeRequest[AnyContentAsJson] = fakeRequestWithSession.withJsonBody(
        Json.parse("""
                     |{
                     | "blah" : "blah"
                     |}
          """.stripMargin)
      )
      private val result = allowlistController.confirmAdd()(fakeRequestWithBody.withCSRFToken)
      status(result) shouldBe Status.BAD_REQUEST
    }
  }

  "confirmDelete" should {

    "return 303 (Redirect) when a Form ID is successfully deleted via the message service" in new AllowlistControllerTestCase {
      private val fakeRequestWithSession = FakeRequest(routes.AllowlistController.showAllowlistPage()).withSession(User.sessionKey -> "user")
      private val fakeRequestWithBody: FakeRequest[AnyContentAsJson] = fakeRequestWithSession.withJsonBody(
        Json.parse("""
                     |{
                     | "formId" : "SA316 2015",
                     | "reasonText" : "some reason text"
                     |}
          """.stripMargin)
      )
      when(mockMessageConnector.deleteFormIdFromAllowlist(any[AllowlistEntry])(any[HeaderCarrier])).thenReturn(
        Future.successful(
          HttpResponse(Http.Status.OK)
        )
      )
      private val result = allowlistController.confirmDelete()(fakeRequestWithBody.withCSRFToken)
      status(result) shouldBe Status.SEE_OTHER
    }

    "return 502 (Bad Gateway) when the message service returns any other status" in new AllowlistControllerTestCase {
      private val fakeRequestWithSession = FakeRequest(routes.AllowlistController.showAllowlistPage()).withSession(User.sessionKey -> "user")
      private val fakeRequestWithBody: FakeRequest[AnyContentAsJson] = fakeRequestWithSession.withJsonBody(
        Json.parse("""
                     |{
                     | "formId" : "SA316 2015",
                     | "reasonText" : "some reason text"
                     |}
          """.stripMargin)
      )
      when(mockMessageConnector.deleteFormIdFromAllowlist(any[AllowlistEntry])(any[HeaderCarrier])).thenReturn(
        Future.successful(
          HttpResponse(Http.Status.NOT_FOUND)
        )
      )
      private val result = allowlistController.confirmDelete()(fakeRequestWithBody.withCSRFToken)
      status(result) shouldBe Status.BAD_GATEWAY
    }

    "return 400 (Bad Request) when the form ID JSON is not valid" in new AllowlistControllerTestCase {
      private val fakeRequestWithSession = FakeRequest(routes.AllowlistController.showAllowlistPage()).withSession(User.sessionKey -> "user")
      val fakeRequestWithBody: FakeRequest[AnyContentAsJson] = fakeRequestWithSession.withJsonBody(
        Json.parse("""
                     |{
                     | "blah" : "blah"
                     |}
          """.stripMargin)
      )
      private val result = allowlistController.confirmDelete()(fakeRequestWithBody.withCSRFToken)
      status(result) shouldBe Status.BAD_REQUEST
    }
  }

}

trait AllowlistControllerTestCase extends SpecBase with MockitoSugar {
  implicit val stubbedMCC: MessagesControllerComponents = stubMessagesControllerComponents()
  implicit val ecc: ExecutionContext = stubbedMCC.executionContext

  val mockMessageConnector: MessageConnector = mock[MessageConnector]

  def allowlistController()(implicit messages: MessagesApi, appConfig: AppConfig): AllowlistController =
    new AllowlistController(mockMessageConnector, stubbedMCC)

}
