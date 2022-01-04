/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.preferencesadminfrontend.controllers

import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.preferencesadminfrontend.config.AppConfig
import uk.gov.hmrc.preferencesadminfrontend.model.SendMessage
import uk.gov.hmrc.preferencesadminfrontend.model.SendMessage.listParser
import uk.gov.hmrc.preferencesadminfrontend.services.SendMessageService
import uk.gov.hmrc.preferencesadminfrontend.views.html.{ message_status, send_messages }

import javax.inject.Inject
import scala.concurrent.{ ExecutionContext, Future }

class MessageController @Inject()(
  authorisedAction: AuthorisedAction,
  messageStatusView: message_status,
  sendMessagesView: send_messages,
  sendMessageService: SendMessageService,
  mcc: MessagesControllerComponents)(implicit appConfig: AppConfig, ec: ExecutionContext)
    extends FrontendController(mcc) with I18nSupport with Logging {

  def show() = authorisedAction.async { implicit request => implicit user =>
    Future.successful(Ok(sendMessagesView(SendMessage())))
  }

  def send() = authorisedAction.async { implicit request => implicit user =>
    SendMessage()
      .bindFromRequest()
      .fold(
        formWithErrors => {
          Future.successful(BadRequest(sendMessagesView(formWithErrors)))
        },
        input => {
          Future.sequence(listParser(input.utrs).map(sendMessageService.sendMessage)).map { result =>
            Ok(messageStatusView(result))
          }
        }
      )
  }
}
