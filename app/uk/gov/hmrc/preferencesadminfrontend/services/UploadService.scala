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
import org.apache.pekko.stream.scaladsl.*
import play.api.mvc.*
import play.api.mvc.Results.Ok
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.preferencesadminfrontend.connectors.ChannelPreferencesConnector
import uk.gov.hmrc.preferencesadminfrontend.services.model.csv.CsvData

import java.nio.file.Path
import javax.inject.Inject
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ ExecutionContext, Future }

class UploadService @Inject() (channelPreferencesConnector: ChannelPreferencesConnector, csvReader: CsvReader) {

  def readFromFile(path: Path)(implicit mat: Materializer): Future[List[CsvData]] = {
    val extractCsvData: PartialFunction[Any, CsvData] = {
      case line: String if line.split(",").map(_.trim).length >= 3 =>
        val cols = line.split(",").map(_.trim)
        CsvData(cols(0), cols(1), cols(2))
    }

    csvReader.readFromFile(path, extractCsvData)
  }

  def process(
    records: List[CsvData]
  )(implicit ec: ExecutionContext, hc: HeaderCarrier, mat: Materializer): Future[Result] = {
    val LimitR = 10
    Source(records)
      .throttle(LimitR, 1.second)
      .mapAsync(parallelism = 2) { record =>
        channelPreferencesConnector.process(record)
      }
      .runWith(Sink.ignore)
      .map { _ =>
        Ok(s"Processed ${records.size} records successfully.")
      }
  }

}
