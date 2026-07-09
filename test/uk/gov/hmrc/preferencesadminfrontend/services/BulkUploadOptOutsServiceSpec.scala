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
import org.apache.pekko.stream.scaladsl.Framing.FramingException
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
import uk.gov.hmrc.preferencesadminfrontend.services.model.csv.UploadedBulKOptOutNinos

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
    case right: Right[_, _] =>
      right.value match {
        case entries: Seq[_] => entries.mkString(",\n").stripSuffix("\n")
        case _               => right.toString
      }

    case o: Any => o.toString
  }

  "readBulkOptOutsFromFile" should {

    "return an error if the file is invalid" in {
      val csvContent =
        s"""${"a".repeat(CsvReader.FrameLength + 1).mkString}
           |""".stripMargin

      val tempFile: Path = Files.createTempFile("test-upload", ".csv")
      Files.write(tempFile, csvContent.getBytes("UTF-8"))

      try {
        val eventualErrorOrCvBulkOptOutCsvDataList =
          bulkUploadOptOutsService.readNinoBulkOptOutsFromFile(tempFile)
        whenReady(eventualErrorOrCvBulkOptOutCsvDataList) { errorOrCvOptOutCsvDataList =>
          errorOrCvOptOutCsvDataList.left.map(_.getClass) mustBe Left(classOf[FramingException])

        }
      } finally Files.deleteIfExists(tempFile)
    }

    "parse an empty csv file returning an empty list" in {
      val csvContent = ""

      val tempFile: Path = Files.createTempFile("test-upload", ".csv")
      Files.write(tempFile, csvContent.getBytes("UTF-8"))

      try {
        val eventualCvBulkOptOutCsvDataList =
          bulkUploadOptOutsService.readNinoBulkOptOutsFromFile(tempFile)
        whenReady(eventualCvBulkOptOutCsvDataList) { cvOptOutCsvDataList =>
          cvOptOutCsvDataList mustBe Right(UploadedBulKOptOutNinos.empty)
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
          |YY336119C
          |""".stripMargin

      val tempFile: Path = Files.createTempFile("test-upload", ".csv")
      Files.write(tempFile, csvContent.getBytes("UTF-8"))

      try {
        val cvOptOutCsvDataList =
          bulkUploadOptOutsService.readNinoBulkOptOutsFromFile(tempFile).futureValue

        cvOptOutCsvDataList mustBe Right(
          UploadedBulKOptOutNinos(
            List(
              Right("YY336119A"),
              Right("YY336119B"),
              Left("YY336119A, unexpected value"),
              Left(", YY336119A"),
              Right("YY336119C")
            )
          )
        )

      } finally Files.deleteIfExists(tempFile)
    }
  }

  "processBulkOptOuts" should {
    "return an empty list when no ninos are passed" in {
      val ninos = List.empty
      val results: Seq[BulkOptOutResult] = bulkUploadOptOutsService.processBulkOptOuts(ninos).futureValue

      results mustBe List.empty
    }

    "return informative results handling errors gracefully" in {

      val ninos = List(
        "AA090601A",
        "AA090602A",
        "invalidformat1",
        "AA090603A",
        "AA090604A",
        "AA090605A",
        "AA090606A",
        "AA090607A",
        "invalidformat2",
        "AA090608A",
        "AA090609A",
        "AA090601B"
      )

      type PossibleOutCome = OptOutResult | Throwable

      def createResult(possibleOutCome: PossibleOutCome): Future[OptOutResult] =
        possibleOutCome match {
          case optOutResult: OptOutResult => Future.successful(optOutResult)
          case error: Throwable           => Future.failed(error)
        }

      when(mockEntityResolverConnector.optOut(TaxIdentifier.ninoIdentifier("AA090601A")))
        .thenReturn(createResult(OptedOut))

      when(mockEntityResolverConnector.optOut(TaxIdentifier.ninoIdentifier("AA090602A")))
        .thenReturn(createResult(AlreadyOptedOut))

      when(mockEntityResolverConnector.optOut(TaxIdentifier.ninoIdentifier("AA090603A")))
        .thenReturn(createResult(AlreadyOptedOut))

      when(mockEntityResolverConnector.optOut(TaxIdentifier.ninoIdentifier("AA090604A")))
        .thenReturn(createResult(PreferenceNotFound))

      when(mockEntityResolverConnector.optOut(TaxIdentifier.ninoIdentifier("AA090605A")))
        .thenReturn(createResult(PreferenceNotFound))

      when(mockEntityResolverConnector.optOut(TaxIdentifier.ninoIdentifier("AA090606A")))
        .thenReturn(createResult(new RuntimeException("error1")))

      when(mockEntityResolverConnector.optOut(TaxIdentifier.ninoIdentifier("AA090607A")))
        .thenReturn(createResult(OptedOut))

      when(mockEntityResolverConnector.optOut(TaxIdentifier.ninoIdentifier("AA090608A")))
        .thenReturn(createResult(new RuntimeException("error2")))

      when(mockEntityResolverConnector.optOut(TaxIdentifier.ninoIdentifier("AA090609A")))
        .thenReturn(createResult(OptedOut))

      when(mockEntityResolverConnector.optOut(TaxIdentifier.ninoIdentifier("AA090601B")))
        .thenReturn(createResult(PreferenceNotFound))

      val results: Seq[BulkOptOutResult] = bulkUploadOptOutsService.processBulkOptOuts(ninos).futureValue
      results mustBe List(
        InvalidNinoBulkOptOutResult("invalidformat1"),
        InvalidNinoBulkOptOutResult("invalidformat2"),
        ProcessedBulkOptOutResult("AA090601A", OptedOut),
        ProcessedBulkOptOutResult("AA090602A", AlreadyOptedOut),
        ProcessedBulkOptOutResult("AA090603A", AlreadyOptedOut),
        ProcessedBulkOptOutResult("AA090604A", PreferenceNotFound),
        ProcessedBulkOptOutResult("AA090605A", PreferenceNotFound),
        FailedCallBulkOptOutResult("AA090606A"),
        ProcessedBulkOptOutResult("AA090607A", OptedOut),
        FailedCallBulkOptOutResult("AA090608A"),
        ProcessedBulkOptOutResult("AA090609A", OptedOut),
        ProcessedBulkOptOutResult("AA090601B", PreferenceNotFound)
      )
    }

  }
}
