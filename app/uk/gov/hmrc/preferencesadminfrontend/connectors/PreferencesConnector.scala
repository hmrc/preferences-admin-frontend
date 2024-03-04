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

import org.apache.pekko.actor.ActorSystem

import javax.inject.{ Inject, Singleton }
import play.api.Configuration
import play.api.libs.json.Format
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.preferencesadminfrontend.controllers.model.EmailRequest

import scala.concurrent.{ ExecutionContext, Future }

@Singleton
class PreferencesConnector @Inject()(
  val httpClient: HttpClient,
  val runModeConfiguration: Configuration,
  val servicesConfig: ServicesConfig,
  val actorSystem: ActorSystem) {

  implicit val ef: Format[Entity] = Entity.formats

  def serviceUrl: String = servicesConfig.baseUrl("preferences")

  def getPreferenceDetails(email: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[List[PreferenceDetails]] =
    httpClient.POST[EmailRequest, List[PreferenceDetails]](s"$serviceUrl/preferences/find-by-email", EmailRequest(email)).recover {
      case _: BadRequestException => Nil
    }
}
