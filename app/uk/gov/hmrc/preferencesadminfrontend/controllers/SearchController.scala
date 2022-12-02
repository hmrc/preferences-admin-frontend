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
import play.api.mvc.{ Action, AnyContent, MessagesControllerComponents }
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.preferencesadminfrontend.config.AppConfig
import uk.gov.hmrc.preferencesadminfrontend.connectors.{ AlreadyOptedOut, OptedOut, PreferenceNotFound }
import uk.gov.hmrc.preferencesadminfrontend.controllers.model.{ OptOutReasonWithIdentifier, Search }
import uk.gov.hmrc.preferencesadminfrontend.services._
import uk.gov.hmrc.preferencesadminfrontend.services.model.TaxIdentifier
import uk.gov.hmrc.preferencesadminfrontend.views.html.{ confirmed, customer_identification, failed, user_opt_out }

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Failure, Success, Try }

@Singleton
class SearchController @Inject()(
  authorisedAction: AuthorisedAction,
  searchService: SearchService,
  mcc: MessagesControllerComponents,
  confirmedView: confirmed,
  customerIdentificationView: customer_identification,
  failedView: failed,
  userOptOutView: user_opt_out)(implicit appConfig: AppConfig, ec: ExecutionContext)
    extends FrontendController(mcc) with I18nSupport with Logging {

  def showSearchPage(): Action[AnyContent] =
    authorisedAction.async { implicit request => implicit user =>
      Future.successful(
        Ok(
          customerIdentificationView(
            Search()
              .bind(Map("name" -> "", "value" -> ""))
              .discardingErrors)))
    }

  def search(): Action[AnyContent] = authorisedAction.async { implicit request => implicit user =>
    Search().bindFromRequest.fold(
      errors => Future.successful(BadRequest(customerIdentificationView(errors))),
      searchTaxIdentifier => {
        searchService.searchPreference(searchTaxIdentifier).map {
          case Nil =>
            Ok(customerIdentificationView(Search().bindFromRequest.withError("value", "error.preference_not_found")))
          case preferenceList =>
            Ok(userOptOutView(OptOutReasonWithIdentifier(), preferenceList))
        }
      }
    )
  }

  def optOut(): Action[AnyContent] = authorisedAction.async { implicit request => implicit user =>
    OptOutReasonWithIdentifier().bindFromRequest.fold(
      errors => {
        errors.value
          .map(f => TaxIdentifier(f.identifierName, f.identifierValue))
          .map { identifier =>
            searchService.getPreference(identifier).map {
              case Nil =>
                Ok(customerIdentificationView(Search().bindFromRequest.withError("value", Messages("error.preference_not_found"))))
              case preferences =>
                Ok(userOptOutView(errors, preferences))
            }
          }
          .getOrElse(Future(Ok(customerIdentificationView(Search().bindFromRequest.withError("value", Messages("error.preference_not_found"))))))
      },
      optOutReason => {
        val identifier = TaxIdentifier(optOutReason.identifierName, optOutReason.identifierValue)
        searchService.optOut(identifier, optOutReason.reason).map {
          case OptedOut => Redirect(routes.SearchController.searchConfirmed())
          case AlreadyOptedOut =>
            Redirect(routes.SearchController.searchFailed(AlreadyOptedOut.errorCode))
          case PreferenceNotFound =>
            Redirect(routes.SearchController.searchFailed(PreferenceNotFound.errorCode))
        }
      }
    )
  }

  def searchConfirmed(): Action[AnyContent] = authorisedAction.async { implicit request => implicit user =>
    Try(OptOutReasonWithIdentifier().bindFromRequest.get) match {
      case Failure(_) => Future.successful(Ok(customerIdentificationView(Search().bindFromRequest.withError("value", Messages("error.preference_not_found")))))
      case Success(form) =>
        searchService.getPreference(TaxIdentifier(form.identifierName, form.identifierValue)).map {
          case Nil =>
            Ok(failedView(TaxIdentifier(form.identifierName, form.identifierValue), Nil, PreferenceNotFound.errorCode))
          case preferences =>
            Ok(confirmedView(preferences))
        }
    }
  }

  def searchFailed(failureCode: String): Action[AnyContent] = authorisedAction.async { implicit request => implicit user =>
    Try(OptOutReasonWithIdentifier().bindFromRequest.get) match {
      case Failure(_) => Future.successful(Ok(customerIdentificationView(Search().bindFromRequest.withError("value", Messages("error.preference_not_found")))))
      case Success(form) =>
        searchService.getPreference(TaxIdentifier(form.identifierName, form.identifierValue)).map { preference =>
          Ok(failedView(TaxIdentifier(form.identifierName, form.identifierValue), preference, failureCode))
        }
    }
  }
}
