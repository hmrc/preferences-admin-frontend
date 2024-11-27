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

import play.api.mvc.{ Action, AnyContent, MessagesBaseController, MessagesControllerComponents, Request, Result }
import uk.gov.hmrc.preferencesadminfrontend.controllers.model.Role.Generic
import uk.gov.hmrc.preferencesadminfrontend.controllers.model.{ Role, User }
import uk.gov.hmrc.preferencesadminfrontend.services.LoginService

import javax.inject.Inject
import scala.concurrent.Future

class AuthorisedAction @Inject() (loginService: LoginService, val controllerComponents: MessagesControllerComponents)
    extends MessagesBaseController {

  def async(block: Request[AnyContent] => User => Future[Result]): Action[AnyContent] = async(Generic)(block)

  def async(role: Role)(block: Request[AnyContent] => User => Future[Result]): Action[AnyContent] =
    Action.async { implicit request =>
      val user = request.session.get(User.sessionKey).map(name => User(name, ""))

      user match {
        case Some(user) if loginService.hasRequiredRole(user, role) => block(request)(user)
        case _ => Future.successful(play.api.mvc.Results.Redirect(routes.LoginController.showLoginPage()))
      }

    }
}
