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
import org.scalatest.EitherValues
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import uk.gov.hmrc.http.{ HeaderCarrier, HttpClient, HttpResponse }
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.preferencesadminfrontend.connectors.ChannelPreferencesConnector.StatusUpdate
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ChannelPreferencesConnectorSpec extends PlaySpec with ScalaFutures with EitherValues with GuiceOneAppPerSuite {

  trait Scope {
    val OK = 200
    val Bad = 400
    val enrolment = "ITSA-NICE-DAY"
    val status = true
    val statusUpdate: StatusUpdate = StatusUpdate(enrolment, status)

    implicit val headerCarrier: HeaderCarrier = HeaderCarrier()
    val httpClient: HttpClient = mock[HttpClient]
    val servicesConfig: ServicesConfig = app.injector.instanceOf[ServicesConfig]

    val channelPreferencesConnector = new ChannelPreferencesConnector(httpClient, servicesConfig)

    val channelPreferencesServiceUrl: String = app.injector.instanceOf[ServicesConfig].baseUrl("channel-preferences")
    val expectedPath = s"$channelPreferencesServiceUrl/channel-preferences/preference/itsa/status"

    def httpResponse(status: Int, body: String): HttpResponse = HttpResponse(
      status = status,
      body = body,
      headers = Map.empty
    )
  }

  "updateStatus" must {
    "return right SentStatus.Sent upon success" in new Scope {
      when(httpClient.POST[StatusUpdate, HttpResponse](expectedPath, statusUpdate))
        .thenReturn(Future.successful(httpResponse(OK, "ITSA-GREAT-DAY")))

      channelPreferencesConnector
        .updateStatus(statusUpdate)(headerCarrier)
        .futureValue mustBe Right(())
    }

    "return left upstream error message upon a failure response" in new Scope {
      when(httpClient.POST[StatusUpdate, HttpResponse](expectedPath, statusUpdate))
        .thenReturn(Future.successful(httpResponse(Bad, "ITSA-BAD-DAY")))

      channelPreferencesConnector
        .updateStatus(statusUpdate)(headerCarrier)
        .futureValue
        .left
        .value mustBe s"upstream error when sending status update, $Bad ITSA-BAD-DAY"
    }
  }
}
