/*
 * Copyright 2017 HM Revenue & Customs
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

import javax.inject.Singleton

import com.google.inject.Inject
import com.kenshoo.play.metrics.MetricsFilter
import play.api.http.DefaultHttpFilters
import play.filters.csrf.CSRFFilter
import uk.gov.hmrc.play.filters.{NoCacheFilter, RecoveryFilter}

@Singleton
class MicroserviceFilters @Inject()(
  metricsFilter: MetricsFilter,
  auditFilter: MicroserviceAuditFilter,
  loggingFilter: MicroserviceLoggingFilter,
  csrfFilter: CSRFFilter,
  authFilter: AuthFilter
) extends DefaultHttpFilters(
  metricsFilter,
  auditFilter,
  loggingFilter,
  csrfFilter,
  authFilter,
  NoCacheFilter,
  RecoveryFilter
)
