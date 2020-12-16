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
import uk.gov.hmrc.preferencesadminfrontend.services.FailedReason.{ BouncedEmail, Duplicate, EmailMissing, NoPreference, NotApplicable, OptedOut, UnVerified }
import uk.gov.hmrc.preferencesadminfrontend.services.SentStatus.{ Failed, Sent }
import uk.gov.hmrc.preferencesadminfrontend.services.model.TaxIdentifier

import javax.inject.Inject
import scala.concurrent.{ ExecutionContext, Future }

class PreferenceService @Inject()(entityResolver: EntityResolverConnector, messageService: MessageService) {

  def getPreference(utr: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[MessageStatus] =
    (for {
      preferenceDetails: Option[PreferenceDetails] <- entityResolver.getPreferenceDetails(TaxIdentifier("sautr", utr))
    } yield
      preferenceDetails match {
        case Some(preference) =>
          (preference.genericPaperless, preference.email) match {
            case (_, Some(email)) if (email.hasBounces) =>
              Future.successful(MessageStatus(utr, preference.genericPaperless, Failed, displayClass(Failed), BouncedEmail))
            case (_, Some(email)) if (email.verifiedOn.isEmpty) =>
              Future.successful(MessageStatus(utr, preference.genericPaperless, Failed, displayClass(Failed), UnVerified))
            case (true, Some(email)) =>
              for {
                sent <- messageService.sendPenalyChargeApologyMessage(email.address, utr)
                status = sent match {
                  case Left(s) => {
                    MessageStatus(utr, preference.genericPaperless, Failed, displayClass(Failed), Duplicate)
                  }
                  case _ => MessageStatus(utr, preference.genericPaperless, Sent, displayClass(Sent), NotApplicable)
                }
              } yield status
            case (false, _) => Future.successful(MessageStatus(utr, false, Failed, displayClass(Failed), OptedOut))
          }
        case _ => Future.successful(MessageStatus(utr, false, Failed, displayClass(Failed), NoPreference))
      }).flatMap(identity)

  def displayClass(s: String) = if (s == "Sent") "green" else "red"

}

case class MessageStatus(utr: String, digital: Boolean, status: String, displayClass: String, reason: String)

trait FailedReason
object FailedReason {
  val NotApplicable = "N/A"
  val UnVerified = "unverified"
  val BouncedEmail = "preference state-bounced"
  val OptedOut = "opted-out"
  val NoPreference = "No Preference record"
  val Duplicate = "Duplicate UTR (message already sent)"
  val EmailMissing = "Email missing"
}

trait SentStatus

object SentStatus {
  val Sent = "Sent"
  val Failed = "unsuccessful"
}
