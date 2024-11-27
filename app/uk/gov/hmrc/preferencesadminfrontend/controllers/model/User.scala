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

package uk.gov.hmrc.preferencesadminfrontend.controllers.model

enum Role {
  case Admin, Generic
}

object Role {
  def fromString(role: String): Role = role.trim.toLowerCase match {
    case "admin"   => Role.Admin
    case "generic" => Role.Generic
    case _         => throw IllegalArgumentException(s"Invalid argument for the role $role")
  }
}

case class User(username: String, password: String, roles: List[Role] = List.empty[Role])

object User {
  val sessionKey = "userId"
  def apply(username: String, password: String): User = User(username, password)

  def apply(username: String, password: String, roles: List[Role]): User = User(username, password, roles)

  def unapply(u: User): Option[(String, String)] = Some(u.username, u.password)

}
