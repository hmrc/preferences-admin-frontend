/*
 * Copyright 2024 HM Revenue & Customs
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

import play.api.mvc.{ AnyContent, Request, Result }
import uk.gov.hmrc.preferencesadminfrontend.controllers.model.User

import javax.inject.Inject
import scala.concurrent.Future

trait RoleAuthorisedAction @Inject() (val authorisedActionService: AuthorisedAction) {
  def role: Role
  def authorisedAction(block: Request[AnyContent] => User => Future[Result]) =
    authorisedActionService.async(role)(block)
}

enum Role {
  case Admin, Generic
}

object Role {
  def fromString(role: String): Role = role.trim.toLowerCase match {
    case "admin"   => Admin
    case "generic" => Generic
    case _         => throw IllegalArgumentException(s"Invalid argument for the role $role")
  }
}
