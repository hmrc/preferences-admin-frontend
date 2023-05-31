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

import org.joda.time.{ DateTime, DateTimeZone }
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status
import play.api.libs.json._
import uk.gov.hmrc.http.{ BadRequestException, HeaderCarrier, HttpClient, HttpResponse, Upstream4xxResponse }
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.preferencesadminfrontend.services.model.{ Email, TaxIdentifier }

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Random

class EntityResolverConnectorSpec extends PlaySpec with ScalaFutures with GuiceOneAppPerSuite {

  def entityResolverserviceUrl = app.injector.instanceOf[ServicesConfig].baseUrl("entity-resolver")

  "getTaxIdentifiers" must {
    "return only sautr if nino does not exist" in new TestCase {
      val expectedPath = s"$entityResolverserviceUrl/entity-resolver/sa/${sautr.value}"
      val responseJson = taxIdentifiersResponseFor(sautr)

      val result = entityConnectorGetEntityMock(expectedPath, responseJson).getTaxIdentifiers(sautr).futureValue

      result.size mustBe (1)
      result must contain(sautr)
    }

    "return all tax identifiers for sautr" in new TestCase {
      val expectedPath = s"$entityResolverserviceUrl/entity-resolver/sa/${sautr.value}"
      val responseJson = taxIdentifiersResponseFor(sautr, nino)

      val result = entityConnectorGetEntityMock(expectedPath, responseJson).getTaxIdentifiers(sautr).futureValue

      result.size mustBe (2)
      result must contain(nino)
      result must contain(sautr)
    }

    "return all tax identifiers for nino" in new TestCase {
      val expectedPath = s"$entityResolverserviceUrl/entity-resolver/paye/${nino.value}"
      val responseJson = taxIdentifiersResponseFor(sautr, nino)

      val result = entityConnectorGetEntityMock(expectedPath, responseJson).getTaxIdentifiers(nino).futureValue

      result.size mustBe (2)
      result must contain(nino)
      result must contain(sautr)
    }

    "return all tax identifiers for nino along with itsaId" in new TestCase {
      val expectedPath = s"$entityResolverserviceUrl/entity-resolver/paye/${nino.value}"
      val responseJson = taxIdentifiersResponseFor(sautr, nino, itsaId)

      val result = entityConnectorGetEntityMock(expectedPath, responseJson).getTaxIdentifiers(nino).futureValue

      result.size mustBe (3)
      result must contain(nino)
      result must contain(sautr)
      result must contain(itsaId)
    }

    "return all tax identifiers for sautr along with itsaId" in new TestCase {
      val expectedPath = s"$entityResolverserviceUrl/entity-resolver/sa/${sautr.value}"
      val responseJson = taxIdentifiersResponseFor(sautr, nino, itsaId)

      val result = entityConnectorGetEntityMock(expectedPath, responseJson).getTaxIdentifiers(sautr).futureValue

      result.size mustBe (3)
      result must contain(nino)
      result must contain(sautr)
      result must contain(itsaId)
    }

    "return empty sequence" in new TestCase {
      val expectedPath = s"$entityResolverserviceUrl/entity-resolver/paye/${nino.value}"

      val result = entityConnectorGetMock(expectedPath, new Upstream4xxResponse("", Status.CONFLICT, Status.CONFLICT))
        .getTaxIdentifiers(nino)
        .futureValue

      result.size mustBe (0)
    }

    "return empty sequence  if Entity-Resolver cannot parse parameter" in new TestCase {
      val expectedPath = s"$entityResolverserviceUrl/entity-resolver/paye/${nino.value}"
      val error = new BadRequestException(message = s"""'{"statusCode":400,"message":"Cannot parse parameter '${nino.name}' with value '${nino.value}'"}'""")

      val result = entityConnectorGetMock(expectedPath, error).getTaxIdentifiers(nino).futureValue

      result mustBe empty
    }
  }

