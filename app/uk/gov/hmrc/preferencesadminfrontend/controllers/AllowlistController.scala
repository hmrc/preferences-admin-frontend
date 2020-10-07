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

import javax.inject.Inject
import play.api.i18n.I18nSupport
import play.api.libs.json.Json
import play.api.mvc.{ Action, AnyContent, MessagesControllerComponents }
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import uk.gov.hmrc.preferencesadminfrontend.config.AppConfig
import uk.gov.hmrc.preferencesadminfrontend.connectors.MessageConnector
import uk.gov.hmrc.preferencesadminfrontend.model.Allowlist._
import uk.gov.hmrc.preferencesadminfrontend.model.{ Allowlist, AllowlistEntry }
import uk.gov.hmrc.preferencesadminfrontend.views.html.{ allowlist_add, allowlist_show, error_template }

import scala.concurrent.{ ExecutionContext, Future }

class AllowlistController @Inject()(messageConnector: MessageConnector, mcc: MessagesControllerComponents)(implicit appConfig: AppConfig, ec: ExecutionContext)
    extends FrontendController(mcc) with I18nSupport {

  def showAllowlistPage: Action[AnyContent] = AuthorisedAction.async { implicit request => implicit user =>
    messageConnector.getAllowlist.map(response =>
      response.status match {
        case OK =>
          Json.parse(response.body).validate[Allowlist].asOpt match {
            case Some(allowlist) => Ok(allowlist_show(allowlist))
            case None            => BadGateway(error_template("Error", "There was an error:", "The allowlist does not appear to be valid.", appConfig))
          }
        case _ => BadGateway(error_template("Error", "There was an error:", response.body, appConfig))
    })
  }

  def addFormId: Action[AnyContent] = AuthorisedAction.async { implicit request => implicit user =>
    Future.successful(Ok(uk.gov.hmrc.preferencesadminfrontend.views.html.allowlist_add(AllowlistEntry())))
  }

  def confirmAdd: Action[AnyContent] = AuthorisedAction.async { implicit request => implicit user =>
    AllowlistEntry()
      .bindFromRequest()
      .fold(
        formWithErrors => {
          Future.successful(BadRequest(allowlist_add(formWithErrors)))
        },
        addEntry => {
          messageConnector
            .addFormIdToAllowlist(addEntry)
            .map(response =>
              response.status match {
                case CREATED => Redirect(routes.AllowlistController.showAllowlistPage())
                case _       => BadGateway(error_template("Error", "There was an error:", response.body, appConfig))
            })
        }
      )
  }

  def deleteFormId(formId: String): Action[AnyContent] = AuthorisedAction.async { implicit request => implicit user =>
    Future.successful(Ok(uk.gov.hmrc.preferencesadminfrontend.views.html.allowlist_delete(AllowlistEntry().fill(AllowlistEntry(formId, "")))))
  }

  def confirmDelete: Action[AnyContent] = AuthorisedAction.async { implicit request => implicit user =>
    AllowlistEntry()
      .bindFromRequest()
      .fold(
        formWithErrors => {
          Future.successful(BadRequest(uk.gov.hmrc.preferencesadminfrontend.views.html.allowlist_delete(formWithErrors)))
        },
        deleteEntry => {
          messageConnector
            .deleteFormIdFromAllowlist(deleteEntry)
            .map(response =>
              response.status match {
                case OK => Redirect(routes.AllowlistController.showAllowlistPage())
                case _  => BadGateway(error_template("Error", "There was an error:", response.body, appConfig))
            })
        }
      )
  }

}
