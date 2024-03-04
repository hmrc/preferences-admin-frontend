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

package uk.gov.hmrc.preferencesadminfrontend.services.model

import play.api.libs.json._

import java.time.ZonedDateTime

case class Preference(
  entityId: Option[EntityId],
  genericPaperless: Boolean,
  genericUpdatedAt: Option[ZonedDateTime],
  email: Option[Email],
  taxIdentifiers: Seq[TaxIdentifier])

object Preference {
  implicit val dateFormatDefault: Format[ZonedDateTime] = new Format[ZonedDateTime] {
    override def reads(json: JsValue): JsResult[ZonedDateTime] = Reads.DefaultZonedDateTimeReads.reads(json)
    override def writes(o: ZonedDateTime): JsValue = Writes.ZonedDateTimeEpochMilliWrites.writes(o)
  }
  implicit val format: OFormat[Preference] = Json.format[Preference]
}
