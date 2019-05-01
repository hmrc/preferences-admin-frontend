/*
 * Copyright 2019 HM Revenue & Customs
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

import javax.inject.{Inject, Singleton}

import play.api.{Configuration, Play}
import play.api.data.Form
import play.api.data.Forms.{mapping, text}
import play.api.i18n.{I18nSupport, MessagesApi}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.config.AppName
import uk.gov.hmrc.play.frontend.controller.FrontendController
import uk.gov.hmrc.preferencesadminfrontend.config.AppConfig
import uk.gov.hmrc.preferencesadminfrontend.connectors.{AlreadyOptedOut, OptedOut, PreferenceNotFound}
import uk.gov.hmrc.preferencesadminfrontend.controllers.model.{OptOutReason, Search}
import uk.gov.hmrc.preferencesadminfrontend.services._
import uk.gov.hmrc.preferencesadminfrontend.services.model.TaxIdentifier

import scala.concurrent.Future

@Singleton
class SearchController @Inject()(auditConnector: AuditConnector, searchService: SearchService)
                                (implicit appConfig: AppConfig, val messagesApi: MessagesApi) extends FrontendController with AppName with I18nSupport {

  def appNameConfiguration: Configuration = Play.current.configuration

  def showSearchPage(taxIdentifierName: String, taxIdentifierValue: String) = AuthorisedAction.async {
    implicit request =>
      implicit user =>
        Future.successful(Ok(uk.gov.hmrc.preferencesadminfrontend.views.html.customer_identification(Search()
              .bind(Map("name" -> taxIdentifierName, "value" -> taxIdentifierValue)).discardingErrors)))
  }

  def search = AuthorisedAction.async {
    implicit request =>
      implicit user =>
        Search().bindFromRequest.fold(
          errors => Future.successful(BadRequest(uk.gov.hmrc.preferencesadminfrontend.views.html.customer_identification(errors))),
          searchTaxIdentifier => {
            searchService.searchPreference(searchTaxIdentifier).map {
              case Nil =>
                Ok(uk.gov.hmrc.preferencesadminfrontend.views.html.customer_identification(Search().bindFromRequest.withError("value", "error.preference_not_found")))
              case preferenceList =>
                Ok(uk.gov.hmrc.preferencesadminfrontend.views.html.user_opt_out(OptOutReason(), searchTaxIdentifier, preferenceList))
            }
          }
        )
  }

  def optOut(taxIdentifierName: String, taxIdentifierValue: String) = AuthorisedAction.async {
    implicit request =>
      implicit user =>
        val identifier = TaxIdentifier(taxIdentifierName, taxIdentifierValue)
        OptOutReason().bindFromRequest.fold(
          errors => {
            searchService.getPreference(identifier).map {
              case Nil =>
                Ok(uk.gov.hmrc.preferencesadminfrontend.views.html.customer_identification(Search().bindFromRequest.withError("value", "error.preference_not_found")))
              case preferences =>
                Ok(uk.gov.hmrc.preferencesadminfrontend.views.html.user_opt_out(errors, identifier, preferences))
            }
          },
          optOutReason => {
            searchService.optOut(identifier, optOutReason.reason).map {
              case OptedOut => Redirect(routes.SearchController.confirmed(taxIdentifierName, taxIdentifierValue))
              case AlreadyOptedOut => Redirect(routes.SearchController.failed(taxIdentifierName, taxIdentifierValue, AlreadyOptedOut.errorCode))
              case PreferenceNotFound => Redirect(routes.SearchController.failed(taxIdentifierName, taxIdentifierValue, PreferenceNotFound.errorCode))
            }
          }
        )
  }

  def confirmed(taxIdentifierName: String, taxIdentifierValue: String) = AuthorisedAction.async {
    implicit request =>
      implicit user =>
        searchService.getPreference(TaxIdentifier(taxIdentifierName, taxIdentifierValue)).map {
          case Nil =>
            Ok(uk.gov.hmrc.preferencesadminfrontend.views.html.failed(TaxIdentifier(taxIdentifierName, taxIdentifierValue), Nil, PreferenceNotFound.errorCode))
          case preferences =>
            Ok(uk.gov.hmrc.preferencesadminfrontend.views.html.confirmed(preferences))
          }
  }

  def failed(taxIdentifierName: String, taxIdentifierValue: String, failureCode: String) = AuthorisedAction.async {
    implicit request =>
      implicit user =>
        searchService.getPreference(TaxIdentifier(taxIdentifierName, taxIdentifierValue)).map { preference =>
          Ok(uk.gov.hmrc.preferencesadminfrontend.views.html.failed(TaxIdentifier(taxIdentifierName, taxIdentifierValue), preference, failureCode))
        }
  }
}




