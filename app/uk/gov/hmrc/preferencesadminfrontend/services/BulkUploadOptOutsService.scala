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
import org.apache.pekko.stream.scaladsl.{ Sink, Source }
import play.api.Logging
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.preferencesadminfrontend.config.BulkOptOutsConfig
import uk.gov.hmrc.preferencesadminfrontend.connectors.{ EntityResolverConnector, OptOutResult }
import uk.gov.hmrc.preferencesadminfrontend.services.model.TaxIdentifier

import java.nio.file.Path
import javax.inject.Inject
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ ExecutionContext, Future }
import scala.util.Try

sealed trait BulkOptOutResult
case class FailedCallBulkOptOutResult(nino: String) extends BulkOptOutResult
case class ProcessedBulkOptOutResult(nino: String, optOutResult: OptOutResult) extends BulkOptOutResult

class BulkUploadOptOutsService @Inject() (
  csvReader: CsvReader,
  entityResolverConnector: EntityResolverConnector,
  bulkOptOutsConfig: BulkOptOutsConfig
) extends Logging {

  def readNinoBulkOptOutsFromFile(
    path: Path
  )(implicit mat: Materializer): Future[List[Either[String, String]]] = {
    val extractCsvData: PartialFunction[Any, Either[String, String]] = {
      case line: String if line.split(",").map(_.trim).length >= 1 =>
        val cols = line.split(",").map(_.trim)
        val ninoValue = cols(0)
        val hasValue = !cols.zipWithIndex.exists { case (value, index) =>
          index > 0 & value.nonEmpty
        }

        if (hasValue && Try(Nino(ninoValue)).isSuccess) {
          Right(ninoValue)
        } else {
          Left(line)
        }
    }

    csvReader.readFromFile(path, extractCsvData)
  }

  def processBulkOptOuts(
    ninos: List[String]
  )(implicit ec: ExecutionContext, mat: Materializer, hc: HeaderCarrier): Future[List[BulkOptOutResult]] =
    Source(ninos)
      .throttle(bulkOptOutsConfig.maxOptOutsPerSecond, 1.second)
      .mapAsync(parallelism = 2) { nino =>
        entityResolverConnector
          .optOut(TaxIdentifier.ninoIdentifier(nino))
          .map { optOutResult =>
            ProcessedBulkOptOutResult(nino, optOutResult)
          }
          .recover { case throwable: Throwable =>
            logger.error(s"Bulk opt out processing failed for nino $nino record", throwable)
            FailedCallBulkOptOutResult(nino)
          }
      }
      .runWith(Sink.seq)
      .map(_.toList)
}
