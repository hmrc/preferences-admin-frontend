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

import javax.inject.{ Inject, Singleton }
import play.api.i18n.{ I18nSupport, Messages }
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.preferencesadminfrontend.config.AppConfig
import uk.gov.hmrc.preferencesadminfrontend.connectors.{ AlreadyOptedOut, OptedOut, PreferenceNotFound }
import uk.gov.hmrc.preferencesadminfrontend.controllers.model.{ OptOutReason, Search }
import uk.gov.hmrc.preferencesadminfrontend.services._
import uk.gov.hmrc.preferencesadminfrontend.services.model.TaxIdentifier
import uk.gov.hmrc.preferencesadminfrontend.views.html.{ confirmed, customer_identification, failed, user_opt_out }

import scala.concurrent.{ ExecutionContext, Future }

@Singleton
class SearchController @Inject()(
  authorisedAction: AuthorisedAction,
  auditConnector: AuditConnector,
  searchService: SearchService,
  mcc: MessagesControllerComponents,
  confirmedView: confirmed,
  customerIdentificationView: customer_identification,
  failedView: failed,
  userOptOutView: user_opt_out)(implicit appConfig: AppConfig, ec: ExecutionContext)
    extends FrontendController(mcc) with I18nSupport with Logging {

  def showSearchPage(taxIdentifierName: String, taxIdentifierValue: String) =
    authorisedAction.async { implicit request => implicit user =>
      Future.successful(
        Ok(
          customerIdentificationView(
            Search()
              .bind(Map("name" -> taxIdentifierName, "value" -> taxIdentifierValue))
              .discardingErrors)))
    }

  def search = authorisedAction.async { implicit request => implicit user =>
    Search().bindFromRequest.fold(
      errors => Future.successful(BadRequest(customerIdentificationView(errors))),
      searchTaxIdentifier => {
        searchService.searchPreference(searchTaxIdentifier).map {
          case Nil =>
            Ok(customerIdentificationView(Search().bindFromRequest.withError("value", "error.preference_not_found")))
          case preferenceList =>
            Ok(userOptOutView(OptOutReason(), searchTaxIdentifier, preferenceList))
        }
      }
    )
  }

  def optOut(taxIdentifierName: String, taxIdentifierValue: String) = authorisedAction.async { implicit request => implicit user =>
    val identifier = TaxIdentifier(taxIdentifierName, taxIdentifierValue)
    OptOutReason().bindFromRequest.fold(
      errors => {
        searchService.getPreference(identifier).map {
          case Nil =>
            Ok(customerIdentificationView(Search().bindFromRequest.withError("value", Messages("error.preference_not_found"))))
          case preferences =>
            Ok(userOptOutView(errors, identifier, preferences))
        }
      },
      optOutReason => {
        searchService.optOut(identifier, optOutReason.reason).map {
          case OptedOut           => Redirect(routes.SearchController.searchConfirmed(taxIdentifierName, taxIdentifierValue))
          case AlreadyOptedOut    => Redirect(routes.SearchController.searchFailed(taxIdentifierName, taxIdentifierValue, AlreadyOptedOut.errorCode))
          case PreferenceNotFound => Redirect(routes.SearchController.searchFailed(taxIdentifierName, taxIdentifierValue, PreferenceNotFound.errorCode))
        }
      }
    )
  }

  def searchConfirmed(taxIdentifierName: String, taxIdentifierValue: String) = authorisedAction.async { implicit request => implicit user =>
    searchService.getPreference(TaxIdentifier(taxIdentifierName, taxIdentifierValue)).map {
      case Nil =>
        Ok(failedView(TaxIdentifier(taxIdentifierName, taxIdentifierValue), Nil, PreferenceNotFound.errorCode))
      case preferences =>
        Ok(confirmedView(preferences))
    }
  }

  def searchFailed(taxIdentifierName: String, taxIdentifierValue: String, failureCode: String) = authorisedAction.async { implicit request => implicit user =>
    searchService.getPreference(TaxIdentifier(taxIdentifierName, taxIdentifierValue)).map { preference =>
      Ok(failedView(TaxIdentifier(taxIdentifierName, taxIdentifierValue), preference, failureCode))
    }
  }
}
