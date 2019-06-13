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

package uk.gov.hmrc.preferencesadminfrontend.connectors

import akka.actor.ActorSystem
import com.typesafe.config.Config
import javax.inject.{Inject, Singleton}
import play.api.{Configuration, Environment, Play}
import play.api.http.Status._
import play.api.Mode.Mode
import play.api.libs.json.Json
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.hooks.HttpHook
import uk.gov.hmrc.play.audit.http.HttpAuditing
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.config.{AppName, ServicesConfig}
import uk.gov.hmrc.play.http.ws.{WSDelete, WSGet, WSPost}
import uk.gov.hmrc.preferencesadminfrontend.FrontendAuditConnector
import uk.gov.hmrc.preferencesadminfrontend.model._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MessageConnector @Inject()(frontendAuditConnector: FrontendAuditConnector,
                                 environment: Environment,
                                 val runModeConfiguration: Configuration,
                                 val actorSystem: ActorSystem) extends HttpGet with WSGet
  with HttpPost with HttpDelete with WSPost with WSDelete with HttpAuditing with AppName with ServicesConfig {

  override protected def mode: Mode = environment.mode
  override def appNameConfiguration: Configuration = Play.current.configuration
  override lazy val configuration: Option[Config] = None

  val hooks: Seq[HttpHook] = Seq()
  override val auditConnector: AuditConnector = frontendAuditConnector

  def serviceUrl: String = baseUrl("message")

  def addRescindments(rescindmentRequest: RescindmentRequest)
                     (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[RescindmentUpdateResult] = {
    POST[RescindmentRequest, RescindmentUpdateResult](s"$serviceUrl/admin/message/add-rescindments", rescindmentRequest)
  }

  def sendRescindmentAlerts()
                     (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[RescindmentAlertsResult] = {
    POSTEmpty[RescindmentAlertsResult](s"$serviceUrl/admin/send-rescindment-alerts")
  }

  def getWhitelist()(implicit hc:HeaderCarrier): Future[HttpResponse] = {
    GET[HttpResponse](s"$serviceUrl/admin/message/brake/gmc/whitelist").recover {
      case e: Exception => HttpResponse(BAD_GATEWAY, None, Map(), Some(e.getMessage))
    }
  }

  def addFormIdToWhitelist(formIdEntry: WhitelistEntry)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    POST[WhitelistEntry,HttpResponse](s"$serviceUrl/admin/message/brake/gmc/whitelist/add",formIdEntry).recover {
      case e: Exception => HttpResponse(BAD_GATEWAY, None, Map(), Some(e.getMessage))
    }
  }

  def deleteFormIdFromWhitelist(formIdEntry: WhitelistEntry)(implicit hc:HeaderCarrier): Future[HttpResponse] = {
    POST[WhitelistEntry,HttpResponse](s"$serviceUrl/admin/message/brake/gmc/whitelist/delete", formIdEntry).recover {
      case e: Exception => HttpResponse(BAD_GATEWAY, None, Map(), Some(e.getMessage))
    }
  }

  def getGmcBatches()(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    GET[HttpResponse](s"$serviceUrl/admin/message/brake/gmc/batches").recover {
      case e: Exception => HttpResponse(BAD_GATEWAY, None, Map(), Some(e.getMessage))
    }
  }

  def getRandomMessagePreview(batch: GmcBatch)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    POST[GmcBatch,HttpResponse](s"$serviceUrl/admin/message/brake/random", batch).recover {
      case e: Exception => HttpResponse(BAD_GATEWAY, None, Map(), Some(e.getMessage))
    }
  }

  def approveGmcBatch(batch: GmcBatchApproval)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    POST[GmcBatchApproval,HttpResponse](s"$serviceUrl/admin/message/brake/accept", batch).recover {
      case e: Exception => HttpResponse(BAD_GATEWAY, None, Map(), Some(e.getMessage))
    }
  }

  def rejectGmcBatch(batch: GmcBatchApproval)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    POST[GmcBatchApproval,HttpResponse](s"$serviceUrl/admin/message/brake/reject", batch).recover {
      case e: Exception => HttpResponse(BAD_GATEWAY, None, Map(), Some(e.getMessage))
    }
  }

}