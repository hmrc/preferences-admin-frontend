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

import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.preferencesadminfrontend.connectors.{ EntityResolverConnector, MessageConnector, PreferenceDetails, PreferencesConnector }
import uk.gov.hmrc.preferencesadminfrontend.services.model.TaxIdentifier

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class PreferenceService @Inject()(entityResolver: EntityResolverConnector, messageService: MessageService) {

  def getPreference(utr: String)(implicit hc: HeaderCarrier, ec: ExecutionContext) =
    for {
      preferenceDetails <- entityResolver.getPreferenceDetails(TaxIdentifier("sautr", utr))
      status = preferenceDetails match {
        case Some(preference) =>
          preference.email match {
            case Some(email) => {
              messageService.sendPenalyChargeApologyMessage(email.address, utr)
              MessageStatus(utr, true, preference.genericPaperless)
            }
            case _ => MessageStatus(utr, true, preference.genericPaperless)
          }
        case _ => MessageStatus(utr, false, false)
      }
    } yield status
}

case class MessageStatus(utr: String, preferenceExists: Boolean, paperless: Boolean)
