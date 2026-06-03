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
import org.mockito.ArgumentMatchers.*
import org.mockito.Mockito.*
import org.scalatest.time.SpanSugar.convertIntToGrainOfTime
import org.scalatestplus.play.PlaySpec
import play.api.test.Helpers.*
import play.api.mvc.Results.{ BadRequest, InternalServerError, Ok }
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.preferencesadminfrontend.connectors.ChannelPreferencesConnector
import uk.gov.hmrc.preferencesadminfrontend.services.model.CsvData

import java.nio.file.{ Files, Path }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class UploadServiceSpec extends PlaySpec with MockitoSugar with ScalaFutures {

  implicit val system: ActorSystem = ActorSystem("UploadServiceSpec")
  implicit val mat: Materializer = Materializer(system)
  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val defaultPatience: PatienceConfig = PatienceConfig(timeout = 5.seconds, interval = 100.millis)
  val mockConnector: ChannelPreferencesConnector = mock[ChannelPreferencesConnector]
  val serviceUnderTest = new UploadService(mockConnector)

  "UploadService.readFromFile" should {
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
        val resultFuture: Future[List[CsvData]] = serviceUnderTest.readFromFile(tempFile)
        whenReady(resultFuture) { records =>
          records must have size 2
          records.head mustBe CsvData("data1", "data2", "data3")
          records(1) mustBe CsvData("data4", "data5", "data6")
        }
      } finally Files.deleteIfExists(tempFile)
    }
  }

  "UploadService.process" should {

    "successfully process all records and return an Ok result" in {
      val records = List(CsvData("1", "2", "3"), CsvData("4", "5", "6"), CsvData("A", "B", "C"))

      when(mockConnector.process(any[CsvData])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Ok("Success")))

      val resultFuture = serviceUnderTest.process(records)

      whenReady(resultFuture) { result =>
        result.header.status mustBe OK
        contentAsString(Future.successful(result)) must include("Processed 3 records successfully.")
      }

      verify(mockConnector, times(3)).process(any[CsvData])(any[HeaderCarrier])
    }

    "handle an empty list of records gracefully" in {
      reset(mockConnector)

      val records = List.empty[CsvData]

      val resultFuture = serviceUnderTest.process(records)

      whenReady(resultFuture) { result =>
        result.header.status mustBe OK
        contentAsString(Future.successful(result)) must include("Processed 0 records successfully.")
      }

      verifyNoInteractions(mockConnector)
    }

    "fail the entire stream processing if a downstream call fails" in {
      val records = List(CsvData("1", "2", "3"), CsvData("4", "5", "6"))

      when(mockConnector.process(any[CsvData])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Ok("Success")))
        .thenReturn(Future.failed(new RuntimeException("Downstream crash!")))

      val resultFuture = serviceUnderTest.process(records)

      whenReady(resultFuture.failed) { exception =>
        exception mustBe a[RuntimeException]
        exception.getMessage mustBe "Downstream crash!"
      }
    }

    "not fail the entire stream processing if a downstream call returns error status" in {
      val records = List(CsvData("1", "2", "3"), CsvData("4", "5", "6"), CsvData("A", "B", "C"))

      when(mockConnector.process(any[CsvData])(any[HeaderCarrier]))
        .thenReturn(Future.successful(BadRequest("error")))
        .thenReturn(Future.successful(Ok("success")))
        .thenReturn(Future.successful(InternalServerError("failed")))

      val resultFuture = serviceUnderTest.process(records)

      whenReady(resultFuture) { result =>
        result.header.status mustBe OK
        contentAsString(Future.successful(result)) must include("Processed 3 records successfully.")
      }
    }
  }
}
