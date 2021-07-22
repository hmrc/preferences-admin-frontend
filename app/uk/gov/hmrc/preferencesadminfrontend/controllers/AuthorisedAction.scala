/*
 * Copyright 2021 HM Revenue & Customs
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

import play.api.mvc._
import uk.gov.hmrc.preferencesadminfrontend.controllers.model.User

import scala.concurrent.Future

object AuthorisedAction {

  def async(block: Request[AnyContent] => User => Future[Result]): Action[AnyContent] =
    Action.async { implicit request =>
      {
        val user = request.session.get(User.sessionKey).map(name => User(name, ""))

        user match {
          case Some(user) => block(request)(user)
          case _          => Future.successful(play.api.mvc.Results.Redirect(routes.LoginController.showLoginPage()))
        }
      }

    }
}
