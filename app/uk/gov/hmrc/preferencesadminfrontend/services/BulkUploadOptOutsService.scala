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
import play.api.mvc.Result
import play.api.mvc.Results.Ok
import play.api.libs.json.Json
import uk.gov.hmrc.preferencesadminfrontend.services.model.csv.{ CvBulkOptOutCsvData, CvBulkOptOutIdentifierType, EmailIdentifierType }

import java.nio.file.Path
import javax.inject.Inject
import scala.concurrent.{ ExecutionContext, Future }

class BulkUploadOptOutsService @Inject() (csvReader: CsvReader) {

  def readBulkOptOutsFromFile(
    path: Path
  )(implicit mat: Materializer): Future[List[Either[String, CvBulkOptOutCsvData]]] = {
    val extractCsvData: PartialFunction[Any, Either[String, CvBulkOptOutCsvData]] = {
      case line: String if line.split(",").map(_.trim).length >= 1 =>
        val cols = line.split(",").map(_.trim)
        val typeText = cols(0)
        val doesNotHaveExtraCoulmnValues = !cols.zipWithIndex.exists { case (value, index) =>
          index > 1 & value.nonEmpty
        }

        CvBulkOptOutIdentifierType.fromString(typeText) match {
          case Some(identifierType: CvBulkOptOutIdentifierType)
              if cols.length > 1 && cols(1).nonEmpty && doesNotHaveExtraCoulmnValues =>
            processIdentifiedLine(line, identifierType, cols(1))

          case _ =>
            Left(line)
        }
    }

    csvReader.readFromFile(path, extractCsvData)
  }

  private def processIdentifiedLine(
    line: String,
    identifierType: CvBulkOptOutIdentifierType,
    value: String
  ): Either[String, CvBulkOptOutCsvData] =
    identifierType match {
      case EmailIdentifierType =>
        validateEmail(value) match {
          case Left(invalidEmail) =>
            Left(s"$line (invalid email address)")
          case Right(validEmail) =>
            Right(CvBulkOptOutCsvData(identifierType, validEmail))
        }
      case _ =>
        Right(CvBulkOptOutCsvData(identifierType, value))

    }

  private def validateEmail(value: String): Either[String, String] = {
    val validEmail = """^([a-zA-Z0-9.!#$%&’'*+/=?^_`{|}~-]+)@([a-zA-Z0-9-]+(?:\.[a-zA-Z0-9-]+)*)$""".r

    value match {
      case validEmail(_, _) => Right(value)
      case invalidEmail     => Left(invalidEmail)
    }
  }

  def processBulkOptOuts(records: List[CvBulkOptOutCsvData])(implicit ec: ExecutionContext): Future[Result] =
    Future.successful {
      val jsonPayload = Json.toJson(records)
      Ok(s"Processing ${records.size} records.\n${Json.prettyPrint(jsonPayload)}")
    }

}
