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

import javax.inject.{ Inject, Singleton }
import play.api.Configuration
import play.api.data.Form
import play.api.data.Forms.{ mapping, _ }
import play.api.i18n.I18nSupport
import play.api.mvc._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.DataEvent
import uk.gov.hmrc.play.bootstrap.config.AppName
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import uk.gov.hmrc.preferencesadminfrontend.config.AppConfig
import uk.gov.hmrc.preferencesadminfrontend.controllers.model.User
import uk.gov.hmrc.preferencesadminfrontend.services.LoginService
import uk.gov.hmrc.time.DateTimeUtils

import scala.concurrent.{ ExecutionContext, Future }

@Singleton
class LoginController @Inject()(loginService: LoginService, auditConnector: AuditConnector, config: Configuration, mcc: MessagesControllerComponents)(
  implicit appConfig: AppConfig,
  ec: ExecutionContext)
    extends FrontendController(mcc) with I18nSupport {

  val showLoginPage = Action.async { implicit request =>
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers)

    val sessionUpdated = request.session + ("ts" -> DateTimeUtils.now.getMillis.toString)
    Future.successful(Ok(uk.gov.hmrc.preferencesadminfrontend.views.html.login(userForm)).withSession(sessionUpdated))
  }

  val login = Action.async { implicit request =>
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers)

    userForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(uk.gov.hmrc.preferencesadminfrontend.views.html.login(formWithErrors))),
      userData => {
        if (loginService.isAuthorised(userData)) {
          auditConnector.sendEvent(createLoginEvent(userData.username, true))
          val sessionUpdated = request.session + (User.sessionKey -> userData.username) + ("ts" -> DateTimeUtils.now.getMillis.toString)
          Future.successful(Redirect(routes.HomeController.showHomePage()).withSession(sessionUpdated))
        } else {
          auditConnector.sendEvent(createLoginEvent(userData.username, false))
          val userFormWithErrors = userForm.fill(userData).withGlobalError("error.credentials.invalid")
          Future.successful(Unauthorized(uk.gov.hmrc.preferencesadminfrontend.views.html.login(userFormWithErrors)))
        }
      }
    )
  }

  val logout = AuthorisedAction.async { implicit request => user =>
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers)
    auditConnector.sendEvent(createLogoutEvent(user.username))
    Future.successful(Redirect(routes.LoginController.showLoginPage()).withSession(Session()))
  }

  def createLoginEvent(username: String, successful: Boolean) = DataEvent(
    auditSource = AppName.fromConfiguration(config),
    auditType = if (successful) "TxSucceeded" else "TxFailed",
    detail = Map("user"          -> username),
    tags = Map("transactionName" -> "Login")
  )

  def createLogoutEvent(username: String) = DataEvent(
    auditSource = AppName.fromConfiguration(config),
    auditType = "TxSucceeded",
    detail = Map("user"          -> username),
    tags = Map("transactionName" -> "Logout")
  )

  val userForm = Form(
    mapping(
      "username" -> nonEmptyText,
      "password" -> nonEmptyText
    )(User.apply)(User.unapply)
  )
}
