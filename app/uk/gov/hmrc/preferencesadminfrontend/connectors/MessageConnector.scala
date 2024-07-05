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

package uk.gov.hmrc.preferencesadminfrontend.connectors

import javax.inject.{ Inject, Singleton }
import play.api.http.Status._
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.preferencesadminfrontend.model._
import play.api.libs.json._
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import play.api.libs.ws.writeableOf_JsValue

import java.net.URI
import scala.concurrent.{ ExecutionContext, Future }

@Singleton
class MessageConnector @Inject() (httpClient: HttpClientV2, val servicesConfig: ServicesConfig)(implicit
  ec: ExecutionContext
) {

  private def serviceUrl(version: Option[String]): String =
    if (version.contains("v4")) s"${servicesConfig.baseUrl("secure-message")}/secure-messaging"
    else servicesConfig.baseUrl("message")

  val serviceUrl: String = serviceUrl(Some("v3"))

  def addRescindments(
    rescindmentRequest: RescindmentRequest
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[RescindmentUpdateResult] =
    httpClient
      .post(new URI(s"$serviceUrl/admin/message/add-rescindments").toURL)
      .withBody(Json.toJson(rescindmentRequest))
      .execute[RescindmentUpdateResult]

  def sendRescindmentAlerts()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[RescindmentAlertsResult] =
    httpClient
      .post(new URI(s"$serviceUrl/admin/send-rescindment-alerts").toURL)
      .execute[RescindmentAlertsResult]

  def getAllowlist()(implicit hc: HeaderCarrier): Future[HttpResponse] =
    httpClient
      .get(new URI(s"$serviceUrl/admin/message/brake/gmc/allowlist").toURL)
      .execute[HttpResponse]
      .recover { case e: Exception =>
        HttpResponse(BAD_GATEWAY, e.getMessage)
      }

  def addFormIdToAllowlist(formIdEntry: AllowlistEntry)(implicit hc: HeaderCarrier): Future[HttpResponse] =
    httpClient
      .post(new URI(s"$serviceUrl/admin/message/brake/gmc/allowlist/add").toURL)
      .withBody(Json.toJson(formIdEntry))
      .execute[HttpResponse]
      .recover { case e: Exception =>
        HttpResponse(BAD_GATEWAY, e.getMessage)
      }

  def deleteFormIdFromAllowlist(formIdEntry: AllowlistEntry)(implicit hc: HeaderCarrier): Future[HttpResponse] =
    httpClient
      .post(new URI(s"$serviceUrl/admin/message/brake/gmc/allowlist/delete").toURL)
      .withBody(Json.toJson(formIdEntry))
      .execute[HttpResponse]
      .recover { case e: Exception =>
        HttpResponse(BAD_GATEWAY, e.getMessage)
      }

  def getGmcBatches(version: String)(implicit hc: HeaderCarrier): Future[HttpResponse] =
    httpClient
      .get(new URI(s"${serviceUrl(Some(version))}/admin/message/brake/gmc/batches").toURL)
      .execute[HttpResponse]
      .recover { case e: Exception =>
        HttpResponse(BAD_GATEWAY, e.getMessage)
      }

  def getRandomMessagePreview(batch: GmcBatch)(implicit hc: HeaderCarrier): Future[HttpResponse] =
    httpClient
      .post(new URI(s"${serviceUrl(batch.version)}/admin/message/brake/random").toURL)
      .withBody(Json.toJson(batch))
      .execute[HttpResponse]
      .recover { case e: Exception =>
        HttpResponse(BAD_GATEWAY, e.getMessage)
      }

  def approveGmcBatch(batch: GmcBatchApproval)(implicit hc: HeaderCarrier): Future[HttpResponse] =
    httpClient
      .post(new URI(s"${serviceUrl(batch.version)}/admin/message/brake/accept").toURL)
      .withBody(Json.toJson(batch))
      .execute[HttpResponse]
      .recover { case e: Exception =>
        HttpResponse(BAD_GATEWAY, e.getMessage)
      }

  def rejectGmcBatch(batch: GmcBatchApproval)(implicit hc: HeaderCarrier): Future[HttpResponse] =
    httpClient
      .post(new URI(s"${serviceUrl(batch.version)}/admin/message/brake/reject").toURL)
      .withBody(Json.toJson(batch))
      .execute[HttpResponse]
      .recover { case e: Exception =>
        HttpResponse(BAD_GATEWAY, e.getMessage)
      }

  def sendMessage(body: JsValue)(implicit hc: HeaderCarrier): Future[HttpResponse] =
    httpClient
      .post(new URI(s"$serviceUrl/messages").toURL)
      .withBody(Json.toJson(body))
      .execute[HttpResponse]
      .recover { case e: Exception =>
        HttpResponse(BAD_GATEWAY, e.getMessage)
      }

}
