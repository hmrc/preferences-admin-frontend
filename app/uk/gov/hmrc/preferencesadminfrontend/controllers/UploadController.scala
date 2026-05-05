/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.preferencesadminfrontend.controllers

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.IOResult
import org.apache.pekko.stream.scaladsl.*
import org.apache.pekko.util.ByteString
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.libs.Files
import play.api.libs.json.Json
import play.api.mvc.*
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.preferencesadminfrontend.config.AppConfig
import uk.gov.hmrc.preferencesadminfrontend.controllers.model.CsvData
import uk.gov.hmrc.preferencesadminfrontend.views.html.*

import java.nio.file.Path
import javax.inject.{ Inject, Singleton }
import scala.concurrent.{ ExecutionContext, Future }

@Singleton
class UploadController @Inject() (
  authorisedAction: AuthorisedAction,
  uploadView: file_upload,
  val mcc: MessagesControllerComponents
)(implicit
  appConfig: AppConfig,
  ec: ExecutionContext,
  actorSystem: ActorSystem
) extends FrontendController(mcc) with I18nSupport with Logging with RoleAuthorisedAction(authorisedAction) {

  override def role: Role = Role.Admin

  val showUploadPage: Action[AnyContent] = authorisedAction { implicit request => _ =>
    Future.successful(Ok(uploadView()))
  }

  def upload(): Action[MultipartFormData[Files.TemporaryFile]] = Action.async(parse.multipartFormData) { request =>
    request.body
      .file("csvFile")
      .map { filePart =>
        val path = filePart.ref.path
        val source = readFromFile(path)
        processFile(source)
      }
      .getOrElse {
        Future.successful(BadRequest("File missing!"))
      }
  }

  private def readFromFile(path: Path) = FileIO
    .fromPath(path)
    .via(Framing.delimiter(ByteString("\n"), maximumFrameLength = 1024, allowTruncation = true))
    .map(_.utf8String.trim)
    .filter(_.nonEmpty)
    .map { line =>
      val cols = line.split(",").map(_.trim)
      if (cols.length >= 3) Some(CsvData(cols(0), cols(1), cols(2))) else None
    }
    .collect { case Some(data) => data }

  private def processFile(source: Source[CsvData, Future[IOResult]]) = source
    .mapAsync(2) { record =>
      val jsonPayload = Json.toJson(record)
      // post json to process api
      Future.successful(s"jsonpayload $jsonPayload to be posted")
    }
    .runWith(Sink.ignore)
    .map { _ =>
      Ok("Processing complete.")
    }
    .recover { case e: Exception =>
      InternalServerError(s"Error: ${e.getMessage}")
    }
}
