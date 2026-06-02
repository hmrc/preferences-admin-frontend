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

import java.nio.file.Path
import javax.inject.Inject
import scala.concurrent.Future

class CsvReader @Inject() {

  private val FrameLength = 1024
  private val AllowTruncation = true

  def readFromFile[A](path: Path, collectFilter: PartialFunction[Any, A])(implicit
    mat: Materializer
  ): Future[List[A]] =
    FileIO
      .fromPath(path)
      .via(Framing.delimiter(ByteString("\n"), FrameLength, AllowTruncation))
      .map(_.utf8String.trim)
      .filter(_.nonEmpty)
      .collect(collectFilter)
      .runWith(Sink.seq)
      .map(_.toList)(mat.executionContext)

}
