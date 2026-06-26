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

package uk.gov.hmrc.preferencesadminfrontend.controllers

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.scaladsl.Framing
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.libs.Files
import play.api.mvc.*
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.preferencesadminfrontend.config.{ AppConfig, BulkOptOutsConfig }
import uk.gov.hmrc.preferencesadminfrontend.connectors.{ AlreadyOptedOut, OptedOut, PreferenceNotFound }
import uk.gov.hmrc.preferencesadminfrontend.services.*
import uk.gov.hmrc.preferencesadminfrontend.views.html.*

import javax.inject.{ Inject, Singleton }
import scala.concurrent.{ ExecutionContext, Future }
@Singleton
class CsvUploadController @Inject() (
  authorisedAction: AuthorisedAction,
  csvUpload: csv_upload,
  csvUploadConfirm: csv_upload_confirmation,
  csvUploadBulkOptOuts: csv_upload_bulk_opt_outs,
  csvUploadBulkOptOutConfirmation: cvs_upload_bulk_opt_confirmation,
  uploadService: UploadService,
  bulkUploadOptOutsService: BulkUploadOptOutsService,
  bulkOptOutsConfig: BulkOptOutsConfig,
  mcc: MessagesControllerComponents
)(implicit appConfig: AppConfig, ec: ExecutionContext, actorSystem: ActorSystem)
    extends FrontendController(mcc) with I18nSupport with Logging with RoleAuthorisedAction(authorisedAction) {

  override def role: Role = Role.Admin

  val showUploadPage: Action[AnyContent] = authorisedAction { implicit request => _ =>
    Future.successful(Ok(csvUpload()))
  }

  def upload(): Action[MultipartFormData[Files.TemporaryFile]] = Action.async(parse.multipartFormData) {
    implicit request =>
      request.body
        .file("csvFile")
        .map { filePart =>
          val path = filePart.ref.path
          uploadService
            .readFromFile(path)
            .flatMap(uploadService.process)
            .map(msg => Ok(csvUploadConfirm(msg)))
        }
        .getOrElse {
          Future.successful(BadRequest(csvUploadConfirm("File missing or incorrect data supplied!")))
        }
  }

  val showBulkOptOutsUploadPage: Action[AnyContent] = authorisedAction { implicit request => _ =>
    Future.successful(
      Ok(
        csvUploadBulkOptOuts(
          maxUploads = bulkOptOutsConfig.maxUploadEntries,
          errors = List.empty,
          uploadedFileHadNoEntries = false,
          uploadedFileWasInvalid = false,
          tooManyEntriesWereUploadedCount = 0
        )
      )
    )
  }

  def uploadBulkOptOuts(): Action[MultipartFormData[Files.TemporaryFile]] = Action.async(parse.multipartFormData) {
    implicit request =>
      request.body
        .file("csvFile")
        .map { filePart =>
          val path = filePart.ref.path
          val eventualErrorOrPotentialOptOuts = bulkUploadOptOutsService.readNinoBulkOptOutsFromFile(path)

          eventualErrorOrPotentialOptOuts.flatMap {
            case Left(framingException: Framing.FramingException) =>
              logger.error("Failed uploading bulk opt outs file", framingException)

              Future.successful(
                Ok(
                  csvUploadBulkOptOuts(
                    maxUploads = bulkOptOutsConfig.maxUploadEntries,
                    errors = List.empty,
                    uploadedFileHadNoEntries = false,
                    uploadedFileWasInvalid = true,
                    tooManyEntriesWereUploadedCount = 0
                  )
                )
              )
            case Right(errorOrOptOutList) =>
              processOptOutFileUpload(errorOrOptOutList)
          }
        }
        .getOrElse {
          Future.successful(
            Ok(
              csvUploadBulkOptOuts(
                bulkOptOutsConfig.maxUploadEntries,
                errors = List.empty,
                uploadedFileHadNoEntries = true,
                uploadedFileWasInvalid = false,
                tooManyEntriesWereUploadedCount = 0
              )
            )
          )
        }
  }

  private def processOptOutFileUpload(
    errorOrOptOutList: List[Either[String, String]]
  )(implicit request: Request[_]): Future[Result] =
    if (errorOrOptOutList.isEmpty) {
      Future.successful(
        Ok(
          csvUploadBulkOptOuts(
            maxUploads = bulkOptOutsConfig.maxUploadEntries,
            errors = List.empty,
            uploadedFileHadNoEntries = true,
            uploadedFileWasInvalid = false,
            tooManyEntriesWereUploadedCount = 0
          )
        )
      )
    } else {
      val errors = errorOrOptOutList.collect { case Left(error) => error }
      val successfulEntries = errorOrOptOutList.collect { case Right(success) => success }
      val uploadedCount = errorOrOptOutList.length
      val hasTooManyUploads = uploadedCount > bulkOptOutsConfig.maxUploadEntries

      if (errors.nonEmpty || hasTooManyUploads) {
        val tooManyEntriesWereUploadedCount = if (hasTooManyUploads) {
          uploadedCount
        } else {
          0
        }

        Future.successful(
          Ok(
            csvUploadBulkOptOuts(
              maxUploads = bulkOptOutsConfig.maxUploadEntries,
              errors = errors,
              uploadedFileHadNoEntries = false,
              uploadedFileWasInvalid = false,
              tooManyEntriesWereUploadedCount = tooManyEntriesWereUploadedCount
            )
          )
        )
      } else {
        bulkUploadOptOutsService.processBulkOptOuts(successfulEntries).map { bulkOptOutResults =>
          showBulkOptOutConfirmation(bulkOptOutResults)
        }
      }
    }

  private def showBulkOptOutConfirmation(
    bulkOptOutResults: List[BulkOptOutResult]
  )(implicit request: Request[_]): Result = {
    val failedCallNinos = bulkOptOutResults.collect { case failedCall: FailedCallBulkOptOutResult =>
      failedCall.nino
    }
    val successfullyOptedOutNinos =
      bulkOptOutResults.collect { case ProcessedBulkOptOutResult(nino, OptedOut) => nino }
    val alreadyOptedOutNinos =
      bulkOptOutResults.collect { case ProcessedBulkOptOutResult(nino, AlreadyOptedOut) => nino }
    val notFoundNinos = bulkOptOutResults.collect { case ProcessedBulkOptOutResult(nino, PreferenceNotFound) =>
      nino
    }

    Ok(
      csvUploadBulkOptOutConfirmation(
        successfullyOptedOutNinos = successfullyOptedOutNinos,
        alreadyOptedOutNinos = alreadyOptedOutNinos,
        notFoundNinos = notFoundNinos,
        failedCallNinos = failedCallNinos
      )
    )
  }
}
