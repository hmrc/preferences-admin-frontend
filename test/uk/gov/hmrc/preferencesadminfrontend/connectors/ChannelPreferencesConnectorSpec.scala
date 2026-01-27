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

package uk.gov.hmrc.preferencesadminfrontend.connectors

import org.mockito.Mockito.when
import org.mockito.ArgumentMatchers.any
import org.scalatest.EitherValues
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.{ JsSuccess, Json }
import uk.gov.hmrc.http.{ HeaderCarrier, HttpResponse }
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.preferencesadminfrontend.connectors.ChannelPreferencesConnector.StatusUpdate
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw
import uk.gov.hmrc.http.client.{ HttpClientV2, RequestBuilder }

import java.net.URI
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ChannelPreferencesConnectorSpec extends PlaySpec with ScalaFutures with EitherValues with GuiceOneAppPerSuite {

  "updateStatus" must {
    "return right SentStatus.Sent upon success" in new Scope {
      when(httpClient.post(expectedPath)).thenReturn(requestBuilder)
      when(requestBuilder.withBody(any)(any, any, any)).thenReturn(requestBuilder)
      when(requestBuilder.execute[HttpResponse]).thenReturn(Future.successful(httpResponse(OK, "ITSA-GREAT-DAY")))

      channelPreferencesConnector
        .updateStatus(statusUpdate)(headerCarrier)
        .futureValue mustBe Right(())
    }

    "return left upstream error message upon a failure response" in new Scope {
      when(httpClient.post(expectedPath)).thenReturn(requestBuilder)
      when(requestBuilder.withBody(any)(any, any, any)).thenReturn(requestBuilder)
      when(requestBuilder.execute[HttpResponse]).thenReturn(Future.successful(httpResponse(Bad, "ITSA-BAD-DAY")))

      channelPreferencesConnector
        .updateStatus(statusUpdate)(headerCarrier)
        .futureValue
        .left
        .value mustBe s"upstream error when sending status update, $Bad ITSA-BAD-DAY"
    }
  }

  "StatusUpdate" must {

    "deserialize from JSON" in {
      val json = Json.parse(
        """{
          | "enrolment": "X123456789",
          | "status": true
          |}""".stripMargin
      )

      val result = json.validate[StatusUpdate]

      result mustBe JsSuccess(StatusUpdate("X123456789", status = true))
    }

    "serialize to JSON" in {
      val statusUpdate = StatusUpdate("X123456789", status = false)

      val result = Json.toJson(statusUpdate)

      (result \ "enrolment").as[String] mustBe "X123456789"
      (result \ "status").as[Boolean] mustBe false
    }
  }

  trait Scope {
    val OK = 200
    val Bad = 400
    val enrolment = "ITSA-NICE-DAY"
    val status = true
    val statusUpdate: StatusUpdate = StatusUpdate(enrolment, status)

    implicit val headerCarrier: HeaderCarrier = HeaderCarrier()
    val httpClient: HttpClientV2 = mock[HttpClientV2]
    val requestBuilder: RequestBuilder = mock[RequestBuilder]
    val servicesConfig: ServicesConfig = app.injector.instanceOf[ServicesConfig]

    val channelPreferencesConnector = new ChannelPreferencesConnector(httpClient, servicesConfig)

    val channelPreferencesServiceUrl: String = app.injector.instanceOf[ServicesConfig].baseUrl("channel-preferences")
    val expectedPath = new URI(s"$channelPreferencesServiceUrl/channel-preferences/preference/itsa/status").toURL

    def httpResponse(status: Int, body: String): HttpResponse = HttpResponse(
      status = status,
      body = body,
      headers = Map.empty
    )
  }
}
