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

package uk.gov.hmrc.preferencesadminfrontend.config

import javax.inject.{Inject, Singleton}

import com.kenshoo.play.metrics.MetricsFilter
import org.joda.time.Duration
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Request
import play.api.{Application, Configuration}
import play.filters.csrf.CSRFFilter
import play.twirl.api.Html
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.config.RunMode
import uk.gov.hmrc.play.frontend.bootstrap.DefaultFrontendGlobal
import uk.gov.hmrc.preferencesadminfrontend.config.filters.{PreferencesFrontendAuditFilter, SessionTimeoutFilterWithEntryPoint}
import uk.gov.hmrc.play.frontend.filters.{ FrontendLoggingFilter, SessionTimeoutFilter }

@Singleton
class AdminFrontendGlobal @Inject()(
     override val loggingFilter: FrontendLoggingFilter,
     override val metricsFilter: MetricsFilter,
     override val frontendAuditFilter: PreferencesFrontendAuditFilter,
     override val auditConnector: AuditConnector,
     override val configuration: Configuration,
     override val csrfFilter: CSRFFilter,
     runMode: RunMode)(implicit val messagesApi: MessagesApi) extends DefaultFrontendGlobal with I18nSupport {

  override def standardErrorTemplate(pageTitle: String, heading: String, message: String)(implicit request: Request[_]): Html =
    throw new RuntimeException("Deprecated - moved to injection of HttpErrorHandler")

  override def microserviceMetricsConfig(implicit app: Application): Option[Configuration] = app.configuration.getConfig(s"${runMode.env}.microservice.metrics")

  override lazy val sessionTimeoutFilter: SessionTimeoutFilter = {

    val defaultTimeout = Duration.standardMinutes(15)
    val timeoutDuration = configuration
      .getLong("session.timeoutSeconds")
      .map(Duration.standardSeconds)
      .getOrElse(defaultTimeout)

    val wipeIdleSession = configuration
      .getBoolean("session.wipeIdleSession")
      .getOrElse(true)

    val additionalSessionKeysToKeep = configuration
      .getStringSeq("session.additionalSessionKeysToKeep")
      .getOrElse(Seq.empty).toSet

    new SessionTimeoutFilterWithEntryPoint(timeoutDuration = timeoutDuration)
  }
}
