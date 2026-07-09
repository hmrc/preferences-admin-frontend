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
import org.apache.pekko.stream.scaladsl.Framing.FramingException
import org.apache.pekko.stream.scaladsl.{ Sink, Source }
import play.api.Logging
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.preferencesadminfrontend.config.BulkOptOutsConfig
import uk.gov.hmrc.preferencesadminfrontend.connectors.{ EntityResolverConnector, OptOutResult }
import uk.gov.hmrc.preferencesadminfrontend.services.model.TaxIdentifier
import uk.gov.hmrc.preferencesadminfrontend.services.model.csv.UploadedBulKOptOutNinos

import java.nio.file.Path
import javax.inject.Inject
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ ExecutionContext, Future }
import scala.util.Try

sealed trait BulkOptOutResult {
  val nino: String
}
sealed trait HasFailedBulkOptOutResult extends BulkOptOutResult

case class InvalidNinoBulkOptOutResult(nino: String) extends HasFailedBulkOptOutResult
case class FailedCallBulkOptOutResult(nino: String) extends HasFailedBulkOptOutResult
case class ProcessedBulkOptOutResult(nino: String, optOutResult: OptOutResult) extends BulkOptOutResult

class BulkUploadOptOutsService @Inject() (
  csvReader: CsvReader,
  entityResolverConnector: EntityResolverConnector,
  bulkOptOutsConfig: BulkOptOutsConfig
)(implicit ec: ExecutionContext)
    extends Logging {

  def readNinoBulkOptOutsFromFile(
    path: Path
  )(implicit mat: Materializer): Future[Either[FramingException, UploadedBulKOptOutNinos]] = {
    val extractCsvData: PartialFunction[Any, String] = {
      case line: String if line.split(",").map(_.trim).length >= 1 =>
        val cols = line.split(",").map(_.trim)

        cols(0)
    }

    csvReader
      .readFromFile(path, extractCsvData)
      .map { entries =>
        Right[FramingException, UploadedBulKOptOutNinos](
          UploadedBulKOptOutNinos.createUniqueFiltered(entries.filter(_.nonEmpty))
        )
      }
      .recover { case e: FramingException => Left(e) }
  }

  def processBulkOptOuts(
    ninos: List[String]
  )(implicit ec: ExecutionContext, mat: Materializer, hc: HeaderCarrier): Future[List[BulkOptOutResult]] = {
    val invalidAndValidNinos: List[Either[String, String]] = ninos.map { nino =>
      if (Try(Nino(nino)).isSuccess) {
        Right(nino)
      } else {
        Left(nino)
      }
    }

    Source(invalidAndValidNinos.collect { case Right(validNino) => validNino })
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
      .map { results =>
        invalidAndValidNinos.collect { case Left(invalidFormattedNino) =>
          InvalidNinoBulkOptOutResult(invalidFormattedNino)
        }
          ++ results.toList
      }
  }
}
