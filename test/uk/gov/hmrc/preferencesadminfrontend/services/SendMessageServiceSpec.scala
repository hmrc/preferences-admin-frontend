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

package uk.gov.hmrc.preferencesadminfrontend.services

import org.joda.time.DateTime
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.mockito.MockitoSugar
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.preferencesadminfrontend.connectors.{EntityResolverConnector, PreferenceDetails}
import uk.gov.hmrc.preferencesadminfrontend.services.model.Email
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SendMessageServiceSpec extends UnitSpec with MockitoSugar with ScalaFutures with IntegrationPatience {

  "Send message" should {
    "return Message status Sent if response from message is successfull" in new TestCase {
     val sendMessageService = new SendMessageService(entityResolver, messageService)
      sendMessageService.sendMessage("1111111111").futureValue shouldBe(MessageStatus("1111111111","Sent","green","N/A"))
    }

    "return Message status Failed if response from message is 409" in new TestCase {
      when(messageService.sendPenalyChargeApologyMessage(any(), any())(any(), any())).thenReturn(Future.successful(Left(502, "409 conflict")))
      val sendMessageService = new SendMessageService(entityResolver, messageService)
      sendMessageService.sendMessage("1111111111").futureValue shouldBe(MessageStatus("1111111111","unsuccessful","red","Duplicate UTR (message already sent)"))
    }

    "return Message status Retry if response failed with any other reason other than conflict" in new TestCase {
      when(messageService.sendPenalyChargeApologyMessage(any(), any())(any(), any())).thenReturn(Future.successful(Left(500, "unexpected error")))
      val sendMessageService = new SendMessageService(entityResolver, messageService)
      sendMessageService.sendMessage("1111111111").futureValue shouldBe(MessageStatus("1111111111","Retry","red","Unknown error"))
    }

    "return Message status Failed if preference status has email bounced" in new TestCase{
     override val testEmail = Email("test@test.com", false, Some(testDate),None, hasBounces =  true)
      when(entityResolver.getPreferenceDetails(any())(any(), any())).thenReturn(Future.successful(Some(PreferenceDetails(true, Some(testDate), false, None, Some(testEmail), None))))
      val sendMessageService = new SendMessageService(entityResolver, messageService)
      sendMessageService.sendMessage("1111111111").futureValue shouldBe(MessageStatus("1111111111","unsuccessful","red","preference state-bounced"))
    }

    "return Message status Failed if preference status has email verifiedOn date is missing" in new TestCase{
      override val testEmail = Email("test@test.com", verified =  false,verifiedOn = None,None, hasBounces =  false)
      when(entityResolver.getPreferenceDetails(any())(any(), any())).thenReturn(Future.successful(Some(PreferenceDetails(true, Some(testDate), false, None, Some(testEmail), None))))
      val sendMessageService = new SendMessageService(entityResolver, messageService)
      sendMessageService.sendMessage("1111111111").futureValue shouldBe(MessageStatus("1111111111","unsuccessful","red","unverified"))
    }

    "return Message status Failed if preference record is missing" in new TestCase{
      override val testEmail = Email("test@test.com", verified =  false,verifiedOn = None,None, hasBounces =  false)
      when(entityResolver.getPreferenceDetails(any())(any(), any())).thenReturn(Future.successful(None))
      val sendMessageService = new SendMessageService(entityResolver, messageService)
      sendMessageService.sendMessage("1111111111").futureValue shouldBe(MessageStatus("1111111111","unsuccessful","red","No Preference record"))
    }

    "return Message status Failed if preference is opted out" in new TestCase{
      when(entityResolver.getPreferenceDetails(any())(any(), any())).thenReturn(Future.successful(Some(PreferenceDetails(genericPaperless = false, Some(testDate), false, None, None, None))))
      val sendMessageService = new SendMessageService(entityResolver, messageService)
      sendMessageService.sendMessage("1111111111").futureValue shouldBe(MessageStatus("1111111111","unsuccessful","red","opted-out"))
    }
  }


  class TestCase {

    val entityResolver = mock[EntityResolverConnector]
    val messageService = mock[MessageService]
    implicit val hc: HeaderCarrier = HeaderCarrier()


    val testDate = DateTime.now.minus(20000)
    val testEmail = Email("test@test.com", true, Some(testDate),None, false)
    val testEntityId = "1111111"

    when(entityResolver.getPreferenceDetails(any())(any(), any())).thenReturn(Future.successful(Some(PreferenceDetails(true, Some(testDate), false, None, Some(testEmail), None))))
    when(messageService.sendPenalyChargeApologyMessage(any(), any())(any(), any())).thenReturn(Future.successful(Right("messageId")))
  }
}
