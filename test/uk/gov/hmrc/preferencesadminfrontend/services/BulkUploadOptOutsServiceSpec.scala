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
import org.mockito.Mockito.when
import org.scalactic.Prettifier
import org.scalatest.concurrent.{ IntegrationPatience, ScalaFutures }
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.preferencesadminfrontend.config.BulkOptOutsConfig
import uk.gov.hmrc.preferencesadminfrontend.connectors.*
import uk.gov.hmrc.preferencesadminfrontend.services.model.TaxIdentifier

import java.nio.file.{ Files, Path }
import scala.concurrent.{ ExecutionContextExecutor, Future }

class BulkUploadOptOutsServiceSpec
    extends AnyWordSpecLike with Matchers with ScalaFutures with IntegrationPatience with MockitoSugar {

  implicit val system: ActorSystem = ActorSystem("UploadServiceSpec")

  implicit val mat: Materializer = Materializer(system)
  implicit val executionContext: ExecutionContextExecutor = mat.executionContext
  implicit val hc: HeaderCarrier = HeaderCarrier()

  private val mockEntityResolverConnector: EntityResolverConnector = mock[EntityResolverConnector]
  private val bulkUploadOptOutsService =
    new BulkUploadOptOutsService(
      new CsvReader,
      mockEntityResolverConnector,
      BulkOptOutsConfig(maxUploadEntries = 100, maxOptOutsPerSecond = 10)
    )

  // not used in comparison but in showing a clear diff on failure
  private implicit val prettifier: Prettifier = {
    case entries: Seq[_] => entries.mkString(",\n").stripSuffix("\n")
    case o: Any          => o.toString
  }

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
        """YY336119A
          |
          |YY336119B,
          |YY336119A, unexpected value
          |, YY336119A
          |YY336119C,
          |invalidformat
          |""".stripMargin

      val tempFile: Path = Files.createTempFile("test-upload", ".csv")
      Files.write(tempFile, csvContent.getBytes("UTF-8"))

      try {
        val cvOptOutCsvDataList =
          bulkUploadOptOutsService.readNinoBulkOptOutsFromFile(tempFile).futureValue

        cvOptOutCsvDataList mustBe List(
          Right("YY336119A"),
          Right("YY336119B"),
          Left("YY336119A, unexpected value"),
          Left(", YY336119A"),
          Right("YY336119C"),
          Left("invalidformat")
        )

      } finally Files.deleteIfExists(tempFile)
    }
  }

  "processBulkOptOuts" should {
    "return an empty list when no ninos are passed" in {
      val ninos = List()
      val results: Seq[BulkOptOutResult] = bulkUploadOptOutsService.processBulkOptOuts(ninos).futureValue

      results mustBe List.empty
    }

    "return informative results handling errors gracefully" in {
      val ninos = List(
        "nino1",
        "nino2",
        "nino3",
        "nino4",
        "nino5",
        "nino6",
        "nino7",
        "nino8",
        "nino9",
        "nino10"
      )

      type PossibleOutCome = OptOutResult | Throwable

      def createResult(possibleOutCome: PossibleOutCome): Future[OptOutResult] =
        possibleOutCome match {
          case optOutResult: OptOutResult => Future.successful(optOutResult)
          case error: Throwable           => Future.failed(error)
        }

      when(mockEntityResolverConnector.optOut(TaxIdentifier.ninoIdentifier("nino1")))
        .thenReturn(createResult(OptedOut))

      when(mockEntityResolverConnector.optOut(TaxIdentifier.ninoIdentifier("nino2")))
        .thenReturn(createResult(AlreadyOptedOut))

      when(mockEntityResolverConnector.optOut(TaxIdentifier.ninoIdentifier("nino3")))
        .thenReturn(createResult(AlreadyOptedOut))

      when(mockEntityResolverConnector.optOut(TaxIdentifier.ninoIdentifier("nino4")))
        .thenReturn(createResult(PreferenceNotFound))

      when(mockEntityResolverConnector.optOut(TaxIdentifier.ninoIdentifier("nino5")))
        .thenReturn(createResult(PreferenceNotFound))

      when(mockEntityResolverConnector.optOut(TaxIdentifier.ninoIdentifier("nino6")))
        .thenReturn(createResult(new RuntimeException("error1")))

      when(mockEntityResolverConnector.optOut(TaxIdentifier.ninoIdentifier("nino7")))
        .thenReturn(createResult(OptedOut))

      when(mockEntityResolverConnector.optOut(TaxIdentifier.ninoIdentifier("nino8")))
        .thenReturn(createResult(new RuntimeException("error2")))

      when(mockEntityResolverConnector.optOut(TaxIdentifier.ninoIdentifier("nino9")))
        .thenReturn(createResult(OptedOut))

      when(mockEntityResolverConnector.optOut(TaxIdentifier.ninoIdentifier("nino10")))
        .thenReturn(createResult(PreferenceNotFound))

      val results: Seq[BulkOptOutResult] = bulkUploadOptOutsService.processBulkOptOuts(ninos).futureValue
      results mustBe List(
        ProcessedBulkOptOutResult("nino1", OptedOut),
        ProcessedBulkOptOutResult("nino2", AlreadyOptedOut),
        ProcessedBulkOptOutResult("nino3", AlreadyOptedOut),
        ProcessedBulkOptOutResult("nino4", PreferenceNotFound),
        ProcessedBulkOptOutResult("nino5", PreferenceNotFound),
        FailedCallBulkOptOutResult("nino6"),
        ProcessedBulkOptOutResult("nino7", OptedOut),
        FailedCallBulkOptOutResult("nino8"),
        ProcessedBulkOptOutResult("nino9", OptedOut),
        ProcessedBulkOptOutResult("nino10", PreferenceNotFound)
      )
    }

  }
}
