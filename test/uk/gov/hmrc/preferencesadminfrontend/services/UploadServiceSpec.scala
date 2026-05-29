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
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.test.Helpers.*
import uk.gov.hmrc.preferencesadminfrontend.services.model.CsvData

import java.nio.file.{ Files, Path }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class UploadServiceSpec extends PlaySpec with MockitoSugar with ScalaFutures {

  implicit val system: ActorSystem = ActorSystem("UploadServiceSpec")
  implicit val mat: Materializer = Materializer(system)

  val service = new UploadService()

  "UploadService" should {

    "readFromFile" should {
      "successfully parse a valid CSV file and skip invalid rows" in {
        val csvContent =
          """data1, data2, data3
            |
            |invalidLine1, invalidLine2
            |data4, data5, data6
            |""".stripMargin

        val tempFile: Path = Files.createTempFile("test-upload", ".csv")
        Files.write(tempFile, csvContent.getBytes("UTF-8"))

        try {
          val resultFuture: Future[List[CsvData]] = service.readFromFile(tempFile)
          whenReady(resultFuture) { records =>
            records must have size 2
            records.head mustBe CsvData("data1", "data2", "data3")
            records(1) mustBe CsvData("data4", "data5", "data6")
          }
        } finally Files.deleteIfExists(tempFile)
      }
    }

    "process" should {
      "return Ok when all records are processed successfully" in {
        val records = List(
          CsvData("A", "B", "C"),
          CsvData("X", "Y", "Z")
        )

        val resultFuture = service.process(records)
        status(resultFuture) mustBe OK
        contentAsString(resultFuture) mustBe "Processing complete."
      }
    }
  }
}
