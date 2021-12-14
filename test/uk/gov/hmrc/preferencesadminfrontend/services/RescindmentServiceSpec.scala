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

package uk.gov.hmrc.preferencesadminfrontend.services

import org.mockito.Mockito._
import org.scalatest.concurrent.{ IntegrationPatience, ScalaFutures }
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.preferencesadminfrontend.model.{ RescindmentAlertsResult, RescindmentRequest, RescindmentUpdateResult }
import uk.gov.hmrc.preferencesadminfrontend.utils.SpecBase

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RescindmentServiceSpec extends PlaySpec with ScalaFutures with IntegrationPatience {

  implicit val hc = HeaderCarrier()

  "addRescindments" should {
    "return a valid update result" in new RescindmentServiceTestCase {
      when(messageConnectorMock.addRescindments(rescindmentRequest)).thenReturn(Future.successful(rescindmentUpdateResult))
      rescindmentService.addRescindments(rescindmentRequest).futureValue mustBe rescindmentUpdateResult
    }
  }

  "sendRescindmentAlerts" should {
    "return a valid alert result" in new RescindmentServiceTestCase {
      when(messageConnectorMock.sendRescindmentAlerts()).thenReturn(Future.successful(rescindmentAlertsResult))
      rescindmentService.sendRescindmentAlerts().futureValue mustBe rescindmentAlertsResult
    }
  }

  trait RescindmentServiceTestCase extends SpecBase {
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
    val rescindmentAlertsResult = RescindmentAlertsResult(
      sent = 1,
      requeued = 1,
      failed = 0,
      hardCopyRequested = 0
    )
    val rescindmentService = new RescindmentService(messageConnectorMock)
  }
}
