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

package uk.gov.hmrc.preferencesadminfrontend.controllers

import javax.inject.{ Inject, Singleton }
import play.api.i18n.{ I18nSupport, MessagesApi }
import play.api.mvc.{ Action, AnyContent, MessagesControllerComponents }
import play.api.{ Configuration, Logging, Play }
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.preferencesadminfrontend.config.AppConfig
import uk.gov.hmrc.preferencesadminfrontend.controllers.model.Rescindment
import uk.gov.hmrc.preferencesadminfrontend.services.RescindmentService
import uk.gov.hmrc.preferencesadminfrontend.views.html.{ rescindment, rescindment_send }

import scala.concurrent.{ ExecutionContext, Future }

@Singleton
class RescindmentController @Inject()(
  authorisedAction: AuthorisedAction,
  rescindmentService: RescindmentService,
  mcc: MessagesControllerComponents,
  rescindmentView: rescindment,
  rescindmentSendView: rescindment_send)(implicit appConfig: AppConfig, ec: ExecutionContext)
    extends FrontendController(mcc) with I18nSupport with Logging {

  def showRescindmentPage(): Action[AnyContent] = authorisedAction.async { implicit request => implicit user =>
    Future.successful(
      Ok(rescindmentView(Rescindment().discardingErrors))
    )
  }

  def rescindmentAction(): Action[AnyContent] = authorisedAction.async { implicit request => implicit user =>
    Rescindment().bindFromRequest.fold(
      errors => Future.successful(BadRequest(rescindmentView(errors))),
      rescindmentRequest => {
        rescindmentService
          .addRescindments(rescindmentRequest)
          .map(updateResult => Ok(rescindmentSendView(Rescindment().discardingErrors, succeeded = updateResult.succeeded.toString)))
      }
    )
  }

  def showRescindmentAlertsPage(): Action[AnyContent] = authorisedAction.async { implicit request => implicit user =>
    Future.successful(
      Ok(rescindmentSendView(Rescindment().discardingErrors))
    )
  }

  def sendRescindmentAlerts(): Action[AnyContent] = authorisedAction.async { implicit request => implicit user =>
    rescindmentService
      .sendRescindmentAlerts()
      .map(
        alertsResult =>
          Ok(
            rescindmentSendView(
              Rescindment().discardingErrors,
              sent = alertsResult.sent.toString,
              failed = alertsResult.failed.toString
            )))
  }
}
