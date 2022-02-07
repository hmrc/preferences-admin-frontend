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

import cats.data.EitherT
import cats.implicits.catsSyntaxTuple2Semigroupal
import cats.syntax.either._
import cats.syntax.option._
import play.api.{ Logger, Logging }
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.preferencesadminfrontend.connectors.{ EnrolmentStoreConnector, EntityResolverConnector, PreferenceDetails }
import uk.gov.hmrc.preferencesadminfrontend.model.MTDPMigration._
import uk.gov.hmrc.preferencesadminfrontend.model.{ PrincipalUserId, UserState }
import uk.gov.hmrc.preferencesadminfrontend.services.model._

import javax.inject.{ Inject, Singleton }
import scala.concurrent.{ ExecutionContext, Future }

@Singleton
class CustomerMigrationResolver @Inject()(
  enrolmentStoreConnector: EnrolmentStoreConnector,
  entityResolverConnector: EntityResolverConnector
)(implicit executionContext: ExecutionContext) {
  val logger = Logger(getClass)
  def resolveCustomerType(identifier: Identifier)(implicit head: HeaderCarrier): Future[Either[String, CustomerType]] =
    getEnrolments(identifier)
      .flatMap(resolve(_, identifier))
      .value

  private def getEnrolments(identifier: Identifier)(implicit headerCarrier: HeaderCarrier): EitherT[Future, String, Enrolments] = {
    val saUtrTaxId = TaxIdentifier("sautr", identifier.utr)
    val itsaTaxId = TaxIdentifier("itsa", identifier.itsaId)
    logger.info(s"getEnrolments for $saUtrTaxId and $itsaTaxId")
    for {
      saEnrolment   <- EitherT(enrolmentStoreConnector.getUserIds(saUtrTaxId))
      saPrincipal   <- EitherT.fromEither[Future](validatePrincipal(saEnrolment))
      saStatus      <- EitherT(getSaStatus(saPrincipal, identifier))
      itsaEnrolment <- EitherT(enrolmentStoreConnector.getUserIds(itsaTaxId))
      itsaPrinciple <- EitherT.fromEither[Future](validatePrincipal(itsaEnrolment))
    } yield {

      logger.info(s"AdminGetEnrolmentssaPrincipal: $saPrincipal")
      logger.info(s"AdminGetEnrolmentssaStatus: $saStatus")
      logger.info(s"AdminGetEnrolmentsitsaPrinciple: $itsaPrinciple")

      Enrolments(
        (saPrincipal, saStatus).mapN(StatefulSAEnrolment.apply),
        itsaPrinciple
      )
    }
  }

  private def resolve(enrolments: Enrolments, identifier: Identifier)(implicit headerCarrier: HeaderCarrier): EitherT[Future, String, CustomerType] = {
    val resolution = enrolments match {
      case Enrolments(Some(ActivatedSAEnrolment(_)), Some(_)) => checkITSAAndSA(identifier).map(_.asRight)
      case Enrolments(Some(ActivatedSAEnrolment(_)), None)    => checkSAPreference(identifier).map(_.getOrElse(NoDigitalFootprint).asRight)
      case Enrolments(_, Some(_))                             => checkITSAPreference(identifier).map(_.getOrElse(ITSAOnlineNoPreference).asRight[String])
      case Enrolments(_, None)                                => Future.successful(NoDigitalFootprint.asRight)
    }

    EitherT(resolution)
  }

  private def checkITSAAndSA(identifier: Identifier)(implicit headerCarrier: HeaderCarrier): Future[CustomerType] =
    for {
      saPreference   <- checkSAPreference(identifier)
      itsaPreference <- checkITSAPreference(identifier)
    } yield
      (saPreference, itsaPreference) match {
        case (Some(_), Some(_)) => {
          logger.info(s"AdmincheckITSAAndSA: $saPreference - $itsaPreference")
          SAandITSA
        }
        case (None, Some(itsaOnline)) => {
          logger.info(s"AdmincheckITSAAndSAitsaOnline: $itsaOnline")
          itsaOnline
        }
        case _ => {
          logger.info(s"AdmincheckITSAAndSA: noPreference")
          ITSAOnlineNoPreference
        }
      }

  private def getSAPreference(identifier: Identifier)(implicit headerCarrier: HeaderCarrier): Future[Option[PreferenceDetails]] =
    entityResolverConnector.getPreferenceDetails(TaxIdentifier("sautr", identifier.utr))

  private def checkSAPreference(identifier: Identifier)(implicit headerCarrier: HeaderCarrier): Future[Option[CustomerType]] =
    getSAPreference(identifier).map(_.flatMap(maybeSAPreference))

  private def checkITSAPreference(identifier: Identifier)(implicit headerCarrier: HeaderCarrier): Future[Option[CustomerType]] =
    getITSAPreference(identifier).map(_.flatMap(maybeITSAPreference))

  def maybeITSAPreference(preferenceDetails: PreferenceDetails): Option[ITSAOnlinePreference] =
    preferenceDetails.isPaperless.map(ITSAOnlinePreference)

  def maybeSAPreference(preferenceDetails: PreferenceDetails): Option[SAOnline] =
    preferenceDetails.entityId.map(SAOnline)

  private def getITSAPreference(identifier: Identifier)(implicit headerCarrier: HeaderCarrier): Future[Option[PreferenceDetails]] =
    entityResolverConnector.getPreferenceDetails(TaxIdentifier("itsa", identifier.itsaId))

  private def validatePrincipal(principals: List[PrincipalUserId]): Either[String, Option[PrincipalUserId]] =
    principals match {
      case one :: Nil => one.some.asRight
      case Nil        => none.asRight
      case more       => s"Too many principal identifiers, ${more.size}.".asLeft
    }

  private def getSaStatus(
    principalUserId: Option[PrincipalUserId],
    identifier: Identifier
  )(implicit headerCarrier: HeaderCarrier): Future[Either[String, Option[UserState]]] =
    principalUserId
      .map(enrolmentStoreConnector.getUserState(_, TaxIdentifier("sautr", identifier.utr)))
      .getOrElse(Future.successful(none.asRight))
}
