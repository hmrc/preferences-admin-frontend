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

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.{ Application, inject }
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import play.api.test.Helpers.{ contentAsString, status }
import uk.gov.hmrc.preferencesadminfrontend.services.EmailService
import uk.gov.hmrc.preferencesadminfrontend.utils.SpecBase
import org.mockito.Mockito.mock
import play.api.test.Helpers.defaultAwaitTimeout
import play.api.http.Status

import scala.concurrent.Future

class EmailControllerSpec extends PlaySpec with GuiceOneAppPerSuite with ScalaFutures with MockitoSugar {

  lazy val mockEmailService: EmailService = mock[EmailService]

  override implicit lazy val app: Application = GuiceApplicationBuilder()
    .overrides(inject.bind[EmailService].toInstance(mockEmailService))
    .build()

  def controller: EmailController = app.injector.instanceOf[EmailController]

  "GET /findEvent/:transId" should {

    "return 200 with the event data when service returns successfully" in {
      val expectedResponse = """{"event": "email_sent", "status": "delivered"}"""

      when(mockEmailService.findEvent(any())(any()))
        .thenReturn(Future.successful(expectedResponse))

      val request = FakeRequest("GET", "/findEvent/test-transaction-123")
      val result = controller.findEvent("test-transaction-123")(request)

      status(result) mustBe Status.OK
      contentAsString(result) mustBe expectedResponse
    }

    "return 200 with empty response when service returns empty string" in {
      when(mockEmailService.findEvent(any())(any()))
        .thenReturn(Future.successful(""))

      val request = FakeRequest("GET", "/findEvent/non-existent-trans-id")
      val result = controller.findEvent("non-existent-trans-id")(request)

      status(result) mustBe Status.OK
      contentAsString(result) mustBe ""
    }

    "propagate the exception when service fails" in {
      when(mockEmailService.findEvent(any())(any()))
        .thenReturn(Future.failed(new RuntimeException("Service unavailable")))

      val request = FakeRequest("GET", "/findEvent/error-trans-id")
      val result = controller.findEvent("error-trans-id")(request)

      intercept[RuntimeException] {
        status(result)
      }
    }
  }
}
