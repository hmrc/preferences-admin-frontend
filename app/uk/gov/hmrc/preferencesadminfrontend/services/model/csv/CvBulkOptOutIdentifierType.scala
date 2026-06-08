/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.preferencesadminfrontend.services.model.csv

import play.api.libs.json.*
import uk.gov.hmrc.preferencesadminfrontend.services.model.*

object CvBulkOptOutIdentifierType {
  private val allValues = List(NinoIdentifierType, SAUTRIdentifierType, ITSAIdentifierType, EmailIdentifierType)

  def fromString(string: String): Option[CvBulkOptOutIdentifierType] =
    allValues.find(_.value == string)

  implicit val identifierTypesRead: Reads[CvBulkOptOutIdentifierType] = new Reads[CvBulkOptOutIdentifierType] {
    override def reads(json: JsValue): JsResult[CvBulkOptOutIdentifierType] =
      json.validate[String].flatMap { incomingValue =>
        allValues
          .find(_.value == incomingValue)
          .map { identifierType =>
            JsSuccess(identifierType)
          }
          .getOrElse(
            JsError(s"'$incomingValue' is invalid, valid values are ${allValues.mkString(", ")}")
          )
      }
  }

  implicit val identifierTypesWrites: Writes[CvBulkOptOutIdentifierType] = new Writes[CvBulkOptOutIdentifierType] {
    override def writes(o: CvBulkOptOutIdentifierType): JsValue = JsString(o.value)
  }
}

sealed trait CvBulkOptOutIdentifierType(val value: String)
case object NinoIdentifierType extends CvBulkOptOutIdentifierType("nino")
case object SAUTRIdentifierType extends CvBulkOptOutIdentifierType("sautr")
case object ITSAIdentifierType extends CvBulkOptOutIdentifierType("itsa")
case object EmailIdentifierType extends CvBulkOptOutIdentifierType("email")
