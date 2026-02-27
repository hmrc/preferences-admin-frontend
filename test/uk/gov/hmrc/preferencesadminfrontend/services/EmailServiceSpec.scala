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

package uk.gov.hmrc.preferencesadminfrontend.services

import org.mockito.Mockito.*
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.preferencesadminfrontend.connectors.EmailConnector

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class EmailServiceSpec extends PlaySpec with MockitoSugar {

  val mockConnector = mock[EmailConnector]
  val service = new EmailService(mockConnector)

  "findEvent" should {
    "return the response body from the connector" in {
      val transId = "123"
      val expectedBody = "event data"

      when(mockConnector.findEvent(transId))
        .thenReturn(Future.successful(HttpResponse(200, expectedBody)))

      service.findEvent(transId).map(_ mustBe expectedBody)
    }
  }
}
