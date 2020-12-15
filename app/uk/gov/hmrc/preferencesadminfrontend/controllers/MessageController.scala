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

package uk.gov.hmrc.preferencesadminfrontend.controllers

import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import uk.gov.hmrc.preferencesadminfrontend.config.AppConfig
import uk.gov.hmrc.preferencesadminfrontend.connectors.{MessageConnector, PreferencesConnector}
import uk.gov.hmrc.preferencesadminfrontend.model.{AllowlistEntry, SendMessage}
import uk.gov.hmrc.preferencesadminfrontend.services.{MessageService, MessageStatus, PreferenceService}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.preferencesadminfrontend.views.html.{allowlist_add, error_template, send_messages}


class MessageController @Inject()(preferenceService: PreferenceService,    messageConnector: MessageConnector, messageService: MessageService, mcc: MessagesControllerComponents)(
  implicit appConfig: AppConfig,
  ec: ExecutionContext)
  extends FrontendController(mcc) with I18nSupport {


  def show() = Action.async{implicit request =>
  Future.successful(Ok(send_messages(SendMessage(), Seq.empty[MessageStatus])))
  }


  def send() = Action.async{implicit request =>
    SendMessage()
      .bindFromRequest()
      .fold(
        formWithErrors => {
          println("error")
          Future.successful(BadRequest(send_messages(formWithErrors, Seq.empty[MessageStatus])))
        },
        send => {
       val result: List[String] = utrList(send.utrs)
       val s =    result.map(preferenceService.getPreference)

          val ss: Future[List[MessageStatus]] =  Future.sequence(s)

          ss.map((s =>
            Ok(send_messages(SendMessage(), s))
          ))


              })
        }

private def utrList(utrs: String) = utrs.split("\r").toList.map(_.trim).distinct




}


