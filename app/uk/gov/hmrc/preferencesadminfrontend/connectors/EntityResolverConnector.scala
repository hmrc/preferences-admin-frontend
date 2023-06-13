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
import org.joda.time.{ DateTime, DateTimeZone }
import play.api.Logger
import play.api.http.Status
import play.api.libs.functional.syntax._
import play.api.libs.json._
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.preferencesadminfrontend.services.model.{ Email, EntityId, TaxIdentifier }
import cats.syntax.either._
import uk.gov.hmrc.http.HttpReads.Implicits.{ readFromJson, readOptionOfNotFound, readRaw }
import scala.concurrent.{ ExecutionContext, Future }
import scala.util.Try

@Singleton
class EntityResolverConnector @Inject()(httpClient: HttpClient, val servicesConfig: ServicesConfig) {

  val logger = Logger(getClass)
  implicit val ef = Entity.formats

  def serviceUrl = servicesConfig.baseUrl("entity-resolver")

  def getTaxIdentifiers(taxId: TaxIdentifier)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Seq[TaxIdentifier]] = {
    def warnNotOptedOut(message: String) = s"getTaxIdentifiersTaxId $message"
    val response = httpClient.GET[Option[Entity]](s"$serviceUrl/entity-resolver/${taxId.regime}/${taxId.value}")
    response
      .map(
        _.fold(Seq.empty[TaxIdentifier])(
          entity =>
            Seq(
              entity.sautr.map(TaxIdentifier("sautr", _)),
              entity.nino.map(TaxIdentifier("nino", _)),
              entity.itsaId.map(TaxIdentifier("HMRC-MTD-IT", _))
            ).flatten)
      )
      .recover {
        case ex: BadRequestException => {
          warnNotOptedOut(ex.message)
          Seq.empty
        }
        case ex @ UpstreamErrorResponse(_, Status.NOT_FOUND, _, _) => {
          warnNotOptedOut(ex.message)
          Seq.empty
        }
        case ex @ UpstreamErrorResponse(_, Status.CONFLICT, _, _) => {
          warnNotOptedOut(ex.message)
          Seq.empty
        }
        case ex => {
          warnNotOptedOut(ex.getMessage)
          Seq.empty
        }
      }
  }

  def getTaxIdentifiers(preferenceDetails: PreferenceDetails)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Seq[TaxIdentifier]] = {
    def warnNotOptedOut(message: String) = s"getTaxIdentifiersPreferenceDetails $message"
    val response = httpClient.GET[Option[Entity]](s"$serviceUrl/entity-resolver/${preferenceDetails.entityId.get}")
    response
      .map(
        _.fold(Seq.empty[TaxIdentifier])(
          entity =>
            Seq(
              entity.sautr.map(TaxIdentifier("sautr", _)),
              entity.nino.map(TaxIdentifier("nino", _)),
              entity.itsaId.map(TaxIdentifier("HMRC-MTD-IT", _))
            ).flatten)
      )
      .recover {
        case ex: BadRequestException => {
          warnNotOptedOut(ex.message)
          Seq.empty
        }
        case ex @ UpstreamErrorResponse(_, Status.NOT_FOUND, _, _) => {
          warnNotOptedOut(ex.message)
          Seq.empty
        }
        case ex @ UpstreamErrorResponse(_, Status.CONFLICT, _, _) => {
          warnNotOptedOut(ex.message)
          Seq.empty
        }
        case ex => {
          warnNotOptedOut(ex.getMessage)
          Seq.empty
        }
      }
  }

  def getPreferenceDetails(taxId: TaxIdentifier)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[PreferenceDetails]] = {
    def warnNotOptedOut(message: String) = s"getTaxIdentifiersPreferenceDetails $message"
    httpClient.GET[Option[PreferenceDetails]](s"$serviceUrl/portal/preferences/${taxId.regime}/${taxId.value}").recover {
      case ex: BadRequestException => {
        warnNotOptedOut(ex.message)
        None
      }
      case ex @ UpstreamErrorResponse(_, Status.NOT_FOUND, _, _) => {
        warnNotOptedOut(ex.message)
        None
      }
      case ex @ UpstreamErrorResponse(_, Status.CONFLICT, _, _) => {
        warnNotOptedOut(ex.message)
        None
      }
      case ex => {
        warnNotOptedOut(ex.getMessage)
        None
      }
    }
  }

  def optOut(taxId: TaxIdentifier)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[OptOutResult] = {
    def warnNotOptedOut(status: Int): Unit = logger.warn(s"Unable to manually opt-out ${taxId.name} user with id ${taxId.value}. Status: $status")

    httpClient
      .POSTEmpty[HttpResponse](s"$serviceUrl/entity-resolver-admin/manual-opt-out/${taxId.regime}/${taxId.value}")
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

  def confirm(entityId: String, itsaId: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Either[String, Unit]] =
    httpClient
      .doEmptyPost(
        s"$serviceUrl/preferences/confirm/$entityId/$itsaId",
        hc.headers(Seq(HeaderNames.authorisation))
      )
      .map { httpResponse =>
        httpResponse.status match {
          case status if Status.isSuccessful(status) => ().asRight
          case other                                 => s"upstream error when confirming ITSA preference, $other ${httpResponse.body}".asLeft
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
  genericUpdatedAt: Option[DateTime],
  isPaperless: Option[Boolean],
  email: Option[Email],
  entityId: Option[EntityId] = None)

object PreferenceDetails {
  implicit val localDateRead: Reads[Option[DateTime]] = new Reads[Option[DateTime]] {
    override def reads(json: JsValue): JsResult[Option[DateTime]] =
      json match {
        case JsNumber(dateTime) =>
          Try {
            JsSuccess(Some(new DateTime(dateTime.longValue, DateTimeZone.UTC)))
          }.getOrElse {
            JsError(s"$dateTime is not a valid date")
          }
        case _ => JsError(s"Expected value to be a date, was actually $json")
      }
  }
  implicit val dateFormatDefault = new Format[DateTime] {
    override def reads(json: JsValue): JsResult[DateTime] = JodaReads.DefaultJodaDateTimeReads.reads(json)
    override def writes(o: DateTime): JsValue = JodaWrites.JodaDateTimeNumberWrites.writes(o)
  }
  implicit val reads: Reads[PreferenceDetails] = (
    (JsPath \ "termsAndConditions" \ "generic").readNullable[JsValue].map(_.fold(false)(m => (m \ "accepted").as[Boolean])) and
      (JsPath \ "termsAndConditions" \ "generic").readNullable[JsValue].map(_.fold(None: Option[DateTime])(m => (m \ "updatedAt").asOpt[DateTime])) and
      (JsPath \ "termsAndConditions" \ "generic").readNullable[JsValue].map(_.fold(None: Option[Boolean])(m => (m \ "paperless").asOpt[Boolean])) and
      (JsPath \ "email").readNullable[Email] and
      (JsPath \ "entityId").readNullable[EntityId]
  )((genericPaperless, genericUpdatedAt, isPaperless, email, entityId) => PreferenceDetails(genericPaperless, genericUpdatedAt, isPaperless, email, entityId))
}
