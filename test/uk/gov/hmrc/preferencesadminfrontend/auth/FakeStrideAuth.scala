package uk.gov.hmrc.preferencesadminfrontend.auth

import com.google.inject.{ Inject, Singleton }
import play.api.mvc._
import play.api.{ Configuration, Environment }
import uk.gov.hmrc.auth.core.AuthConnector

import scala.concurrent.{ ExecutionContext, Future }

@Singleton
class FakeStrideAuth @Inject() (
  val authConnector: AuthConnector,
  val env: Environment,
  val config: Configuration,
  defaultActionBuilder: DefaultActionBuilder
) extends StrideAuth {

  def authorisedFromStride(
    action: Request[AnyContent] => OperatorDetails => Future[Result]
  )(implicit ec: ExecutionContext): Action[AnyContent] =
    defaultActionBuilder.async(implicit request => action(request)(OperatorDetails(List.empty, Some(""), "")))
}
