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

import cats.syntax.option.*
import org.mockito.ArgumentMatchers.eq as eql
import org.mockito.Mockito.when
import org.mockito.ArgumentMatchers.any
import org.scalatest.EitherValues
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.Json
import uk.gov.hmrc.http.{ HeaderCarrier, HttpResponse }
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.preferencesadminfrontend.model.UserState.Activated
import uk.gov.hmrc.preferencesadminfrontend.model.{ PrincipalUserIds, UserState }
import uk.gov.hmrc.preferencesadminfrontend.services.model.TaxIdentifier
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw
import uk.gov.hmrc.http.client.{ HttpClientV2, RequestBuilder }

import java.net.{ URI, URL }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EnrolmentStoreConnectorSpec extends PlaySpec with ScalaFutures with EitherValues with GuiceOneAppPerSuite {

  "getUserIds" must {
    "return right list of principal ids upon success" in new Scope {
      when(httpClient.get(eql(expectedPrincipalPath))(any)).thenReturn(requestBuilder)
      when(requestBuilder.execute[HttpResponse])
        .thenReturn(Future.successful(HttpResponse(OK, Json.toJson(principalUserIds).toString())))

      enrolmentStoreConnector
        .getUserIds(taxId)(headerCarrier)
        .futureValue mustBe Right(List(principalUserId))
    }

    "return an empty list of principal user ids when NO_CONTENT is returned" in new Scope {
      when(httpClient.get(eql(expectedPrincipalPath))(any)).thenReturn(requestBuilder)
      when(requestBuilder.execute[HttpResponse]).thenReturn(Future.successful(httpResponse(NoContent, body = "")))

      enrolmentStoreConnector
        .getUserIds(taxId)(headerCarrier)
        .futureValue mustBe Right(List.empty)
    }

    "return an upstream error when neither OK or NO_CONTENT status code is returned" in new Scope {
      when(httpClient.get(eql(expectedPrincipalPath))(any)).thenReturn(requestBuilder)
      when(requestBuilder.execute[HttpResponse]).thenReturn(Future.successful(httpResponse(Bad, body = "BAD_NEWS")))

      enrolmentStoreConnector
        .getUserIds(taxId)(headerCarrier)
        .futureValue
        .left
        .value mustBe s"upstream error when getting principals, $Bad BAD_NEWS"
    }
  }

  "getUserState" must {
    "return right some users state upon success" in new Scope {
      when(httpClient.get(eql(expectedUserStatePath))(any)).thenReturn(requestBuilder)
      when(requestBuilder.execute[HttpResponse])
        .thenReturn(Future.successful(httpResponse(OK, body = Json.toJson(userState).toString())))

      enrolmentStoreConnector
        .getUserState(principalUserId, taxId)(headerCarrier)
        .futureValue mustBe Right(userState.some)
    }

    "return right none when a NOT_FOUND status is returned" in new Scope {
      when(httpClient.get(eql(expectedUserStatePath))(any)).thenReturn(requestBuilder)
      when(requestBuilder.execute[HttpResponse]).thenReturn(Future.successful(httpResponse(NotFound, body = "")))

      enrolmentStoreConnector
        .getUserState(principalUserId, taxId)(headerCarrier)
        .futureValue mustBe Right(None)
    }

    "return an upstream error when neither OK or NOT_FOUND status code is returned" in new Scope {
      when(httpClient.get(eql(expectedUserStatePath))(any)).thenReturn(requestBuilder)
      when(requestBuilder.execute[HttpResponse]).thenReturn(Future.successful(httpResponse(Bad, body = "BAD_NEWS")))

      enrolmentStoreConnector
        .getUserState(principalUserId, taxId)(headerCarrier)
        .futureValue
        .left
        .value mustBe s"upstream error when checking enrolment state, $Bad BAD_NEWS"
    }
  }

  trait Scope {
    val OK = 200
    val NoContent = 204
    val Bad = 400
    val NotFound = 404

    val saUtr = "MY-UTR"
    val taxId: TaxIdentifier = TaxIdentifier("sautr", saUtr)

    val principalUserId: String = "6696231619140440"
    val principalUserIds: PrincipalUserIds = Json
      .parse("""{
               |
               |    "principalUserIds":[
               |        "6696231619140440"
               |    ]
               |
               |
               |}""".stripMargin)
      .as[PrincipalUserIds]
    val userState: UserState = UserState(Activated)

    implicit val headerCarrier: HeaderCarrier = HeaderCarrier()
    val httpClient: HttpClientV2 = mock[HttpClientV2]
    val requestBuilder: RequestBuilder = mock[RequestBuilder]
    val servicesConfig: ServicesConfig = app.injector.instanceOf[ServicesConfig]

    val enrolmentStoreConnector = new EnrolmentStoreConnector(httpClient, servicesConfig)
    val enrolmentStoreServiceUrl: String = app.injector.instanceOf[ServicesConfig].baseUrl("enrolment-store")
    val expectedPrincipalPath: URL =
      new URI(
        s"$enrolmentStoreServiceUrl/enrolment-store-proxy/enrolment-store/enrolments/IR-SA~UTR~$saUtr/users?type=principal"
      ).toURL
    val expectedUserStatePath: URL =
      new URI(
        s"$enrolmentStoreServiceUrl/enrolment-store-proxy/enrolment-store/users/$principalUserId/enrolments/IR-SA~UTR~$saUtr"
      ).toURL

    def httpResponse(status: Int, body: String): HttpResponse = HttpResponse(
      status = status,
      body = body,
      headers = Map.empty
    )
  }
}
