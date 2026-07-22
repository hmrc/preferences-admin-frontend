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

package uk.gov.hmrc.preferencesadminfrontend.views.messages

object BulkUploadErrorMessages {

  val duplicateNinos = "The following uploaded Ninos were duplicates, so were attempted once"
  val invalidFormatNinos = "The following uploaded entries were in an invalid Nino format"
  val notFullyOptedIn = "The following uploaded Ninos were not fully opted in"
  val notFoundNinos = "The following uploaded Ninos do not exist"
  val unexpectedFailureNinos = "The following Ninos failed for unexpected reasons"
  val invalidFileFormat =
    "Invalid File Format. The uploaded file format could not be processed. Please enter a valid .csv file format."
  val tooManyUploadedNinosFormat = "Too many Ninos were uploaded (%d)"
  val noUploadedNinos = "The uploaded file had no Ninos"

}
