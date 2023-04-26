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

import java.util.UUID
import play.api.libs.json._

case class EntityId(value: String) {
  override def toString = value
}

object EntityId {

  def generate(): EntityId = EntityId(UUID.randomUUID().toString)

  private val read = new Reads[EntityId] {
    override def reads(json: JsValue): JsResult[EntityId] = json match {
      case JsString(s) => JsSuccess(EntityId(s))
      case _           => JsError("No entityId")
    }
  }

  private val write = new Writes[EntityId] {
    override def writes(e: EntityId): JsValue = JsString(e.value)
  }

  implicit val formats: Format[EntityId] = Format(read, write)
}