  "getPreferenceDetails" must {
    val verfiedOn = Some(new DateTime(2018, 2, 15, 0, 0, DateTimeZone.UTC))

    "return generic paperless preference true and valid email address and verification true if user is opted in for saUtr" in new TestCase {
      val expectedPath = s"$entityResolverserviceUrl/portal/preferences/sa/${sautr.value}"
      val responseJson = preferenceDetailsResponseForGenericOptedIn(true)

      val result = entityConnectorGetPreferenceDetailsMock(expectedPath, responseJson).getPreferenceDetails(sautr).futureValue

      result mustBe defined
      result.get.genericPaperless mustBe true
      result.get.email.get.address mustBe "john.doe@digital.hmrc.gov.uk"
      result.get.email.get.verified mustBe true
      result.get.email.get.verifiedOn.get.isEqual(verfiedOn.get.getMillis) mustBe true
    }

    "return generic paperless preference true and valid email address and verification false if user is opted in for saUtr" in new TestCase {
      val expectedPath = s"$entityResolverserviceUrl/portal/preferences/sa/${sautr.value}"
      val responseJson = preferenceDetailsResponseForGenericOptedIn(false)

      val result = entityConnectorGetPreferenceDetailsMock(expectedPath, responseJson).getPreferenceDetails(sautr).futureValue

      result mustBe defined
      result.get.genericPaperless mustBe true
      result.get.email mustBe Some(Email("john.doe@digital.hmrc.gov.uk", false, None, None, false, None))
    }

    "return generic paperless preference false and email as 'None' if user is opted out for saUtr" in new TestCase {
      val expectedPath = s"$entityResolverserviceUrl/portal/preferences/sa/${sautr.value}"
      val responseJson = preferenceDetailsResponseForOptedOut()

      val result = entityConnectorGetPreferenceDetailsMock(expectedPath, responseJson).getPreferenceDetails(sautr).futureValue

      result mustBe defined
      result.get.genericPaperless mustBe false
      result.get.email mustBe None
    }

    "return email address and verification if user is opted in for nino" in new TestCase {
      val expectedPath = s"$entityResolverserviceUrl/portal/preferences/paye/${nino.value}"
      val responseJson = preferenceDetailsResponseForGenericOptedIn(true)

      val result = entityConnectorGetPreferenceDetailsMock(expectedPath, responseJson).getPreferenceDetails(nino).futureValue

      result mustBe defined
      result.get.genericPaperless mustBe true
      result.get.email.get.address mustBe "john.doe@digital.hmrc.gov.uk"
      result.get.email.get.verified mustBe true
      result.get.email.get.verifiedOn.get.isEqual(verfiedOn.get.getMillis) mustBe true
    }

    "return None if taxId does not exist" in new TestCase {
      val expectedPath = s"$entityResolverserviceUrl/portal/preferences/sa/${sautr.value}"
      val error = new Upstream4xxResponse("", Status.NOT_FOUND, Status.NOT_FOUND)
      val result = entityConnectorGetMock(expectedPath, error).getPreferenceDetails(sautr).futureValue

      result must not be defined
    }

    "return None if taxId is malformed" in new TestCase {
      val expectedPath = s"$entityResolverserviceUrl/portal/preferences/paye/${nino.value}"
      val error = new BadRequestException(message = s"""'{"statusCode":400,"message":"Cannot parse parameter '${nino.name}' with value '${nino.value}'"}'""")

      val result = entityConnectorGetMock(expectedPath, error).getPreferenceDetails(nino).futureValue

      result must not be defined
    }
  }

  "optOut" must {
    "return true if status is OK (user is opted out)" in new TestCase {
      val expectedPath = s"$entityResolverserviceUrl/entity-resolver-admin/manual-opt-out/sa/${sautr.value}"

      val result = entityConnectorPostMock(expectedPath, emptyJson).optOut(sautr).futureValue

      result mustBe OptedOut
    }

    "return false if CONFLICT" in new TestCase {
      val expectedPath = s"$entityResolverserviceUrl/entity-resolver-admin/manual-opt-out/sa/${sautr.value}"

      val error = new Upstream4xxResponse("", Status.CONFLICT, Status.CONFLICT)
      val result = entityConnectorPostMock(expectedPath, error).optOut(sautr).futureValue

      result mustBe AlreadyOptedOut
    }

    "return false if NOT_FOUND" in new TestCase {
      val expectedPath = s"$entityResolverserviceUrl/entity-resolver-admin/manual-opt-out/sa/${sautr.value}"

      val error = new Upstream4xxResponse("", Status.NOT_FOUND, Status.NOT_FOUND)
      val result = entityConnectorPostMock(expectedPath, error).optOut(sautr).futureValue

      result mustBe PreferenceNotFound
    }

    "return false if PRECONDITION_FAILED" in new TestCase {
      val expectedPath = s"$entityResolverserviceUrl/entity-resolver-admin/manual-opt-out/sa/${sautr.value}"
      val error = new Upstream4xxResponse("", Status.PRECONDITION_FAILED, Status.PRECONDITION_FAILED)
      val result = entityConnectorPostMock(expectedPath, error).optOut(sautr).futureValue

      result mustBe PreferenceNotFound
    }

  }

  class TestCase {
    val sautr = TaxIdentifier("sautr", Random.nextInt(1000000).toString)
    val nino = TaxIdentifier("nino", "NA000914D")
    val itsaId = TaxIdentifier("HMRC-MTD-IT", "XYIT00000067034")

    implicit val hc: HeaderCarrier = HeaderCarrier()

