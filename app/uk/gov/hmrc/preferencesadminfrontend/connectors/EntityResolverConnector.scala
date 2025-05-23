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
import java.time.{ Instant, ZonedDateTime }
import play.api.Logger
import play.api.http.Status
import play.api.libs.functional.syntax._
import play.api.libs.json._
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.preferencesadminfrontend.services.model.{ Email, EntityId, TaxIdentifier }
import cats.syntax.either._
import uk.gov.hmrc.http.HttpReads.Implicits.{ readFromJson, readOptionOfNotFound, readRaw }
import uk.gov.hmrc.http.client.HttpClientV2

import java.net.URI
import scala.concurrent.{ ExecutionContext, Future }
import scala.util.Try

@Singleton
class EntityResolverConnector @Inject() (httpClient: HttpClientV2, val servicesConfig: ServicesConfig) {

  val logger = Logger(getClass)
  implicit val ef: Format[Entity] = Entity.formats

  def serviceUrl = servicesConfig.baseUrl("entity-resolver")

  def getTaxIdentifiers(
    taxId: TaxIdentifier
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Seq[TaxIdentifier]] = {
    def warnNotOptedOut(message: String) = s"getTaxIdentifiersTaxId $message"
    val response =
      httpClient
        .get(new URI(s"$serviceUrl/entity-resolver/${taxId.regime}/${taxId.value}").toURL)
        .execute[Option[Entity]]
    response
      .map(
        _.fold(Seq.empty[TaxIdentifier])(entity =>
          Seq(
            entity.sautr.map(TaxIdentifier("sautr", _)),
            entity.nino.map(TaxIdentifier("nino", _)),
            entity.itsaId.map(TaxIdentifier("HMRC-MTD-IT", _))
          ).flatten
        )
      )
      .recover {
        case ex: BadRequestException =>
          warnNotOptedOut(ex.message)
          Seq.empty
        case ex @ UpstreamErrorResponse(_, Status.NOT_FOUND, _, _) =>
          warnNotOptedOut(ex.message)
          Seq.empty
        case ex @ UpstreamErrorResponse(_, Status.CONFLICT, _, _) =>
          warnNotOptedOut(ex.message)
          Seq.empty
        case ex =>
          warnNotOptedOut(ex.getMessage)
          Seq.empty
      }
  }

  def getTaxIdentifiers(
    preferenceDetails: PreferenceDetails
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Seq[TaxIdentifier]] = {
    def warnNotOptedOut(message: String) = s"getTaxIdentifiersPreferenceDetails $message"
    val response =
      httpClient
        .get(new URI(s"$serviceUrl/entity-resolver/${preferenceDetails.entityId.get}").toURL)
        .execute[Option[Entity]]
    response
      .map(
        _.fold(Seq.empty[TaxIdentifier])(entity =>
          Seq(
            entity.sautr.map(TaxIdentifier("sautr", _)),
            entity.nino.map(TaxIdentifier("nino", _)),
            entity.itsaId.map(TaxIdentifier("HMRC-MTD-IT", _))
          ).flatten
        )
      )
      .recover {
        case ex: BadRequestException =>
          warnNotOptedOut(ex.message)
          Seq.empty
        case ex @ UpstreamErrorResponse(_, Status.NOT_FOUND, _, _) =>
          warnNotOptedOut(ex.message)
          Seq.empty
        case ex @ UpstreamErrorResponse(_, Status.CONFLICT, _, _) =>
          warnNotOptedOut(ex.message)
          Seq.empty
        case ex =>
          warnNotOptedOut(ex.getMessage)
          Seq.empty
      }
  }

  def getPreferenceDetails(
    taxId: TaxIdentifier
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[PreferenceDetails]] = {
    def warnNotOptedOut(message: String) = s"getTaxIdentifiersPreferenceDetails $message"
    httpClient
      .get(new URI(s"$serviceUrl/portal/preferences/${taxId.regime}/${taxId.value}").toURL)
      .execute[Option[PreferenceDetails]]
      .recover {
        case ex: BadRequestException =>
          warnNotOptedOut(ex.message)
          None
        case ex @ UpstreamErrorResponse(_, Status.NOT_FOUND, _, _) =>
          warnNotOptedOut(ex.message)
          None
        case ex @ UpstreamErrorResponse(_, Status.CONFLICT, _, _) =>
          warnNotOptedOut(ex.message)
          None
        case ex =>
          warnNotOptedOut(ex.getMessage)
          None
      }
  }

