/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.preferencesadminfrontend.services

import java.util.UUID

import org.apache.commons.codec.binary.Base64
import javax.inject.Inject
import play.api.http.Status._
import play.api.libs.json.{ JsError, JsObject, JsSuccess, Json, __ }
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.preferencesadminfrontend.connectors.MessageConnector
import uk.gov.hmrc.preferencesadminfrontend.model.{ BatchMessagePreview, GmcBatch, MessagePreview }

import scala.concurrent.{ ExecutionContext, Future }
import scala.io.Source
import uk.gov.hmrc.templates.views.html.penaltyChargeApologies

class MessageService @Inject()(messageConnector: MessageConnector) {

  def getGmcBatches()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Either[Seq[GmcBatch], String]] =
    messageConnector.getGmcBatches.map(response =>
      response.status match {
        case OK =>
          Json.parse(response.body).validate[Seq[GmcBatch]].asOpt match {
            case Some(batches) => Left(batches)
            case None          => Right("The GMC batches retrieved do not appear to be valid.")
          }
        case _ => Right(response.body)
    })

  def getRandomMessagePreview(batch: GmcBatch)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Either[BatchMessagePreview, String]] =
    messageConnector
      .getRandomMessagePreview(batch)
      .map(response =>
        response.status match {
          case OK =>
            Json.parse(response.body).validate[MessagePreview].asOpt match {
              case Some(preview) => Left(BatchMessagePreview(preview, batch.batchId))
              case None          => Right("The message preview retrieved does not appear to be valid.")
            }
          case _ => Right(response.body)
      })

  type ResponseStatus = Int
  type ResponseBody = String
  type MessageId = String

  val messageContent = Base64.encodeBase64String(penaltyChargeApologies().toString().getBytes("UTF-8"))

  def sendPenalyChargeApologyMessage(email: String, sautr: String)(
    implicit hc: HeaderCarrier,
    ec: ExecutionContext): Future[Either[(ResponseStatus, ResponseBody), String]] = {
    val aMessage: JsObject = Json
      .parse(
        Source.fromURL(getClass.getResource("/message.json")).mkString
      )
      .as[JsObject]
      .deepMerge(Json.obj("content" -> messageContent))
      .deepMerge(Json.obj("recipient" -> Map("taxIdentifier" -> Map("value" -> sautr))))
      .deepMerge(Json.obj("recipient" -> Map("email" -> email)))
      .deepMerge(Json.obj("externalRef" -> Map("id" -> UUID.randomUUID().toString)))

    messageConnector
      .sendMessage(aMessage)
      .map(response =>
        response.status match {
          case CREATED =>
            response.json.validate((__ \ 'id).json.pick) match {
              case JsSuccess(value, path) => Right(Json.stringify(value))
            }
          case _ => Left((response.status, response.body))
      })
  }

}