    lazy val mockResponse = mock[Option[Entity]]
    val emptyJson = Json.obj()
    implicit val ef = Entity.formats

    lazy val servicesConfig = app.injector.instanceOf[ServicesConfig]
    def entityConnectorGetEntityMock(expectedPath: String, jsonBody: JsValue): EntityResolverConnector = {
      val mockHttp: HttpClient = mock[HttpClient]
      when(
        mockHttp.GET[Option[Entity]](ArgumentMatchers.eq(expectedPath), ArgumentMatchers.any(), ArgumentMatchers.any())(
          ArgumentMatchers.any(),
          ArgumentMatchers.any(),
          ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(jsonBody.as[Entity])))
      new EntityResolverConnector(mockHttp, servicesConfig)
    }

    def entityConnectorGetPreferenceDetailsMock(expectedPath: String, jsonBody: JsValue): EntityResolverConnector = {
      val mockHttp: HttpClient = mock[HttpClient]
      when(
        mockHttp.GET[Option[PreferenceDetails]](ArgumentMatchers.eq(expectedPath), ArgumentMatchers.any(), ArgumentMatchers.any())(
          ArgumentMatchers.any(),
          ArgumentMatchers.any(),
          ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(jsonBody.as[PreferenceDetails])))
      new EntityResolverConnector(mockHttp, servicesConfig)
    }

    def entityConnectorGetMock(expectedPath: String, error: Throwable): EntityResolverConnector = {
      val mockHttp: HttpClient = mock[HttpClient]
      when(
        mockHttp.GET(ArgumentMatchers.eq(expectedPath), ArgumentMatchers.any(), ArgumentMatchers.any())(
          ArgumentMatchers.any(),
          ArgumentMatchers.any(),
          ArgumentMatchers.any()))
        .thenReturn(Future.failed(error))
      new EntityResolverConnector(mockHttp, servicesConfig)
    }

    def entityConnectorPostMock(expectedPath: String, jsonBody: JsValue): EntityResolverConnector = {
      lazy val mockResponse = mock[HttpResponse]
      val mockHttp: HttpClient = mock[HttpClient]
      when(
        mockHttp.POSTEmpty[HttpResponse](ArgumentMatchers.eq(expectedPath), ArgumentMatchers.any())(
          ArgumentMatchers.any(),
          ArgumentMatchers.any(),
          ArgumentMatchers.any()))
        .thenReturn(Future.successful(mockResponse))
      new EntityResolverConnector(mockHttp, servicesConfig)
    }

    def entityConnectorPostMock(expectedPath: String, error: Throwable): EntityResolverConnector = {
      val mockHttp: HttpClient = mock[HttpClient]
      when(
        mockHttp.POSTEmpty(ArgumentMatchers.eq(expectedPath), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.failed(error))
      new EntityResolverConnector(mockHttp, servicesConfig)
    }

    def taxIdentifiersResponseFor(taxIds: TaxIdentifier*) = {
      val taxIdsJson: Seq[(String, JsValue)] = taxIds.map {
        case TaxIdentifier(name, value) => name -> JsString(value)
      }
      taxIdsJson.foldLeft(Json.obj("_id" -> "6a048719-3d4b-4a3e-9440-17b238807bc9"))(_ + _)
    }

    def preferenceDetailsResponseForGenericOptedIn(emailVerified: Boolean) = {
      val genericUpdatedAt = 1518652800000L
      val genericUpdatedAtStr = s""" "updatedAt": $genericUpdatedAt """
      val verifiedOnDate = 1518652800000L
      val verifiedOnDateStr = if (emailVerified) s""" "verifiedOn": $verifiedOnDate, """ else ""
      Json.parse(s"""
                    |{
                    |  "digital": true,
                    |  "termsAndConditions": {
                    |    "generic": {
                    |      "accepted": true,
                    |      $genericUpdatedAtStr
                    |    }
                    |  },
                    |  "email": {
                    |    "email": "john.doe@digital.hmrc.gov.uk",
                    |    "status": "${if (emailVerified) "verified" else ""}",
                    |    $verifiedOnDateStr
                    |    "mailboxFull": false,
                    |    "hasBounces":false
                    |  }
                    |}
       """.stripMargin)
    }

    def preferenceDetailsResponseForOptedOut() = {
      val genericUpdatedAt = 1518652800000L
      val genericUpdatedAtStr = s""" "updatedAt": $genericUpdatedAt """
      Json.parse(s"""
                    |{
                    |  "digital": false,
                    |   "termsAndConditions": {
                    |    "generic": {
                    |      "accepted": false,
                    |      $genericUpdatedAtStr
                    |    }
                    |  }
                    |}
       """.stripMargin)
    }

  }

}
