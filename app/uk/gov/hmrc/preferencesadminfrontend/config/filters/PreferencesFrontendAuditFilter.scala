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

package uk.gov.hmrc.preferencesadminfrontend.config.filters

import org.apache.pekko.stream.Materializer
import play.api.Configuration

import javax.inject.{ Inject, Singleton }
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.config.{ ControllerConfigs, HttpAuditEvent }
import uk.gov.hmrc.play.bootstrap.frontend.filters.{ DefaultFrontendAuditFilter, RequestHeaderAuditing }

import scala.concurrent.ExecutionContext
@Singleton
class PreferencesFrontendAuditFilter @Inject() (
  override val config: Configuration,
  controllerConfigs: ControllerConfigs,
  override val auditConnector: AuditConnector,
  httpAuditEvent: HttpAuditEvent,
  requestHeaderAuditing: RequestHeaderAuditing,
  override val mat: Materializer
)(implicit ec: ExecutionContext)
    extends DefaultFrontendAuditFilter(
      config,
      controllerConfigs,
      auditConnector,
      httpAuditEvent,
      requestHeaderAuditing,
      mat
    ) {

  override val maskedFormFields: Seq[String] = Seq("password")

}
