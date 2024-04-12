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

import cats.syntax.either._
import play.api.http.Status
import play.api.libs.json.{ Json, OFormat }
import uk.gov.hmrc.http.{ HeaderCarrier, HttpClient, HttpResponse }
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.preferencesadminfrontend.connectors.ChannelPreferencesConnector.StatusUpdate
import uk.gov.hmrc.http.HttpReads.Implicits._
import javax.inject.{ Inject, Singleton }
import scala.concurrent.{ ExecutionContext, Future }

object ChannelPreferencesConnector {
  final case class StatusUpdate(enrolment: String, status: Boolean)

  object StatusUpdate {
    implicit val fmt: OFormat[StatusUpdate] = Json.format[StatusUpdate]
  }
}

@Singleton
class ChannelPreferencesConnector @Inject() (httpClient: HttpClient, val servicesConfig: ServicesConfig)(implicit
  ec: ExecutionContext
) {
  def serviceUrl: String = servicesConfig.baseUrl("channel-preferences")

  def updateStatus(statusUpdate: StatusUpdate)(implicit hc: HeaderCarrier): Future[Either[String, Unit]] =
    httpClient
      .POST[StatusUpdate, HttpResponse](s"$serviceUrl/channel-preferences/preference/itsa/status", statusUpdate)
      .map { httpResponse =>
        httpResponse.status match {
          case status if Status.isSuccessful(status) => ().asRight
          case other => s"upstream error when sending status update, $other ${httpResponse.body}".asLeft
        }
      }
}
