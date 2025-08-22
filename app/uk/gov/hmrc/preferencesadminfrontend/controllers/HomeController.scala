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

import play.api.Logging

import javax.inject.{ Inject, Singleton }
import play.api.i18n.I18nSupport
import play.api.mvc.*
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.preferencesadminfrontend.config.AppConfig
import uk.gov.hmrc.preferencesadminfrontend.controllers.Role.Admin
import uk.gov.hmrc.preferencesadminfrontend.services.*
import uk.gov.hmrc.preferencesadminfrontend.controllers.model.{ SearchNinos, User }
import uk.gov.hmrc.preferencesadminfrontend.services.LoginService
import uk.gov.hmrc.preferencesadminfrontend.views.html.{ decode, emailSummary, home, multiSearch }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class HomeController @Inject() (
  authorisedAction: AuthorisedAction,
  homeView: home,
  loginService: LoginService,
  decoderView: decode,
  multiSearchView: multiSearch,
  emailSummaryView: emailSummary,
  searchService: SearchService,
  val mcc: MessagesControllerComponents
)(implicit
  appConfig: AppConfig
) extends FrontendController(mcc) with I18nSupport with Logging {

  val showHomePage: Action[AnyContent] = authorisedAction.async { implicit request => _ =>
    Future.successful(Ok(homeView()))
  }

  val showDecodePage: Action[AnyContent] = authorisedAction.async { implicit request => _ =>
    Future.successful(Ok(decoderView()))
  }

  val showMultiSearchPage: Action[AnyContent] = authorisedAction.async { implicit request => _ =>
    Future.successful(Ok(multiSearchView()))
  }

  def showResultsPage(): Action[AnyContent] = authorisedAction.async { implicit request => implicit user =>
    SearchNinos()
      .bindFromRequest()
      .fold(
        errors => Future.successful(BadRequest(emailSummaryView(List.empty, Some(s"Error found - $errors")))),
        searchTaxIdentifier =>
          searchService.searchPreferences(searchTaxIdentifier.identifiers).map {
            case Nil =>
              Ok(
                emailSummaryView(Nil, None)
              )
            case preferenceList =>
              Ok(emailSummaryView(preferenceList, None))
          }
      )
  }
}
