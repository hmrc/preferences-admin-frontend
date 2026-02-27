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

package uk.gov.hmrc.preferencesadminfrontend.model

import org.scalatestplus.play.PlaySpec

import play.api.libs.json.Json
import uk.gov.hmrc.preferencesadminfrontend.model.{ GmcBatch, GmcBatchApproval }

class GmcBatchApprovalSpec extends PlaySpec {

  "GmcBatchApproval" must {

    "return Invalid for empty or illegal reasonText" in {
      GmcBatchApproval.reasonTextConstraint("") mustBe play.api.data.validation.Invalid("A reason is required")
      GmcBatchApproval.reasonTextConstraint("Invalid!") mustBe play.api.data.validation
        .Invalid("Invalid characters entered")
    }

    "serialize to and from Json" in {
      val approval = GmcBatchApproval("b", "f", "i", "t", "reason")
      Json.toJson(approval).as[GmcBatchApproval] mustBe approval
    }

    "bind and unbind correctly via Form" in {
      val data = Map(
        "batchId"    -> "b",
        "formId"     -> "f",
        "issueDate"  -> "i",
        "templateId" -> "t",
        "reasonText" -> "Valid Reason"
      )
      val form = GmcBatchApproval().bind(data)
      form.hasErrors mustBe false
      GmcBatchApproval().fill(form.get).data mustBe data
    }

    "be created from a GmcBatch object" in {
      val batch = GmcBatch("batch-123", "form-123", "2023-01-01", "template-123", None)
      val result = GmcBatchApproval(batch, "Manual Approval")

      result.batchId mustBe "batch-123"
      result.reasonText mustBe "Manual Approval"
    }
  }
}
