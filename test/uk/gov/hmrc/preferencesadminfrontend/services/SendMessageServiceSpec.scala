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

import org.joda.time.DateTime
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.OptionValues
import org.scalatest.concurrent.{ IntegrationPatience, ScalaFutures }
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.preferencesadminfrontend.connectors.{ EntityResolverConnector, PreferenceDetails }
import uk.gov.hmrc.preferencesadminfrontend.services.model.Email

import scala.concurrent.Future

class SendMessageServiceSpec extends PlaySpec with OptionValues with MockitoSugar with ScalaFutures with IntegrationPatience {
  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global

  "Send message" should {
    "return Message status Sent if response from message is successfull" in new TestCase {
      val sendMessageService = new SendMessageService(entityResolver, messageService)
      sendMessageService.sendMessage("1111111111").futureValue mustBe (MessageStatus("1111111111", "Sent", "green", "N/A"))
    }

    "return Message status Failed if response from message is 409" in new TestCase {
      when(messageService.sendPenalyChargeApologyMessage(any(), any())(any(), any())).thenReturn(Future.successful(Left(502, "409 conflict")))
      val sendMessageService = new SendMessageService(entityResolver, messageService)
      sendMessageService
        .sendMessage("1111111111")
        .futureValue mustBe (MessageStatus("1111111111", "unsuccessful", "red", "Duplicate UTR (message already sent)"))
    }

    "return Message status Failed and Send paper if isPaperless is true and there is pendingEmail" in new TestCase {
      override val testEmail = Email("test@test.com", true, Some(testDate), None, hasBounces = false, pendingEmail = Some("pendingemail@email.com"))
      when(entityResolver.getPreferenceDetails(any())(any(), any()))
        .thenReturn(Future.successful(Some(PreferenceDetails(true, Some(testDate), isPaperless = Some(true), false, None, Some(testEmail), None))))
      val sendMessageService = new SendMessageService(entityResolver, messageService)
      sendMessageService
        .sendMessage("1111111111")
        .futureValue mustBe (MessageStatus("1111111111", "Send Paper", "red", "Email issue identified"))
    }

    "return Message status Failed if isPaperless is false" in new TestCase {
      when(entityResolver.getPreferenceDetails(any())(any(), any()))
        .thenReturn(Future.successful(Some(PreferenceDetails(false, Some(testDate), isPaperless = Some(false), false, None, Some(testEmail), None))))
      val sendMessageService = new SendMessageService(entityResolver, messageService)
      sendMessageService.sendMessage("1111111111").futureValue mustBe (MessageStatus("1111111111", "unsuccessful", "red", "opted-out"))
    }

    "return Message status Please Retry if response failed with any other reason other than conflict" in new TestCase {
      when(messageService.sendPenalyChargeApologyMessage(any(), any())(any(), any())).thenReturn(Future.successful(Left(500, "unexpected error")))
      val sendMessageService = new SendMessageService(entityResolver, messageService)
      sendMessageService.sendMessage("1111111111").futureValue mustBe (MessageStatus("1111111111", "Please Retry", "red", "Comms Failed 50x"))
    }

    "return Message status Failed if preference status has email bounced" in new TestCase {
      override val testEmail = Email("test@test.com", false, Some(testDate), None, hasBounces = true, None)
      when(entityResolver.getPreferenceDetails(any())(any(), any()))
        .thenReturn(Future.successful(Some(PreferenceDetails(true, Some(testDate), Some(true), false, None, Some(testEmail), None))))
      val sendMessageService = new SendMessageService(entityResolver, messageService)
      sendMessageService.sendMessage("1111111111").futureValue mustBe (MessageStatus("1111111111", "unsuccessful", "red", "preference state-bounced"))
    }

    "return Message status Failed if preference status has email verifiedOn date is missing" in new TestCase {
      override val testEmail = Email("test@test.com", verified = false, verifiedOn = None, None, hasBounces = false, None)
      when(entityResolver.getPreferenceDetails(any())(any(), any()))
        .thenReturn(Future.successful(Some(PreferenceDetails(true, Some(testDate), Some(true), false, None, Some(testEmail), None))))
      val sendMessageService = new SendMessageService(entityResolver, messageService)
      sendMessageService.sendMessage("1111111111").futureValue mustBe (MessageStatus("1111111111", "unsuccessful", "red", "unverified"))
    }

    "return Message status Failed if preference record is missing" in new TestCase {
      override val testEmail = Email("test@test.com", verified = false, verifiedOn = None, None, hasBounces = false, None)
      when(entityResolver.getPreferenceDetails(any())(any(), any())).thenReturn(Future.successful(None))
      val sendMessageService = new SendMessageService(entityResolver, messageService)
      sendMessageService.sendMessage("1111111111").futureValue mustBe (MessageStatus("1111111111", "unsuccessful", "red", "No Preference record"))
    }

    "return Message status Failed if preference is opted out" in new TestCase {
      when(entityResolver.getPreferenceDetails(any())(any(), any()))
        .thenReturn(Future.successful(Some(PreferenceDetails(genericPaperless = false, Some(testDate), Some(true), false, None, None, None))))
      val sendMessageService = new SendMessageService(entityResolver, messageService)
      sendMessageService.sendMessage("1111111111").futureValue mustBe (MessageStatus("1111111111", "unsuccessful", "red", "opted-out"))
    }
  }

  class TestCase {

    val entityResolver = mock[EntityResolverConnector]
    val messageService = mock[MessageService]
    implicit val hc: HeaderCarrier = HeaderCarrier()

    val testDate = DateTime.now.minus(20000)
    val testEmail = Email("test@test.com", true, Some(testDate), None, hasBounces = false, pendingEmail = None)
    val testEntityId = "1111111"

    when(entityResolver.getPreferenceDetails(any())(any(), any()))
      .thenReturn(Future.successful(Some(PreferenceDetails(true, Some(testDate), isPaperless = Some(true), false, None, Some(testEmail), None))))
    when(messageService.sendPenalyChargeApologyMessage(any(), any())(any(), any())).thenReturn(Future.successful(Right("messageId")))
  }
}
