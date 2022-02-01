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

package uk.gov.hmrc.preferencesadminfrontend.connectors

import cats.data.EitherT
import cats.syntax.either._
import cats.syntax.option._
import play.api.Logging
import play.api.http.Status._
import uk.gov.hmrc.http.{ HeaderCarrier, HttpClient, HttpResponse }
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.preferencesadminfrontend.model.{ PrincipalUserId, PrincipalUserIds, UserState }
import uk.gov.hmrc.preferencesadminfrontend.services.model.TaxIdentifier

import javax.inject.{ Inject, Singleton }
import scala.concurrent.{ ExecutionContext, Future }

@Singleton
class EnrolmentStoreConnector @Inject()(httpClient: HttpClient, val servicesConfig: ServicesConfig)(implicit ec: ExecutionContext) extends Logging {

  def serviceUrl: String = servicesConfig.baseUrl("enrolment-store")

  def getUserIds(taxIdentifier: TaxIdentifier)(implicit hc: HeaderCarrier): Future[Either[String, List[PrincipalUserId]]] =
    (for {
      id <- EitherT.fromEither[Future](resolveId(taxIdentifier))
      response <- EitherT(
                   httpClient
                     .GET[HttpResponse](s"$serviceUrl/enrolment-store-proxy/enrolment-store/enrolments/$id/users?type=principal")
                     .map(handleGetUserIdsResponse))
      _ = logger.logger.warn("------------ getUserIds: " + response.map(_.id))
    } yield response).value

  def getUserState(principalUserId: PrincipalUserId, saUtr: TaxIdentifier)(implicit hc: HeaderCarrier): Future[Either[String, Option[UserState]]] =
    (for {
      id <- EitherT.fromEither[Future](resolveId(saUtr))
      response <- EitherT(
                   httpClient
                     .GET[HttpResponse](s"$serviceUrl/enrolment-store-proxy/enrolment-store/users/${principalUserId.id}/enrolments/$id")
                     .map(handleCheckEnrolmentsResponse))
      _ = logger.logger.warn("------------ getUserState: " + response.map(_.state))
    } yield response).value

  private def handleGetUserIdsResponse(httpResponse: HttpResponse): Either[String, List[PrincipalUserId]] =
    httpResponse.status match {
      case OK         => httpResponse.json.as[PrincipalUserIds].principalUserIds.asRight
      case NO_CONTENT => List.empty[PrincipalUserId].asRight
      case other      => s"upstream error when getting principals, $other ${httpResponse.body}".asLeft
    }

  private def handleCheckEnrolmentsResponse(httpResponse: HttpResponse): Either[String, Option[UserState]] =
    httpResponse.status match {
      case OK        => httpResponse.json.as[UserState].some.asRight
      case NOT_FOUND => none.asRight
      case other     => s"upstream error when checking enrolment state, $other ${httpResponse.body}".asLeft
    }

  def resolveId(taxIdentifier: TaxIdentifier): Either[String, String] =
    taxIdentifier.name match {
      case "sautr" => s"IR-SA~UTR~${taxIdentifier.value}".asRight
      case "itsa"  => s"HMRC-MTD-IT~MTDITID~${taxIdentifier.value}".asRight
      case _       => s"unknown tax identifier: ${taxIdentifier.name}, ${taxIdentifier.value}".asLeft
    }
}
