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

package uk.gov.hmrc.preferencesadminfrontend.services

import org.apache.pekko.stream.Materializer
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.mvc.Results.Ok

import java.nio.file.Path
import javax.inject.Inject
import scala.concurrent.{ ExecutionContext, Future }

class BulkUploadOptOutsService @Inject() (csvReader: CsvReader) {

  def readNinoBulkOptOutsFromFile(
    path: Path
  )(implicit mat: Materializer): Future[List[Either[String, String]]] = {
    val extractCsvData: PartialFunction[Any, Either[String, String]] = {
      case line: String if line.split(",").map(_.trim).length >= 1 =>
        val cols = line.split(",").map(_.trim)
        val ninoValue = cols(0)
        val doesNotHaveExtraCoulmnValues = !cols.zipWithIndex.exists { case (value, index) =>
          index > 0 & value.nonEmpty
        }

        if (doesNotHaveExtraCoulmnValues) {
          Right(ninoValue)
        } else {
          Left(s"$line")
        }

    }

    csvReader.readFromFile(path, extractCsvData)
  }

  def processBulkOptOuts(records: List[String])(implicit ec: ExecutionContext): Future[Result] =
    Future.successful {
      val jsonPayload = Json.toJson(records)
      Ok(s"Processing ${records.size} records.\n${Json.prettyPrint(jsonPayload)}")
    }
}
