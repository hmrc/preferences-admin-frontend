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
import play.api.mvc._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.preferencesadminfrontend.config.AppConfig
import uk.gov.hmrc.preferencesadminfrontend.views.html.{ decode, home }
import scala.concurrent.Future

@Singleton
class HomeController @Inject() (homeView: home, decoderView: decode, val mcc: MessagesControllerComponents)(implicit
  appConfig: AppConfig
) extends FrontendController(mcc) with I18nSupport with Logging {

  val showHomePage = Action.async { implicit request =>
    Future.successful(Ok(homeView()))
  }

  val showDecodePage = Action.async { implicit request =>
    Future.successful(Ok(decoderView()))
  }
}
