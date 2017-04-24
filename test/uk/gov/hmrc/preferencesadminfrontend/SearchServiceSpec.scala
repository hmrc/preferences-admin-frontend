/*
 * Copyright 2017 HM Revenue & Customs
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

package uk.gov.hmrc.preferencesadminfrontend

import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.preferencesadminfrontend.connectors.{EntityResolverConnector, PreferenceDetails}
import uk.gov.hmrc.preferencesadminfrontend.services._
import uk.gov.hmrc.preferencesadminfrontend.services.model.{Email, TaxIdentifier}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SearchServiceSpec extends UnitSpec with MockitoSugar with ScalaFutures {

  implicit val hc = HeaderCarrier()

  "getPreferences" should {

    val validSaUtr = TaxIdentifier("sautr", "123456789")
    val validNino = TaxIdentifier("nino", "SS123456S")

    "return preference for nino user when it exists" in new TestCase {

      val preferenceDetails = Some(PreferenceDetails(paperless = true, Some(Email("john.doe@digital.hmrc.gov.uk", verified = true))))
      when(entityResolverConnector.getPreferenceDetails(validNino)).thenReturn(Future.successful(preferenceDetails))
      val taxIdentifiers = Seq(validNino, validSaUtr)
      when(entityResolverConnector.getTaxIdentifiers(validNino)).thenReturn(Future.successful(taxIdentifiers))

      val result = searchService.getPreference(validNino).futureValue

      result match {
        case Some(preference) => {
          preference.paperless shouldBe true
          preference.email shouldBe Some(Email("john.doe@digital.hmrc.gov.uk", true))
          preference.taxIdentifiers shouldBe Seq(validNino, validSaUtr)
        }
        case _ => fail()
      }
    }

    "return preference for utr user when it exists" in new TestCase {
      val preferenceDetails = Some(PreferenceDetails(paperless = true, Some(Email("john.doe@digital.hmrc.gov.uk", verified = true))))
      when(entityResolverConnector.getPreferenceDetails(validSaUtr)).thenReturn(Future.successful(preferenceDetails))
      val taxIdentifiers = Seq(validNino, validSaUtr)
      when(entityResolverConnector.getTaxIdentifiers(validSaUtr)).thenReturn(Future.successful(taxIdentifiers))

      val result = searchService.getPreference(validSaUtr).futureValue

      result match {
        case Some(preference) => {
          preference.paperless shouldBe true
          preference.email shouldBe Some(Email("john.doe@digital.hmrc.gov.uk", true))
          preference.taxIdentifiers shouldBe Seq(validNino, validSaUtr)
        }
        case _ => fail()
      }
    }

    "return preference for utr user who has opted out" in new TestCase {
      val optedOutPreferenceDetails = Some(PreferenceDetails(paperless = false, None))
      when(entityResolverConnector.getPreferenceDetails(validSaUtr)).thenReturn(Future.successful(optedOutPreferenceDetails))
      val taxIdentifiers = Seq(validNino, validSaUtr)
      when(entityResolverConnector.getTaxIdentifiers(validSaUtr)).thenReturn(Future.successful(taxIdentifiers))

      val result = searchService.getPreference(validSaUtr).futureValue

      result match {
        case Some(preference) => {
          preference.paperless shouldBe false
          preference.email shouldBe None
          preference.taxIdentifiers shouldBe Seq(validNino, validSaUtr)
        }
        case _ => fail()
      }
    }

    "return None if the saUtr identifier does not exist" in new TestCase {
      val preferenceDetails = None
      when(entityResolverConnector.getPreferenceDetails(validSaUtr)).thenReturn(Future.successful(preferenceDetails))
      val taxIdentifiers = Seq()
      when(entityResolverConnector.getTaxIdentifiers(validSaUtr)).thenReturn(Future.successful(taxIdentifiers))

      val result = searchService.getPreference(validSaUtr).futureValue

      result shouldBe None
    }

  }

  "optOut" should {

    "call entity resolver to opt the user out" in new TestCase {
      when(entityResolverConnector.optOut(validSaUtr)).thenReturn(Future.successful(true))

      val result = searchService.optOut(validSaUtr).futureValue
      verify(entityResolverConnector, times(1)).optOut(validSaUtr)
    }

    "create an audit event when the user is opted out" in new TestCase {
      when(entityResolverConnector.optOut(validSaUtr)).thenReturn(Future.successful(true))
      when(auditConnector.sendEvent(any())(any(),any())).thenReturn(Future.successful(AuditResult.Success))

      val result = searchService.optOut(validSaUtr).futureValue
      verify(auditConnector, times(1)).sendEvent(any())(any(),any())
    }

    "create an audit event when the user is not opted out as not found" in new TestCase {
      when(entityResolverConnector.optOut(validSaUtr)).thenReturn(Future.successful(false))
      when(auditConnector.sendEvent(any())(any(),any())).thenReturn(Future.successful(AuditResult.Success))

      val result = searchService.optOut(validSaUtr).futureValue
      verify(auditConnector, times(1)).sendEvent(any())(any(),any())
    }
  }

  trait TestCase {
    val validSaUtr = TaxIdentifier("sautr", "123456789")
    val validNino = TaxIdentifier("nino", "CE067583D")
    val invalidNino = TaxIdentifier("nino", "123123456S")

    val auditConnector = mock[AuditConnector]
    val entityResolverConnector = mock[EntityResolverConnector]
    val searchService = new SearchService(entityResolverConnector, auditConnector)
  }
}