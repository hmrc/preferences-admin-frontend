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

package uk.gov.hmrc.preferencesadminfrontend.services.model

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.libs.json.{ JsString, JsSuccess }

class CvBulkOptOutIdentifierTypeSpec extends AnyWordSpecLike with Matchers {

  "fromString" should {
    "map to NinoIdentifierType" in {
      CvBulkOptOutIdentifierType.fromString("nino") mustBe Some(NinoIdentifierType)
    }

    "map to SAUTRIdentifierType" in {
      CvBulkOptOutIdentifierType.fromString("sautr") mustBe Some(SAUTRIdentifierType)
    }

    "map to ITSAIdentifierType" in {
      CvBulkOptOutIdentifierType.fromString("itsa") mustBe Some(ITSAIdentifierType)
    }

    "map to EmailIdentifierType" in {
      CvBulkOptOutIdentifierType.fromString("email") mustBe Some(EmailIdentifierType)
    }

    "not map when an unknown entry is passed" in {
      CvBulkOptOutIdentifierType.fromString("x") mustBe None
    }
  }

  "writes" should {

    "write out NinoIdentifierType" in {
      CvBulkOptOutIdentifierType.identifierTypesWrites.writes(NinoIdentifierType) mustBe JsString("nino")
    }

    "write out SAUTRIdentifierType" in {
      CvBulkOptOutIdentifierType.identifierTypesWrites.writes(SAUTRIdentifierType) mustBe JsString("sautr")
    }

    "write out ITSAIdentifierType" in {
      CvBulkOptOutIdentifierType.identifierTypesWrites.writes(ITSAIdentifierType) mustBe JsString("itsa")
    }

    "write out EmailIdentifierType" in {
      CvBulkOptOutIdentifierType.identifierTypesWrites.writes(EmailIdentifierType) mustBe JsString("email")
    }
  }

  "reads" should {
    "read for NinoIdentifierType" in {
      CvBulkOptOutIdentifierType.identifierTypesRead.reads(JsString("nino")) mustBe JsSuccess(NinoIdentifierType)
    }

    "read for SAUTRIdentifierType" in {
      CvBulkOptOutIdentifierType.identifierTypesRead.reads(JsString("sautr")) mustBe JsSuccess(SAUTRIdentifierType)
    }

    "read for ITSAIdentifierType" in {
      CvBulkOptOutIdentifierType.identifierTypesRead.reads(JsString("itsa")) mustBe JsSuccess(ITSAIdentifierType)
    }

    "read for EmailIdentifierType" in {
      CvBulkOptOutIdentifierType.identifierTypesRead.reads(JsString("email")) mustBe JsSuccess(EmailIdentifierType)
    }
  }

}
