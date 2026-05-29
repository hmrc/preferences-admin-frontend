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
import org.apache.pekko.util.ByteString
import play.api.libs.json.Json
import play.api.mvc.*
import play.api.mvc.Results.Ok
import uk.gov.hmrc.preferencesadminfrontend.services.model.CsvData

import java.nio.file.Path
import javax.inject.Inject
import scala.concurrent.{ ExecutionContext, Future }

class UploadService @Inject() {

  private val FrameLength = 1024
  private val AllowTruncation = true

  def readFromFile(path: Path)(implicit mat: Materializer): Future[List[CsvData]] =
    FileIO
      .fromPath(path)
      .via(Framing.delimiter(ByteString("\n"), FrameLength, AllowTruncation))
      .map(_.utf8String.trim)
      .filter(_.nonEmpty)
      .collect {
        case line if line.split(",").map(_.trim).length >= 3 =>
          val cols = line.split(",").map(_.trim)
          CsvData(cols(0), cols(1), cols(2))
      }
      .runWith(Sink.seq)
      .map(_.toList)(mat.executionContext)

  def process(records: List[CsvData])(implicit ec: ExecutionContext): Future[Result] = {
    val processingFutures = records.map { record =>
      val jsonPayload = Json.toJson(record)
      Future.successful(s"jsonpayload $jsonPayload to be posted")
    }

    Future
      .sequence(processingFutures)
      .map { _ =>
        Ok(s"Processing ${processingFutures.size} records.")
      }
  }
}
