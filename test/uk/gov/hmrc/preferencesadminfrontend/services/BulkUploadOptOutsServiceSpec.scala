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

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.Materializer
import org.scalactic.Prettifier
import org.scalatest.concurrent.{ IntegrationPatience, ScalaFutures }
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.test.Helpers.*
import uk.gov.hmrc.http.HeaderCarrier

import java.nio.file.{ Files, Path }
import scala.concurrent.{ ExecutionContextExecutor, Future }

class BulkUploadOptOutsServiceSpec extends AnyWordSpecLike with Matchers with ScalaFutures with IntegrationPatience {

  implicit val system: ActorSystem = ActorSystem("UploadServiceSpec")

  implicit val mat: Materializer = Materializer(system)
  implicit val executionContext: ExecutionContextExecutor = mat.executionContext
  implicit val hc: HeaderCarrier = HeaderCarrier()

  private val bulkUploadOptOutsService = new BulkUploadOptOutsService(new CsvReader)

  "readBulkOptOutsFromFile" should {
    "parse an empty csv file returning an empty list" in {
      val csvContent = ""

      val tempFile: Path = Files.createTempFile("test-upload", ".csv")
      Files.write(tempFile, csvContent.getBytes("UTF-8"))

      try {
        val eventualCvBulkOptOutCsvDataList: Future[List[Either[String, String]]] =
          bulkUploadOptOutsService.readNinoBulkOptOutsFromFile(tempFile)
        whenReady(eventualCvBulkOptOutCsvDataList) { cvOptOutCsvDataList =>
          cvOptOutCsvDataList mustBe List.empty
        }
      } finally Files.deleteIfExists(tempFile)
    }

    "parse a valid CSV file returning failed rows and successful rows in a processable format ignoring extra empty values" in {
      val csvContent =
        """nino1
          |
          |nino2,
          |nino3, unexpected value
          |, nino4
          |nino5
          |""".stripMargin

      val tempFile: Path = Files.createTempFile("test-upload", ".csv")
      Files.write(tempFile, csvContent.getBytes("UTF-8"))

      try {
        val eventualCvBulkOptOutCsvDataList: Future[List[Either[String, String]]] =
          bulkUploadOptOutsService.readNinoBulkOptOutsFromFile(tempFile)

        whenReady(eventualCvBulkOptOutCsvDataList) { cvOptOutCsvDataList =>
          // not used in comparison but in showing a clear diff on failure
          implicit val prettifier: Prettifier = {
            case entries: List[_] => entries.mkString(",\n").stripSuffix("\n")
            case o: Any           => o.toString
          }

          cvOptOutCsvDataList mustBe List(
            Right("nino1"),
            Right("nino2"),
            Left("nino3, unexpected value"),
            Left(", nino4"),
            Right("nino5")
          )
        }
      } finally Files.deleteIfExists(tempFile)
    }
  }

//  "processBulkOptOuts" should {
//    "return Ok when all records are processed successfully" in {
//      val records = List(
//        CvBulkOptOutCsvData(NinoIdentifierType, "B"),
//        CvBulkOptOutCsvData(ITSAIdentifierType, "Y")
//      )
//
//      val resultFuture = bulkUploadOptOutsService.processBulkOptOuts(records)
//      status(resultFuture) mustBe OK
//      val responseContent = contentAsString(resultFuture)
//
//      responseContent mustBe
//        """
//          |Processing 2 records.
//          |[ {
//          |  "identifierType" : "nino",
//          |  "value" : "B"
//          |}, {
//          |  "identifierType" : "itsa",
//          |  "value" : "Y"
//          |} ]""".stripMargin.trim
//    }
//  }
}
