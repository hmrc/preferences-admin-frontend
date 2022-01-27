/*
 * Copyright 2022 HM Revenue & Customs
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

import play.api.Logging
import play.api.data.FormError
import play.api.i18n.I18nSupport
import play.api.libs.json.Json
import play.api.mvc.{ MessagesControllerComponents, Result }
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.preferencesadminfrontend.config.AppConfig

import uk.gov.hmrc.preferencesadminfrontend.model.{ MigrationEntries, MigrationSummary }
import uk.gov.hmrc.preferencesadminfrontend.services.{ Identifier, MigratePreferencesService, MigrationResult }

import uk.gov.hmrc.preferencesadminfrontend.model.{ MigrationEntries, MigrationSummary, SummaryItem, SyncEntries }

import uk.gov.hmrc.preferencesadminfrontend.views.html.{ migration_entries, migration_status, migration_summary }
import java.net.IDN
import javax.inject.Inject
import scala.concurrent.{ ExecutionContext, Future }
import Identifier._

class MessageController @Inject()(
  authorisedAction: AuthorisedAction,
  migrationEntriesView: migration_entries,
  migrationSummaryView: migration_summary,
  migrationStatusView: migration_status,
  sendMessageService: MigratePreferencesService,
  mcc: MessagesControllerComponents)(implicit appConfig: AppConfig, ec: ExecutionContext)
    extends FrontendController(mcc) with I18nSupport with Logging {

  def show() = authorisedAction.async { implicit request => implicit user =>
    Future.successful(Ok(migrationEntriesView(MigrationEntries())))
  }

  def check() = authorisedAction.async { implicit request => implicit user =>
    MigrationEntries()
      .bindFromRequest()
      .fold(
        formWithErrors => {
          Future.successful(BadRequest(migrationEntriesView(formWithErrors)))
        },
        input => {
          parse(input.entries) match {
            case Right(identifiers) =>
              sendMessageService.migrate(identifiers, dryRun = false).map {
                result =>
                  val identifiersSerialized = Json.toJson(identifiers).toString()

                  val total = result
                  val group = result.groupBy(_.status)
                  val noDigitalFootPrint = group("NoDigital")
                  val saOnline = group("SAOnlineCustomer")
                  val ItsaOnlineNoPreference = group("ITSAOnlineNoPreference")
                  val ItsaOnlinewithPreference = group("ITSAOnline")
                  val saItsaCustomer = group("SA&ITSACustomer")

                  val summary = MigrationSummary(
                    total = SummaryItem(total.size, total),
                    noDigitalFootprint = SummaryItem(noDigitalFootPrint.size, noDigitalFootPrint),
                    saOnlineCustomer = SummaryItem(saOnline.size, saOnline),
                    itsaOnlineNoPreference = SummaryItem(ItsaOnlineNoPreference.size, ItsaOnlineNoPreference),
                    itsaOnlineCustomerPreference = SummaryItem(ItsaOnlinewithPreference.size, ItsaOnlinewithPreference),
                    saAndItsaCustomer = SummaryItem(saItsaCustomer.size, saItsaCustomer)
                  )
                  Ok(migrationSummaryView(summary, identifiers, SyncEntries().fill(SyncEntries(identifiersSerialized, false))))
              }
            case Left(value) => Future.successful(BadRequest(migrationEntriesView(MigrationEntries().withError(FormError("identifiers", value)))))
          }
        }
      )
  }

  def sync() = authorisedAction.async { implicit request => implicit user =>
    val json = request.body.asFormUrlEncoded.get("entries").toList.head

    val identifiers = Json.parse(json).validate[List[Identifier]].get

    sendMessageService.migrate(identifiers = identifiers, dryRun = true).map { resut: List[MigrationResult] =>
      Ok(migrationStatusView(resut))
    }

  }

  def parse(input: String): Either[String, List[Identifier]] = {
    val lines: List[String] = input.split("\n").toList
    def parseEntries(lines: List[String]): Either[String, List[Identifier]] = {
      def add(line: String): Either[String, Identifier] =
        line.split(",").toList.map(_.trim) match {
          case first :: second :: Nil if first.isEmpty  => Left(s"ItsaId is missing for $second")
          case first :: second :: Nil if second.isEmpty => Left(s"Utr is missing for $first")
          case first :: second :: Nil                   => Right(Identifier(first, second))
          case first :: Nil                             => Left(s"whitespace")
          case _ :: _ :: _ :: Nil                       => Left(s"only itsaId and utr is required")
          case Nil                                      => Left("empty line")
        }
      def loop(lines: List[String], acc: Either[String, List[Identifier]]): Either[String, List[Identifier]] =
        lines match {
          case line :: tail => {
            val lineResult = add(line).left.flatMap(x => Left(x))
            lineResult.flatMap(item => loop(tail, acc.map(i => i :+ item)))
          }
          case Nil => acc
          case _   => Left("input is empty")
        }
      loop(lines, Right(List.empty))
    }
    parseEntries(lines)
  }

}