  def optOut(taxId: TaxIdentifier)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[OptOutResult] = {
    def warnNotOptedOut(status: Int): Unit =
      logger.warn(s"Unable to manually opt-out ${taxId.name} user with id ${taxId.value}. Status: $status")

    httpClient
      .post(new URI(s"$serviceUrl/entity-resolver-admin/manual-opt-out/${taxId.regime}/${taxId.value}").toURL)
      .execute[HttpResponse]
      .map(_ => OptedOut)
      .recover {
        case _: NotFoundException =>
          warnNotOptedOut(404)
          PreferenceNotFound
        case ex @ UpstreamErrorResponse(_, Status.CONFLICT, _, _) =>
          warnNotOptedOut(ex.statusCode)
          AlreadyOptedOut
        case ex @ UpstreamErrorResponse(_, Status.NOT_FOUND, _, _) =>
          warnNotOptedOut(ex.statusCode)
          PreferenceNotFound
        case ex @ UpstreamErrorResponse(_, Status.PRECONDITION_FAILED, _, _) =>
          warnNotOptedOut(ex.statusCode)
          PreferenceNotFound
      }
  }

  def confirm(entityId: String, itsaId: String)(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Either[String, Unit]] =
    httpClient
      .post(new URI(s"$serviceUrl/preferences/confirm/$entityId/$itsaId").toURL)
      .transform(_.addHttpHeaders(hc.headers(Seq(HeaderNames.authorisation)): _*))
      .execute[HttpResponse]
      .map { httpResponse =>
        httpResponse.status match {
          case status if Status.isSuccessful(status) => ().asRight
          case other => s"upstream error when confirming ITSA preference, $other ${httpResponse.body}".asLeft
        }
      }
}

sealed trait OptOutResult

case object OptedOut extends OptOutResult

case object AlreadyOptedOut extends OptOutResult {
  val errorCode: String = "AlreadyOptedOut"
}

case object PreferenceNotFound extends OptOutResult {
  val errorCode: String = "PreferenceNotFound"
}

case class Entity(sautr: Option[String], nino: Option[String], itsaId: Option[String])

object Entity {
  implicit val writes: Writes[Entity] = Json.writes[Entity]

  implicit val reads: Reads[Entity] = (
    (JsPath \ "sautr").readNullable[String] and
      (JsPath \ "nino").readNullable[String] and
      (JsPath \ "HMRC-MTD-IT").readNullable[String]
  )((sautr, nino, itsaid) => Entity(sautr, nino, itsaid))

  implicit val formats: Format[Entity] = Format(reads, writes)
}

case class PreferenceDetails(
  genericPaperless: Boolean,
  genericUpdatedAt: Option[ZonedDateTime],
  isPaperless: Option[Boolean],
  email: Option[Email],
  entityId: Option[EntityId] = None,
  eventType: Option[String] = None,
  viaMobileApp: Option[Boolean] = None
)

object PreferenceDetails {
  implicit val localDateRead: Reads[Option[ZonedDateTime]] = new Reads[Option[ZonedDateTime]] {
    override def reads(json: JsValue): JsResult[Option[ZonedDateTime]] =
      json match {
        case JsNumber(dateTime) =>
          Try {
            JsSuccess(Some(ZonedDateTime.from(Instant.ofEpochMilli(dateTime.longValue))))
          }.getOrElse {
            JsError(s"$dateTime is not a valid date")
          }
        case _ => JsError(s"Expected value to be a date, was actually $json")
      }
  }
  implicit val dateFormatDefault: Format[ZonedDateTime] = new Format[ZonedDateTime] {
    override def reads(json: JsValue): JsResult[ZonedDateTime] = Reads.DefaultZonedDateTimeReads.reads(json)
    override def writes(o: ZonedDateTime): JsValue = Writes.ZonedDateTimeEpochMilliWrites.writes(o)
  }
  implicit val reads: Reads[PreferenceDetails] = (
    (JsPath \ "termsAndConditions" \ "generic")
      .readNullable[JsValue]
      .map(_.fold(false)(m => (m \ "accepted").as[Boolean])) and
      (JsPath \ "termsAndConditions" \ "generic")
        .readNullable[JsValue]
        .map(_.fold(None: Option[ZonedDateTime])(m => (m \ "updatedAt").asOpt[ZonedDateTime])) and
      (JsPath \ "termsAndConditions" \ "generic")
        .readNullable[JsValue]
        .map(_.fold(None: Option[Boolean])(m => (m \ "paperless").asOpt[Boolean])) and
      (JsPath \ "email").readNullable[Email] and
      (JsPath \ "entityId").readNullable[EntityId] and
      (JsPath \ "termsAndConditions" \ "generic" \ "eventType").readNullable[String] and
      (JsPath \ "termsAndConditions" \ "generic" \ "isViaMobileApp").readNullable[Boolean]
  )((genericPaperless, genericUpdatedAt, isPaperless, email, entityId, eventType, viaMobileApp) =>
    PreferenceDetails(
      genericPaperless,
      genericUpdatedAt,
      isPaperless,
      email,
      entityId,
      eventType,
      viaMobileApp
    )
  )
}
