/*
 * Copyright 2020 HM Revenue & Customs
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

import org.joda.time.{ DateTime, DateTimeZone }
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.{ IntegrationPatience, ScalaFutures }
import org.scalatest.mockito.MockitoSugar
import play.api.Configuration
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.preferencesadminfrontend.connectors._
import uk.gov.hmrc.preferencesadminfrontend.services.model.{ Email, EntityId, Preference, TaxIdentifier }
import uk.gov.hmrc.preferencesadminfrontend.utils.SpecBase

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SearchServiceSpec extends UnitSpec with MockitoSugar with ScalaFutures with IntegrationPatience {

  implicit val hc = HeaderCarrier()

  "getPreferences" should {

    "return preference for nino user when it exists" in new SearchServiceTestCase {
      when(entityResolverConnectorMock.getPreferenceDetails(validNino)).thenReturn(Future.successful(optedInPreferenceDetails))
      when(entityResolverConnectorMock.getTaxIdentifiers(validNino)).thenReturn(Future.successful(taxIdentifiers))

      searchService.searchPreference(validNino).futureValue shouldBe List(optedInPreference)

      val expectedAuditEvent = searchService.createSearchEvent("me", validNino, Some(optedInPreference))
      verify(auditConnectorMock).sendMergedEvent(argThat(isSimilar(expectedAuditEvent)))(any(), any())
    }

    "return preference for utr user when it exists" in new SearchServiceTestCase {
      when(entityResolverConnectorMock.getPreferenceDetails(validSaUtr)).thenReturn(Future.successful(optedInPreferenceDetails))
      when(entityResolverConnectorMock.getTaxIdentifiers(validSaUtr)).thenReturn(Future.successful(taxIdentifiers))

      searchService.searchPreference(validSaUtr).futureValue shouldBe List(optedInPreference)

      val expectedAuditEvent = searchService.createSearchEvent("me", validSaUtr, Some(optedInPreference))
      verify(auditConnectorMock).sendMergedEvent(argThat(isSimilar(expectedAuditEvent)))(any(), any())
    }

    "return preference for utr user who has opted out" in new SearchServiceTestCase {
      when(entityResolverConnectorMock.getPreferenceDetails(validSaUtr)).thenReturn(Future.successful(optedOutPreferenceDetails))
      when(entityResolverConnectorMock.getTaxIdentifiers(validSaUtr)).thenReturn(Future.successful(taxIdentifiers))

      searchService.searchPreference(validSaUtr).futureValue shouldBe List(optedOutPreference)

      val expectedAuditEvent = searchService.createSearchEvent("me", validSaUtr, Some(optedOutPreference))
      verify(auditConnectorMock).sendMergedEvent(argThat(isSimilar(expectedAuditEvent)))(any(), any())
    }

    "return None if the saUtr identifier does not exist" in new SearchServiceTestCase {
      val preferenceDetails = None
      when(entityResolverConnectorMock.getPreferenceDetails(validSaUtr)).thenReturn(Future.successful(preferenceDetails))
      override val taxIdentifiers = Seq()
      when(entityResolverConnectorMock.getTaxIdentifiers(validSaUtr)).thenReturn(Future.successful(taxIdentifiers))

      searchService.searchPreference(validSaUtr).futureValue shouldBe Nil

      val expectedAuditEvent = searchService.createSearchEvent("me", validSaUtr, None)
      verify(auditConnectorMock).sendMergedEvent(argThat(isSimilar(expectedAuditEvent)))(any(), any())
    }

    "return preference for email address user when it exists" in new SearchServiceTestCase {
      when(preferencesConnectorMock.getPreferenceDetails(validEmailid)).thenReturn(Future.successful(optedInPreferenceDetailsList))
      when(entityResolverConnectorMock.getTaxIdentifiers(optedInPreferenceDetailsList.head)).thenReturn(Future.successful(taxIdentifiers))

      searchService.searchPreference(validEmailid).futureValue shouldBe List(optedInPreference)
    }

    "return None if that nino does not exist" in new SearchServiceTestCase {
      when(preferencesConnectorMock.getPreferenceDetails(unknownEmailid)).thenReturn(Future.successful(Nil))
      when(entityResolverConnectorMock.getTaxIdentifiers(optedInPreferenceDetailsList.head)).thenReturn(Future.successful(taxIdentifiers))

      searchService.searchPreference(unknownEmailid).futureValue shouldBe Nil
    }

    "return multiple preferences for email address user when it exists" in new SearchServiceTestCase {
      when(preferencesConnectorMock.getPreferenceDetails(validEmailid)).thenReturn(Future.successful(optedInPreferenceDetailsList2))
      when(entityResolverConnectorMock.getTaxIdentifiers(optedInPreferenceDetailsList.head)).thenReturn(Future.successful(taxIdentifiers))

      searchService.searchPreference(validEmailid).futureValue shouldBe optedInPreferenceList
    }

  }

  "optOut" should {

    "call entity resolver to opt the user out" in new SearchServiceTestCase {
      when(entityResolverConnectorMock.getPreferenceDetails(validSaUtr))
        .thenReturn(Future.successful(optedInPreferenceDetails), Future.successful(optedOutPreferenceDetails))
      when(entityResolverConnectorMock.getTaxIdentifiers(validSaUtr)).thenReturn(Future.successful(taxIdentifiers))
      when(entityResolverConnectorMock.optOut(validSaUtr)).thenReturn(Future.successful(OptedOut))

      searchService.optOut(validSaUtr, "my optOut reason").futureValue shouldBe OptedOut
      verify(entityResolverConnectorMock, times(1)).optOut(validSaUtr)
    }

    "create an audit event when the user is opted out" in new SearchServiceTestCase {
      when(entityResolverConnectorMock.getPreferenceDetails(validSaUtr))
        .thenReturn(Future.successful(optedInPreferenceDetails), Future.successful(optedOutPreferenceDetails))
      when(entityResolverConnectorMock.getTaxIdentifiers(validSaUtr)).thenReturn(Future.successful(taxIdentifiers))
      when(entityResolverConnectorMock.optOut(validSaUtr)).thenReturn(Future.successful(OptedOut))
      when(auditConnectorMock.sendEvent(any())(any(), any())).thenReturn(Future.successful(AuditResult.Success))

      searchService.optOut(validSaUtr, "my optOut reason").futureValue shouldBe OptedOut

      val expectedAuditEvent =
        searchService.createOptOutEvent("me", validSaUtr, Some(optedInPreference), Some(optedOutPreference), OptedOut, "my optOut reason")
      verify(auditConnectorMock).sendMergedEvent(argThat(isSimilar(expectedAuditEvent)))(any(), any())
    }

    "create an audit event when the user is not opted out as it is not found" in new SearchServiceTestCase {
      when(entityResolverConnectorMock.getPreferenceDetails(validSaUtr)).thenReturn(Future.successful(None), Future.successful(None))
      when(entityResolverConnectorMock.getTaxIdentifiers(validSaUtr)).thenReturn(Future.successful(Seq.empty))
      when(entityResolverConnectorMock.optOut(validSaUtr)).thenReturn(Future.successful(PreferenceNotFound))
      when(auditConnectorMock.sendEvent(any())(any(), any())).thenReturn(Future.successful(AuditResult.Success))

      searchService.optOut(validSaUtr, "my optOut reason").futureValue shouldBe PreferenceNotFound

      val expectedAuditEvent = searchService.createOptOutEvent("me", validSaUtr, None, None, PreferenceNotFound, "my optOut reason")
      verify(auditConnectorMock).sendMergedEvent(argThat(isSimilar(expectedAuditEvent)))(any(), any())
    }

    "create an audit event when the user is already opted out" in new SearchServiceTestCase {
      when(entityResolverConnectorMock.getPreferenceDetails(validSaUtr))
        .thenReturn(Future.successful(optedOutPreferenceDetails), Future.successful(optedOutPreferenceDetails))
      when(entityResolverConnectorMock.getTaxIdentifiers(validSaUtr)).thenReturn(Future.successful(taxIdentifiers))
      when(entityResolverConnectorMock.optOut(validSaUtr)).thenReturn(Future.successful(AlreadyOptedOut))
      when(auditConnectorMock.sendEvent(any())(any(), any())).thenReturn(Future.successful(AuditResult.Success))

      searchService.optOut(validSaUtr, "my optOut reason").futureValue shouldBe AlreadyOptedOut

      val expectedAuditEvent =
        searchService.createOptOutEvent("me", validSaUtr, Some(optedOutPreference), Some(optedOutPreference), AlreadyOptedOut, "my optOut reason")
      verify(auditConnectorMock).sendMergedEvent(argThat(isSimilar(expectedAuditEvent)))(any(), any())
    }
  }

  "createSearchEvent" should {
    "generate the correct event when the preference exists" in new SearchServiceTestCase {
      val preference = Preference(
        entityId = Some(EntityId("383cfb1b-5f57-417b-9380-545f35c29a22")),
        genericPaperless = true,
        genericUpdatedAt = genericUpdatedAt,
        taxCreditsPaperless = true,
        taxCreditsUpdatedAt = taxCreditsUpdatedAt,
        email = Some(Email(address = "john.doe@digital.hmrc.gov.uk", verified = true, verifiedOn = verifiedOn, language = None)),
        taxIdentifiers = Seq(TaxIdentifier("sautr", "123"), TaxIdentifier("nino", "ABC"))
      )
      val event = searchService.createSearchEvent("me", TaxIdentifier("sautr", "123"), Some(preference))

      event.auditSource shouldBe "preferences-admin-frontend"
      event.auditType shouldBe "TxSucceeded"
      event.request.detail shouldBe Map(
        "preference"   -> "{\"entityId\":\"383cfb1b-5f57-417b-9380-545f35c29a22\",\"genericPaperless\":true,\"genericUpdatedAt\":1518652800000,\"taxCreditsPaperless\":true,\"taxCreditsUpdatedAt\":1518652800000,\"email\":{\"address\":\"john.doe@digital.hmrc.gov.uk\",\"verified\":true,\"verifiedOn\":1518652800000},\"taxIdentifiers\":[{\"name\":\"sautr\",\"value\":\"123\"},{\"name\":\"nino\",\"value\":\"ABC\"}]}",
        "result"       -> "Found",
        "query"        -> "{\"name\":\"sautr\",\"value\":\"123\"}",
        "DataCallType" -> "request",
        "user"         -> "me"
      )
      event.request.tags("transactionName") shouldBe "Paperless opt out search"
    }

    "generate the correct event when the preference does not exist" in new SearchServiceTestCase {
      val event = searchService.createSearchEvent("me", TaxIdentifier("sautr", "123"), None)

      event.auditSource shouldBe "preferences-admin-frontend"
      event.auditType shouldBe "TxSucceeded"
      event.request.detail shouldBe Map(
        "preference"   -> "Not found",
        "result"       -> "Not found",
        "query"        -> "{\"name\":\"sautr\",\"value\":\"123\"}",
        "DataCallType" -> "request",
        "user"         -> "me")
      event.request.tags("transactionName") shouldBe "Paperless opt out search"

    }
  }

  "createOptoutEvent" should {

    "generate the correct event user is opted out" in new SearchServiceTestCase {
      val event = searchService.createOptOutEvent("me", validSaUtr, Some(optedInPreference), Some(optedOutPreference), OptedOut, "my optOut reason")

      event.auditSource shouldBe "preferences-admin-frontend"
      event.auditType shouldBe "TxSucceeded"
      event.request.detail shouldBe Map(
        "optOutReason"       -> "my optOut reason",
        "query"              -> "{\"name\":\"sautr\",\"value\":\"123456789\"}",
        "originalPreference" -> "{\"genericPaperless\":true,\"genericUpdatedAt\":1518652800000,\"taxCreditsPaperless\":false,\"taxCreditsUpdatedAt\":1518652800000,\"email\":{\"address\":\"john.doe@digital.hmrc.gov.uk\",\"verified\":true,\"verifiedOn\":1518652800000,\"language\":\"cy\"},\"taxIdentifiers\":[{\"name\":\"sautr\",\"value\":\"123456789\"},{\"name\":\"nino\",\"value\":\"CE067583D\"}]}",
        "DataCallType"       -> "request",
        "newPreference"      -> "{\"genericPaperless\":false,\"genericUpdatedAt\":1518652800000,\"taxCreditsPaperless\":false,\"taxCreditsUpdatedAt\":1518652800000,\"taxIdentifiers\":[{\"name\":\"sautr\",\"value\":\"123456789\"},{\"name\":\"nino\",\"value\":\"CE067583D\"}]}",
        "reasonOfFailure"    -> "Done",
        "user"               -> "me"
      )
      event.request.tags("transactionName") shouldBe "Manual opt out from paperless"
    }

    "generate the correct event when the preference already opted out" in new SearchServiceTestCase {
      val event = searchService.createOptOutEvent("me", validSaUtr, Some(optedOutPreference), Some(optedOutPreference), AlreadyOptedOut, "my optOut reason")

      event.auditSource shouldBe "preferences-admin-frontend"
      event.auditType shouldBe "TxFailed"
      event.request.detail shouldBe Map(
        "optOutReason"       -> "my optOut reason",
        "query"              -> "{\"name\":\"sautr\",\"value\":\"123456789\"}",
        "originalPreference" -> "{\"genericPaperless\":false,\"genericUpdatedAt\":1518652800000,\"taxCreditsPaperless\":false,\"taxCreditsUpdatedAt\":1518652800000,\"taxIdentifiers\":[{\"name\":\"sautr\",\"value\":\"123456789\"},{\"name\":\"nino\",\"value\":\"CE067583D\"}]}",
        "DataCallType"       -> "request",
        "newPreference"      -> "{\"genericPaperless\":false,\"genericUpdatedAt\":1518652800000,\"taxCreditsPaperless\":false,\"taxCreditsUpdatedAt\":1518652800000,\"taxIdentifiers\":[{\"name\":\"sautr\",\"value\":\"123456789\"},{\"name\":\"nino\",\"value\":\"CE067583D\"}]}",
        "reasonOfFailure"    -> "Preference already opted out",
        "user"               -> "me"
      )
      event.request.tags("transactionName") shouldBe "Manual opt out from paperless"

    }

    "generate the correct event when the preference does not exist" in new SearchServiceTestCase {
      val event = searchService.createOptOutEvent("me", validSaUtr, None, None, PreferenceNotFound, "my optOut reason")

      event.auditSource shouldBe "preferences-admin-frontend"
      event.auditType shouldBe "TxFailed"
      event.request.detail shouldBe Map(
        "optOutReason"       -> "my optOut reason",
        "query"              -> "{\"name\":\"sautr\",\"value\":\"123456789\"}",
        "originalPreference" -> "Not found",
        "DataCallType"       -> "request",
        "newPreference"      -> "Not found",
        "reasonOfFailure"    -> "Preference not found",
        "user"               -> "me"
      )
      event.request.tags("transactionName") shouldBe "Manual opt out from paperless"

    }

    "generate the correct event when entity is not found" in new SearchServiceTestCase {
      val event = searchService.createOptOutEvent("me", validSaUtr, None, None, PreferenceNotFound, "my optOut reason")

      event.auditSource shouldBe "preferences-admin-frontend"
      event.auditType shouldBe "TxFailed"
      event.request.tags("transactionName") shouldBe "Manual opt out from paperless"
      event.request.detail shouldBe Map(
        "optOutReason"       -> "my optOut reason",
        "query"              -> "{\"name\":\"sautr\",\"value\":\"123456789\"}",
        "originalPreference" -> "Not found",
        "DataCallType"       -> "request",
        "newPreference"      -> "Not found",
        "reasonOfFailure"    -> "Preference not found",
        "user"               -> "me"
      )

      event.response.tags("transactionName") shouldBe "Manual opt out from paperless"
      event.response.detail shouldBe Map(
        "optOutReason"       -> "my optOut reason",
        "query"              -> "{\"name\":\"sautr\",\"value\":\"123456789\"}",
        "originalPreference" -> "Not found",
        "DataCallType"       -> "response",
        "newPreference"      -> "Not found",
        "reasonOfFailure"    -> "Preference not found",
        "user"               -> "me"
      )
    }

  }

  trait SearchServiceTestCase extends SpecBase {

    val validSaUtr = TaxIdentifier("sautr", "123456789")
    val validNino = TaxIdentifier("nino", "CE067583D")
    val invalidNino = TaxIdentifier("nino", "123123456S")
    val validEmailid = TaxIdentifier("email", "test@test.com")
    val unknownEmailid = TaxIdentifier("email", "test9@test.com")

    val genericUpdatedAt = Some(new DateTime(2018, 2, 15, 0, 0, DateTimeZone.UTC))
    val taxCreditsUpdatedAt = Some(new DateTime(2018, 2, 15, 0, 0, DateTimeZone.UTC))
    val verifiedOn = Some(new DateTime(2018, 2, 15, 0, 0, DateTimeZone.UTC))

    val verifiedEmail = Email("john.doe@digital.hmrc.gov.uk", verified = true, verifiedOn = verifiedOn, language = Some("cy"))

    def preferenceDetails(genericPaperless: Boolean, taxCreditsPaperless: Boolean) = {
      val email = if (genericPaperless | taxCreditsPaperless) Some(verifiedEmail) else None
      Some(PreferenceDetails(genericPaperless, genericUpdatedAt, taxCreditsPaperless, taxCreditsUpdatedAt, email))
    }

    def preferenceDetails(genericPaperless: Boolean, taxCreditsPaperless: Boolean, entityId: EntityId) = {
      val email = if (genericPaperless | taxCreditsPaperless) Some(verifiedEmail) else None
      List(PreferenceDetails(genericPaperless, genericUpdatedAt, taxCreditsPaperless, taxCreditsUpdatedAt, email))
    }

    def multiplepreferenceDetails(genericPaperless: Boolean, taxCreditsPaperless: Boolean, entityId: EntityId) = {
      val email = if (genericPaperless | taxCreditsPaperless) Some(verifiedEmail) else None
      List(
        PreferenceDetails(genericPaperless, genericUpdatedAt, taxCreditsPaperless, taxCreditsUpdatedAt, email),
        PreferenceDetails(genericPaperless, genericUpdatedAt, taxCreditsPaperless, taxCreditsUpdatedAt, email)
      )
    }

    val optedInPreferenceDetails = preferenceDetails(genericPaperless = true, taxCreditsPaperless = false)
    val optedOutPreferenceDetails = preferenceDetails(genericPaperless = false, taxCreditsPaperless = false)
    val optedInPreferenceDetailsList = preferenceDetails(genericPaperless = true, taxCreditsPaperless = false, entityId = EntityId(value = "x123"))
    val optedInPreferenceDetailsList2 = multiplepreferenceDetails(genericPaperless = true, taxCreditsPaperless = false, entityId = EntityId(value = "x123"))

    val taxIdentifiers = Seq(validSaUtr, validNino)

    val optedInPreference = Preference(
      entityId = None,
      genericPaperless = true,
      genericUpdatedAt = genericUpdatedAt,
      taxCreditsPaperless = false,
      taxCreditsUpdatedAt = taxCreditsUpdatedAt,
      email = Some(verifiedEmail),
      taxIdentifiers = taxIdentifiers
    )
    val optedInPreferenceList = List(
      Preference(
        entityId = None,
        genericPaperless = true,
        genericUpdatedAt = genericUpdatedAt,
        taxCreditsPaperless = false,
        taxCreditsUpdatedAt = taxCreditsUpdatedAt,
        email = Some(verifiedEmail),
        taxIdentifiers = taxIdentifiers
      ),
      Preference(
        entityId = None,
        genericPaperless = true,
        genericUpdatedAt = genericUpdatedAt,
        taxCreditsPaperless = false,
        taxCreditsUpdatedAt = taxCreditsUpdatedAt,
        email = Some(verifiedEmail),
        taxIdentifiers = taxIdentifiers
      )
    )

    val optedOutPreference = Preference(
      entityId = None,
      genericPaperless = false,
      genericUpdatedAt = genericUpdatedAt,
      taxCreditsPaperless = false,
      taxCreditsUpdatedAt = taxCreditsUpdatedAt,
      email = None,
      taxIdentifiers = taxIdentifiers
    )

    val config = Configuration.from(Map("appName" -> "preferences-admin-frontend"))
    val searchService = new SearchService(entityResolverConnectorMock, preferencesConnectorMock, auditConnectorMock, config)
  }

}
