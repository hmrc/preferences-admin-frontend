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

package uk.gov.hmrc.preferencesadminfrontend.auth

import com.google.inject.{ Inject, Singleton }
import play.api.mvc.*
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.{ ExecutionContext, Future }

@Singleton
class FakeStrideAuth @Inject() (
  val authConnector: AuthConnector,
  val serviceConfig: ServicesConfig,
  defaultActionBuilder: DefaultActionBuilder
) extends StrideAuth {

  def authorisedFromStride(
    action: Request[AnyContent] => OperatorDetails => Future[Result]
  )(implicit ec: ExecutionContext): Action[AnyContent] =
    defaultActionBuilder.async(implicit request => action(request)(OperatorDetails(List.empty, Some(""), "")))

  def authorisedFromStrideOrLoginCredentials(
    action: Request[AnyContent] => OperatorDetails => Future[Result]
  )(implicit ec: ExecutionContext): Action[AnyContent] =
    defaultActionBuilder.async(implicit request => action(request)(OperatorDetails(List.empty, Some(""), "")))
}
