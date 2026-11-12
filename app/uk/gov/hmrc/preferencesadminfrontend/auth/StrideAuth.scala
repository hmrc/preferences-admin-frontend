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

import com.google.inject.{ ImplementedBy, Inject, Singleton }
import play.api.libs.json.{ Format, Json }
import play.api.mvc.*
import play.api.mvc.Results.*
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.AuthProvider.PrivilegedApplication
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendHeaderCarrierProvider

import scala.concurrent.{ ExecutionContext, Future }

trait AuthRedirects {

  def serviceConfig: ServicesConfig

  private def strideLoginUrl: String = serviceConfig.baseUrl("stride-auth-frontend") + "/stride/sign-in"

  lazy val origin: String = serviceConfig.getString("appName")

  def toStrideLogin(successUrl: String, failureUrl: Option[String] = None): Result =
    Redirect(
      strideLoginUrl,
      Map(
        "successURL" -> Seq(successUrl),
        "origin"     -> Seq(origin)
      ) ++ failureUrl.map(f => Map("failureURL" -> Seq(f))).getOrElse(Map())
    )

}

@ImplementedBy(classOf[StrideAuthImpl])
trait StrideAuth extends AuthRedirects with AuthorisedFunctions with Results with FrontendHeaderCarrierProvider {

  def authorisedFromStride(
    action: Request[AnyContent] => OperatorDetails => Future[Result]
  )(implicit ec: ExecutionContext): Action[AnyContent]

}

@Singleton
class StrideAuthImpl @Inject() (
  val authConnector: AuthConnector,
  val serviceConfig: ServicesConfig,
  actionBuilder: DefaultActionBuilder
) extends StrideAuth {

  val role: String = serviceConfig.getString("stride.role")

  def authorisedFromStride(
    action: Request[AnyContent] => OperatorDetails => Future[Result]
  )(implicit ec: ExecutionContext): Action[AnyContent] =
    actionBuilder.async { implicit request =>
      authorised(Enrolment(role) and AuthProviders(PrivilegedApplication))
        .retrieve(Retrievals.allEnrolments and Retrievals.email and Retrievals.credentials) {
          case allEnrolments ~ email ~ credentials =>
            action(request)(
              OperatorDetails(
                allEnrolments.enrolments.map(_.key).toList,
                credentials.map(_.providerId),
                email.getOrElse("")
              )
            )
        }
        .recoverWith {
          case _: NoActiveSession =>
            Future.successful(
              toStrideLogin(
                s"http://${request.host}${request.uri}"
              )
            )
          case _: InsufficientEnrolments =>
            Future.successful(
              SeeOther(uk.gov.hmrc.preferencesadminfrontend.controllers.routes.InsufficientRolesController.get().url)
            )
        }
    }

}

case class OperatorDetails(roles: List[String], pid: Option[String], email: String)

@SuppressWarnings(Array("org.wartremover.warts.Any"))
object OperatorDetails {
  given Format[OperatorDetails] = Json.format[OperatorDetails]
}
