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

package uk.gov.hmrc.preferencesadminfrontend.model

import play.api.data.Forms.{ mapping, nonEmptyText, optional, text }
import play.api.data.{ Form, Forms, Mapping }
import play.api.data.validation.{ Constraint, Invalid, Valid, ValidationError }
import play.api.libs.json.{ Json, OWrites }
import uk.gov.hmrc.preferencesadminfrontend.model.AllowlistEntry.{ nonEmptyTextWithError, reasonTextConstraint }
import uk.gov.hmrc.preferencesadminfrontend.services.MessageStatus

case class SendMessage(utrs: String)

object SendMessage {

  def utrList(utrs: String) = utrs.trim.split("\r").toList.map(_.trim)

  val sizeConstraint: Constraint[String] = Constraint("constraints.size")({ size =>
    if (size.trim.isEmpty) {
      Invalid("Can't send empty content")
    } else if (utrList(size).length > 100)
      Invalid("Can't send more than 100 UTR's at once")
    else {
      Valid
    }
  })

  implicit val writes: OWrites[SendMessage] = Json.writes[SendMessage]

  def apply(): Form[SendMessage] = Form(
    mapping(
      "utrs" -> text.verifying(sizeConstraint)
    )(SendMessage.apply)(SendMessage.unapply)
  )

}
