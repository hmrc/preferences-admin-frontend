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

sealed abstract case class UploadedBulKOptOutNinos(
  distinctErrorOrOptOuts: List[Either[String, String]],
  duplicateValid: List[String]
)

object UploadedBulKOptOutNinos {

  val empty: UploadedBulKOptOutNinos = new UploadedBulKOptOutNinos(List.empty, List.empty) {}

  def apply(errorOrOptOutList: List[Either[String, String]]): UploadedBulKOptOutNinos = {

    val empty = (List.empty[Either[String, String]], List.empty[String])

    val orderPreserved = errorOrOptOutList.foldLeft(empty) { case ((distinctResults, distinct), current) =>
      current match {
        case Left(_) =>
          if (distinctResults.contains(current)) {
            (distinctResults, distinct)
          } else {
            (distinctResults :+ current, distinct)
          }

        case Right(validValue) =>
          if (distinctResults.contains(current)) {
            (distinctResults, distinct :+ validValue)
          } else {
            (distinctResults :+ current, distinct)
          }
      }
    }

    new UploadedBulKOptOutNinos(orderPreserved._1, orderPreserved._2.distinct) {}
  }
}
