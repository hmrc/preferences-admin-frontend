/*
 * Copyright 2019 HM Revenue & Customs
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

import javax.inject.{Inject, Singleton}
import play.api.{Configuration, Play}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.config.AppName
import uk.gov.hmrc.play.frontend.controller.FrontendController
import uk.gov.hmrc.preferencesadminfrontend.config.AppConfig
import uk.gov.hmrc.preferencesadminfrontend.controllers.model.{Rescindment, Search}
import uk.gov.hmrc.preferencesadminfrontend.services.RescindmentService

import scala.concurrent.Future

@Singleton
class RescindmentController @Inject()(auditConnector: AuditConnector, rescindmentService: RescindmentService)
                                     (implicit appConfig: AppConfig, val messagesApi: MessagesApi)
  extends FrontendController with AppName with I18nSupport {

  def appNameConfiguration: Configuration = Play.current.configuration

  def showRescindmentPage(): Action[AnyContent] = AuthorisedAction.async {
    implicit request =>
      implicit user =>
        Future.successful(
          Ok(uk.gov.hmrc.preferencesadminfrontend.views.html.rescindment(Rescindment().discardingErrors))
        )
  }

  def rescindment(): Action[AnyContent] = AuthorisedAction.async {
    implicit request =>
      implicit user =>
        Rescindment().bindFromRequest.fold(
          errors => Future.successful(BadRequest(uk.gov.hmrc.preferencesadminfrontend.views.html.rescindment(errors))),
          rescindmentRequest => {
            rescindmentService.addRescindments(rescindmentRequest).map(updateResult =>
              Ok(uk.gov.hmrc.preferencesadminfrontend.views.html.rescindment_send(
                Rescindment().discardingErrors, succeeded = updateResult.succeeded.toString)
              )
            )
          }
        )
  }

  def showRescindmentAlertsPage(): Action[AnyContent] = AuthorisedAction.async {
    implicit request =>
      implicit user =>
        Future.successful(
          Ok(uk.gov.hmrc.preferencesadminfrontend.views.html.rescindment_send(Rescindment().discardingErrors))
        )
  }

  def sendRescindmentAlerts(): Action[AnyContent] = AuthorisedAction.async {
    implicit request =>
      implicit user =>
        rescindmentService.sendRescindmentAlerts().map(alertsResult =>
          Ok(uk.gov.hmrc.preferencesadminfrontend.views.html.rescindment_send(
            Rescindment().discardingErrors, sent = alertsResult.sent.toString, failed = alertsResult.failed.toString
          ))
        )
  }
}