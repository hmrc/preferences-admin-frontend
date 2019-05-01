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
import org.joda.time.{DateTime, DateTimeZone}
import play.api.Mode.Mode
import play.api.{Configuration, Environment, Logger, Play}
import play.api.http.Status
import play.api.libs.functional.syntax._
import play.api.libs.json._
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.ws.{WSGet, WSPost}
import uk.gov.hmrc.preferencesadminfrontend.services.model.{Email, EntityId, TaxIdentifier}

import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.hooks.HttpHook
import uk.gov.hmrc.preferencesadminfrontend.FrontendAuditConnector
import uk.gov.hmrc.play.audit.http.HttpAuditing
import uk.gov.hmrc.play.config.AppName

import scala.util.Try

@Singleton
class EntityResolverConnector @Inject()(frontendAuditConnector: FrontendAuditConnector,
                                        environment: Environment,
                                        val runModeConfiguration: Configuration,
                                        val actorSystem: ActorSystem) extends HttpGet with WSGet
  with HttpPost with WSPost with HttpAuditing with AppName with ServicesConfig {

  implicit val ef = Entity.formats

  override protected def mode: Mode = environment.mode
  override def appNameConfiguration: Configuration = Play.current.configuration
  override lazy val configuration: Option[Config] = None

  val hooks: Seq[HttpHook] = Seq()

  override val auditConnector = frontendAuditConnector

  def serviceUrl = baseUrl("entity-resolver")

  def getTaxIdentifiers(taxId: TaxIdentifier)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Seq[TaxIdentifier]] = {
    val response = GET[Option[Entity]](s"$serviceUrl/entity-resolver/${taxId.regime}/${taxId.value}")
    response.map(
      _.fold(Seq.empty[TaxIdentifier])(entity =>
        Seq(
          entity.sautr.map(TaxIdentifier("sautr", _)),
          entity.nino.map(TaxIdentifier("nino", _))
        ).flatten)
    ).recover {
      case ex: BadRequestException => Seq.empty
    }
  }

  def getTaxIdentifiers(preferenceDetails: PreferenceDetails)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Seq[TaxIdentifier]] = {
      val response = GET[Option[Entity]](s"$serviceUrl/entity-resolver/${preferenceDetails.entityId.get}")
      response.map(
        _.fold(Seq.empty[TaxIdentifier])(entity =>
          Seq(
            entity.sautr.map(TaxIdentifier("sautr", _)),
            entity.nino.map(TaxIdentifier("nino", _))
          ).flatten)
      ).recover {
        case ex: BadRequestException => Seq.empty
      }
  }


  def getPreferenceDetails(taxId: TaxIdentifier)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[PreferenceDetails]] = {
    GET[Option[PreferenceDetails]](s"$serviceUrl/portal/preferences/${taxId.regime}/${taxId.value}").recover {
      case ex: BadRequestException => None
    }
  }

  def optOut(taxId: TaxIdentifier)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[OptOutResult] = {

    def warnNotOptedOut(status: Int) = Logger.warn(s"Unable to manually opt-out ${taxId.name} user with id ${taxId.value}. Status: $status")

    POSTEmpty(s"$serviceUrl/entity-resolver-admin/manual-opt-out/${taxId.regime}/${taxId.value}")
      .map(_ => OptedOut)
      .recover {
        case ex: NotFoundException =>
          warnNotOptedOut(404)
          PreferenceNotFound
        case ex@Upstream4xxResponse(_, Status.CONFLICT, _, _) =>
          warnNotOptedOut(ex.upstreamResponseCode)
          AlreadyOptedOut
        case ex@Upstream4xxResponse(_, Status.PRECONDITION_FAILED, _, _) =>
          warnNotOptedOut(ex.upstreamResponseCode)
          PreferenceNotFound
      }
  }
}

trait OptOutResult

case object OptedOut extends OptOutResult

case object AlreadyOptedOut extends OptOutResult {
  val errorCode: String = "AlreadyOptedOut"
}

case object PreferenceNotFound extends OptOutResult {
  val errorCode: String = "PreferenceNotFound"
}


case class Entity(sautr: Option[String], nino: Option[String])

object Entity {
  val formats = Json.format[Entity]
}

case class PreferenceDetails(genericPaperless: Boolean, genericUpdatedAt : Option[DateTime], taxCreditsPaperless: Boolean, taxCreditsUpdatedAt : Option[DateTime], email: Option[Email], entityId: Option[EntityId] = None)

object PreferenceDetails {
  implicit val localDateRead: Reads[Option[DateTime]] = new Reads[Option[DateTime]] {
    override def reads(json: JsValue): JsResult[Option[DateTime]] = {
      json match {
        case JsNumber(dateTime) => Try {
          JsSuccess(Some(new DateTime(dateTime.longValue, DateTimeZone.UTC)))
        }.getOrElse {
          JsError(s"$dateTime is not a valid date")
        }
        case _ => JsError(s"Expected value to be a date, was actually $json")
      }
    }
  }

  implicit val reads: Reads[PreferenceDetails] = (
    (JsPath \ "termsAndConditions" \ "generic").readNullable[JsValue].map(_.fold(false)(m => (m \ "accepted").as[Boolean])) and
    (JsPath \ "termsAndConditions" \ "generic").readNullable[JsValue].map(_.fold(None: Option[DateTime])(m => (m \ "updatedAt").asOpt[DateTime])) and
    (JsPath \ "termsAndConditions" \ "taxCredits").readNullable[JsValue].map(_.fold(false)(m => (m \ "accepted").as[Boolean])) and
    (JsPath \ "termsAndConditions" \ "taxCredits").readNullable[JsValue].map(_.fold(None: Option[DateTime])(m => (m \ "updatedAt").asOpt[DateTime])) and
      (JsPath \ "email").readNullable[Email] and
      (JsPath \ "entityId").readNullable[EntityId]
    ) ((genericPaperless, genericUpdatedAt, taxCreditsPaperless, taxCreditsUpdatedAt, email, entityId) => PreferenceDetails(genericPaperless, genericUpdatedAt, taxCreditsPaperless, taxCreditsUpdatedAt, email, entityId))
}