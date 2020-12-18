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
import uk.gov.hmrc.preferencesadminfrontend.services.FailedReason.{ BouncedEmail, Duplicate, EmailIssueIdentified, EmailMissing, NoPreference, NotApplicable, OptedOut, UnKnownError, UnVerified }
import uk.gov.hmrc.preferencesadminfrontend.services.SentStatus.{ Failed, Retry, SendPaper, Sent }
import uk.gov.hmrc.preferencesadminfrontend.services.model.TaxIdentifier

import javax.inject.Inject
import scala.concurrent.{ ExecutionContext, Future }

class SendMessageService @Inject()(entityResolver: EntityResolverConnector, messageService: MessageService) {

  def sendMessage(utr: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[MessageStatus] =
    (for {
      preferenceDetails: Option[PreferenceDetails] <- entityResolver.getPreferenceDetails(TaxIdentifier("sautr", utr))
    } yield
      preferenceDetails match {
        case Some(preference) =>
          (preference.isPaperless, preference.genericPaperless, preference.email) match {
            case (_, true, Some(email)) if (email.pendingEmail.nonEmpty) =>
              Future.successful(MessageStatus(utr, SendPaper, displayClass(Failed), EmailIssueIdentified))
            case (_, _, Some(email)) if (email.hasBounces) =>
              Future.successful(MessageStatus(utr, Failed, displayClass(Failed), BouncedEmail))
            case (_, _, Some(email)) if (email.verifiedOn.isEmpty) =>
              Future.successful(MessageStatus(utr, Failed, displayClass(Failed), UnVerified))

            case (Some(true), true, Some(email)) if (email.pendingEmail.isEmpty) =>
              for {
                sent <- messageService.sendPenalyChargeApologyMessage(email.address, utr)
                status = sent match {
                  case Left(s) =>
                    s match {
                      case (502, s._2) if s._2.contains("409") => MessageStatus(utr, Failed, displayClass(Failed), Duplicate)
                      case _                                   => MessageStatus(utr, Retry, displayClass(Failed), UnKnownError)
                    }
                  case _ => MessageStatus(utr, Sent, displayClass(Sent), NotApplicable)
                }
              } yield status
            case (_, false, _) => Future.successful(MessageStatus(utr, Failed, displayClass(Failed), OptedOut))
            case _             => Future.successful(MessageStatus(utr, Failed, displayClass(Failed), EmailMissing))
          }
        case _ => Future.successful(MessageStatus(utr, Failed, displayClass(Failed), NoPreference))
      }).flatMap(identity)

  def displayClass(s: String) = if (s == "Sent") "green" else "red"
}

case class MessageStatus(utr: String, status: String, displayClass: String, reason: String)

object FailedReason {
  val NotApplicable = "N/A"
  val UnVerified = "unverified"
  val BouncedEmail = "preference state-bounced"
  val OptedOut = "opted-out"
  val NoPreference = "No Preference record"
  val Duplicate = "Duplicate UTR (message already sent)"
  val EmailMissing = "Email missing"
  val EmailIssueIdentified = "Email issue identified"
  val UnKnownError = "Comms Failed 50x"
}

object SentStatus {
  val Sent = "Sent"
  val Failed = "unsuccessful"
  val Retry = "Please Retry"
  val SendPaper = "Send Paper"
}
