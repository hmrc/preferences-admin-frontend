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

package uk.gov.hmrc.preferencesadminfrontend.connectors

import org.apache.pekko.actor.ActorSystem
import org.mockito.Mockito.when
import org.scalatest.EitherValues
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Configuration
import uk.gov.hmrc.http.client.{ HttpClientV2, RequestBuilder }
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.net.{ URI, URL }
import org.mockito.ArgumentMatchers.any
import uk.gov.hmrc.hmrcfrontend.views.Aliases.Event
import uk.gov.hmrc.http.{ BadRequestException, HeaderCarrier }

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class PreferencesConnectorSpec extends PlaySpec with ScalaFutures with EitherValues with GuiceOneAppPerSuite {

  "getPreferencesByEmail" must {
    "return a list of PreferenceDetails on success" in new Scope {
      val expectedPath: URL = new URI(s"$serviceUrl/preferences/find-by-email").toURL

      when(mockHttpClient.post(expectedPath)).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody(any)(any, any, any)).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[List[PreferenceDetails]](any, any))
        .thenReturn(Future.successful(mockResponseList))

      val result: Seq[PreferenceDetails] = connector.getPreferencesByEmail(email).futureValue

      result mustBe mockResponseList
    }

    "return an empty list if the service returns a BadRequestException" in new Scope {
      val expectedPath: URL = new URI(s"$serviceUrl/preferences/find-by-email").toURL

      when(mockHttpClient.post(expectedPath)).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody(any)(any, any, any)).thenReturn(mockRequestBuilder)

      // BLANK 1: Make the execute call fail with a BadRequestException("Bad Request")
      when(mockRequestBuilder.execute[List[PreferenceDetails]](any, any))
        .thenReturn(Future.failed(BadRequestException("Bad Request")))

      val result: Seq[PreferenceDetails] = connector.getPreferencesByEmail(email).futureValue

      // BLANK 2: What should the result be?
      result mustBe Nil
    }
  }

  "getPreferencesEvents" must {
    "return a list of events when successful" in new Scope {
      val entityId = "some-entity-id"
      val expectedPath: URL = new URI(s"$serviceUrl/preferences-admin/events/$entityId").toURL

      when(mockHttpClient.get(expectedPath)).thenReturn(mockRequestBuilder)

      // It returns a Future of a List of Events.
      when(mockRequestBuilder.execute[List[Event]](any, any))
        .thenReturn(Future.successful(mockEvents))

      val result = connector.getPreferencesEvents(entityId).futureValue

      result mustBe mockEvents
    }
  }

  trait Scope {

    val email = "test@example.com"

    val mockResponseList: Seq[PreferenceDetails] = List(
      PreferenceDetails(genericPaperless = true, genericUpdatedAt = None, isPaperless = None, email = None)
    )

    val mockEvents: Seq[Event] = List(
      Event("Title", "10am", "Content")
    )

    implicit val hc: HeaderCarrier = HeaderCarrier()

    val mockHttpClient: HttpClientV2 = mock[HttpClientV2]
    val mockRequestBuilder: RequestBuilder = mock[RequestBuilder]

    val servicesConfig: ServicesConfig = app.injector.instanceOf[ServicesConfig]
    val serviceUrl: String = servicesConfig.baseUrl("preferences")

    val mockConfig: Configuration = mock[Configuration]
    val mockActorSystem: ActorSystem = mock[ActorSystem]

    val connector = new PreferencesConnector(mockHttpClient, mockConfig, servicesConfig, mockActorSystem)
  }

}
