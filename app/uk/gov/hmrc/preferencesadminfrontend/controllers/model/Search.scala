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

import play.api.data.Form
import play.api.data.Forms.{ mapping, nonEmptyText, text }
import uk.gov.hmrc.preferencesadminfrontend.services.model.TaxIdentifier

object Search {

  def apply() =
    Form[TaxIdentifier](
      mapping(
        "name" -> text
          .verifying(
            "error.name_invalid",
            name => name == "sautr" || name == "nino" || name == "email" || name == "HMRC-MTD-IT"
          ),
        "value" -> nonEmptyText
      )((name, value) => TaxIdentifier.apply(name, if (name == "HMRC-MTD-IT") value else value.toUpperCase))(
        TaxIdentifier.unapply
      )
    )
}
