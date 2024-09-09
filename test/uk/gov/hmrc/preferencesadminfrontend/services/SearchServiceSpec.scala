/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.preferencesadminfrontend.services

import org.mockito.ArgumentMatchers.*
import org.mockito.Mockito.*
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.{ IntegrationPatience, ScalaFutures }
import org.scalatestplus.play.PlaySpec
import play.api.Configuration
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import uk.gov.hmrc.play.audit.model.DataEvent
import uk.gov.hmrc.preferencesadminfrontend.connectors.*
import uk.gov.hmrc.preferencesadminfrontend.controllers.model.Event
import uk.gov.hmrc.preferencesadminfrontend.services.model.{ Email, EntityId, Preference, TaxIdentifier }
import uk.gov.hmrc.preferencesadminfrontend.utils.SpecBase

import java.time.{ ZoneOffset, ZonedDateTime }
import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ ExecutionContext, Future }

class SearchServiceSpec
    extends PlaySpec with SearchServiceTestCase with ScalaFutures with IntegrationPatience with BeforeAndAfterEach {

  implicit val hc: HeaderCarrier = HeaderCarrier()
  val entityId = UUID.randomUUID().toString
  override def afterEach(): Unit = reset(auditConnectorMock)

  "getPreferences" should {
    "return preference for nino user when it exists" in {

      when(entityResolverConnectorMock.getPreferenceDetails(validNino))
        .thenReturn(Future.successful(optedInPreferenceDetails(entityId)))
      when(preferencesConnectorMock.getPreferencesEvents(any[String])(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(List.empty[Event]))
      when(entityResolverConnectorMock.getTaxIdentifiers(validNino)).thenReturn(Future.successful(taxIdentifiers))
      val expectedAuditEvent = searchService.createSearchEvent("me", validNino, Some(optedInPreference(entityId)))
      when(
        auditConnectorMock
          .sendMergedEvent(argThat(isSimilar(expectedAuditEvent)))(any[HeaderCarrier], any[ExecutionContext])
      ).thenReturn(Future.successful(AuditResult.Success))

      searchService.searchPreference(validNino).futureValue mustBe List(optedInPreference(entityId))
    }

    "return preference for itsa user when it exists" in {
      when(entityResolverConnectorMock.getPreferenceDetails(validItsa))
        .thenReturn(Future.successful(optedInPreferenceDetails(entityId)))
      when(preferencesConnectorMock.getPreferencesEvents(any[String])(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(List.empty[Event]))
      when(entityResolverConnectorMock.getTaxIdentifiers(validItsa)).thenReturn(Future.successful(taxIdentifiers))
      val expectedAuditEvent = searchService.createSearchEvent("me", validItsa, Some(optedInPreference(entityId)))
      when(
        auditConnectorMock
          .sendMergedEvent(argThat(isSimilar(expectedAuditEvent)))(any[HeaderCarrier], any[ExecutionContext])
      ).thenReturn(Future.successful(AuditResult.Success))

      searchService.searchPreference(validItsa).futureValue mustBe List(optedInPreference(entityId))
    }

    "return preference for utr user when it exists" in {
      when(entityResolverConnectorMock.getPreferenceDetails(validSaUtr))
        .thenReturn(Future.successful(optedInPreferenceDetails(entityId)))
      when(preferencesConnectorMock.getPreferencesEvents(any[String])(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(List.empty[Event]))
      when(entityResolverConnectorMock.getTaxIdentifiers(validSaUtr)).thenReturn(Future.successful(taxIdentifiers))
      val expectedAuditEvent = searchService.createSearchEvent("me", validSaUtr, Some(optedInPreference(entityId)))
      when(
        auditConnectorMock
          .sendMergedEvent(argThat(isSimilar(expectedAuditEvent)))(any[HeaderCarrier], any[ExecutionContext])
      ).thenReturn(Future.successful(AuditResult.Success))

      searchService.searchPreference(validSaUtr).futureValue mustBe List(optedInPreference(entityId))
    }

    "return preference for utr user who has opted out" in {
      when(entityResolverConnectorMock.getPreferenceDetails(validSaUtr))
        .thenReturn(Future.successful(optedOutPreferenceDetails(entityId)))
      when(preferencesConnectorMock.getPreferencesEvents(any[String])(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(List.empty[Event]))
      when(entityResolverConnectorMock.getTaxIdentifiers(validSaUtr)).thenReturn(Future.successful(taxIdentifiers))
      val expectedAuditEvent = searchService.createSearchEvent("me", validSaUtr, Some(optedOutPreference(entityId)))
      when(
        auditConnectorMock
          .sendMergedEvent(argThat(isSimilar(expectedAuditEvent)))(any[HeaderCarrier], any[ExecutionContext])
      ).thenReturn(Future.successful(AuditResult.Success))

      searchService.searchPreference(validSaUtr).futureValue mustBe List(optedOutPreference(entityId))
    }

    "return preference for email address user when it exists" in {
      when(preferencesConnectorMock.getPreferenceDetails(validEmailid.value))
        .thenReturn(Future.successful(optedInPreferenceDetailsList(entityId)))
      when(preferencesConnectorMock.getPreferencesEvents(any[String])(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(List.empty[Event]))
      when(entityResolverConnectorMock.getTaxIdentifiers(optedInPreferenceDetailsList(entityId).head))
        .thenReturn(Future.successful(taxIdentifiers))

      searchService.searchPreference(validEmailid).futureValue mustBe List(optedInPreference(entityId))
    }

    "return None if the saUtr identifier does not exist" in {
      val preferenceDetails = None
      when(entityResolverConnectorMock.getPreferenceDetails(validSaUtr))
        .thenReturn(Future.successful(preferenceDetails))
      when(preferencesConnectorMock.getPreferencesEvents(any[String])(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(List.empty[Event]))
      when(entityResolverConnectorMock.getTaxIdentifiers(validSaUtr)).thenReturn(Future.successful(Seq()))

      searchService.searchPreference(validSaUtr).futureValue mustBe Nil
    }

    "return None if that nino does not exist" in {
      when(preferencesConnectorMock.getPreferenceDetails(unknownEmailid.value)).thenReturn(Future.successful(Nil))
      when(entityResolverConnectorMock.getTaxIdentifiers(optedInPreferenceDetailsList(entityId).head))
        .thenReturn(Future.successful(taxIdentifiers))

      searchService.searchPreference(unknownEmailid).futureValue mustBe Nil
    }

    "return multiple preferences for email address user when it exists" in {
      when(preferencesConnectorMock.getPreferenceDetails(validEmailid.value))
        .thenReturn(Future.successful(optedInPreferenceDetailsList2(entityId)))
      when(entityResolverConnectorMock.getTaxIdentifiers(optedInPreferenceDetailsList(entityId).head))
        .thenReturn(Future.successful(taxIdentifiers))

      searchService.searchPreference(validEmailid).futureValue mustBe optedInPreferenceList(entityId)
    }

  }

  "optOut" should {

    "call entity resolver to opt the user out" in {
      when(entityResolverConnectorMock.getPreferenceDetails(validSaUtr))
        .thenReturn(
          Future.successful(optedInPreferenceDetails(entityId)),
          Future.successful(optedOutPreferenceDetails(entityId))
        )
      when(preferencesConnectorMock.getPreferencesEvents(any[String])(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(List.empty[Event]))
      when(preferencesConnectorMock.getPreferencesEvents(any[String])(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(List.empty[Event]))
      when(entityResolverConnectorMock.getTaxIdentifiers(validSaUtr)).thenReturn(Future.successful(taxIdentifiers))
      when(entityResolverConnectorMock.optOut(validSaUtr)).thenReturn(Future.successful(OptedOut))

      searchService.optOut(validSaUtr, "my optOut reason").futureValue mustBe OptedOut
      verify(entityResolverConnectorMock, times(1)).optOut(validSaUtr)
    }

    "create an audit event when the user is opted out" in {
      when(entityResolverConnectorMock.getPreferenceDetails(validSaUtr))
        .thenReturn(Future.successful(optedInPreferenceDetails), Future.successful(optedOutPreferenceDetails(entityId)))
      when(preferencesConnectorMock.getPreferencesEvents(any[String])(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(List.empty[Event]))
      when(preferencesConnectorMock.getPreferencesEvents(any[String])(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(List.empty[Event]))
      when(entityResolverConnectorMock.getTaxIdentifiers(validSaUtr)).thenReturn(Future.successful(taxIdentifiers))
      when(entityResolverConnectorMock.optOut(validSaUtr)).thenReturn(Future.successful(OptedOut))
      when(auditConnectorMock.sendEvent(any[DataEvent])(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(AuditResult.Success))

      searchService.optOut(validSaUtr, "my optOut reason").futureValue mustBe OptedOut

      val expectedAuditEvent =
        searchService.createOptOutEvent(
          "me",
          validSaUtr,
          Some(optedInPreference(entityId)),
          Some(optedOutPreference(entityId)),
          OptedOut,
          "my optOut reason"
        )
      verify(auditConnectorMock)
        .sendMergedEvent(argThat(isSimilar(expectedAuditEvent)))(any[HeaderCarrier], any[ExecutionContext])
    }

    "create an audit event when the user is not opted out as it is not found" in {
      when(entityResolverConnectorMock.getPreferenceDetails(validSaUtr))
        .thenReturn(Future.successful(None), Future.successful(None))
      when(entityResolverConnectorMock.getTaxIdentifiers(validSaUtr)).thenReturn(Future.successful(Seq.empty))
      when(entityResolverConnectorMock.optOut(validSaUtr)).thenReturn(Future.successful(PreferenceNotFound))
      when(auditConnectorMock.sendEvent(any[DataEvent])(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(AuditResult.Success))

      searchService.optOut(validSaUtr, "my optOut reason").futureValue mustBe PreferenceNotFound

      val expectedAuditEvent =
        searchService.createOptOutEvent("me", validSaUtr, None, None, PreferenceNotFound, "my optOut reason")
      verify(auditConnectorMock)
        .sendMergedEvent(argThat(isSimilar(expectedAuditEvent)))(any[HeaderCarrier], any[ExecutionContext])
    }

    "create an audit event when the user is already opted out" in {
      when(entityResolverConnectorMock.getPreferenceDetails(validSaUtr))
        .thenReturn(
          Future.successful(optedOutPreferenceDetails(entityId)),
          Future.successful(optedOutPreferenceDetails(entityId))
        )
      when(preferencesConnectorMock.getPreferencesEvents(any[String])(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(List.empty[Event]))
      when(entityResolverConnectorMock.getTaxIdentifiers(validSaUtr)).thenReturn(Future.successful(taxIdentifiers))
      when(entityResolverConnectorMock.optOut(validSaUtr)).thenReturn(Future.successful(AlreadyOptedOut))
      when(auditConnectorMock.sendEvent(any[DataEvent])(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(AuditResult.Success))

      searchService.optOut(validSaUtr, "my optOut reason").futureValue mustBe AlreadyOptedOut

      val expectedAuditEvent =
        searchService.createOptOutEvent(
          "me",
          validSaUtr,
          Some(optedOutPreference(entityId)),
          Some(optedOutPreference(entityId)),
          AlreadyOptedOut,
          "my optOut reason"
        )
      verify(auditConnectorMock)
        .sendMergedEvent(argThat(isSimilar(expectedAuditEvent)))(any[HeaderCarrier], any[ExecutionContext])
    }
  }

  "createSearchEvent" should {
    "generate the correct event when the preference exists" in {
      val preference = Preference(
        entityId = Some(EntityId("383cfb1b-5f57-417b-9380-545f35c29a22")),
        genericPaperless = true,
        genericUpdatedAt = genericUpdatedAt,
        email = Some(
          Email(
            address = "john.doe@digital.hmrc.gov.uk",
            verified = true,
            verifiedOn = verifiedOn,
            language = None,
            false,
            None
          )
        ),
        taxIdentifiers = Seq(TaxIdentifier("sautr", "123"), TaxIdentifier("nino", "ABC")),
        "",
        List.empty[Event]
      )
      val event = searchService.createSearchEvent("me", TaxIdentifier("sautr", "123"), Some(preference))

      event.auditSource mustBe "preferences-admin-frontend"
      event.auditType mustBe "TxSucceeded"
      event.request.detail mustBe Map(
        "preference"   -> s"${Json.toJson(preference)}",
        "result"       -> "Found",
        "query"        -> "{\"name\":\"sautr\",\"value\":\"123\"}",
        "DataCallType" -> "request",
        "user"         -> "me"
      )
      event.request.tags("transactionName") mustBe "Paperless opt out search"
    }

    "generate the correct event when the preference does not exist" in {
      val event = searchService.createSearchEvent("me", TaxIdentifier("sautr", "123"), None)

      event.auditSource mustBe "preferences-admin-frontend"
      event.auditType mustBe "TxSucceeded"
      event.request.detail mustBe Map(
        "preference"   -> "Not found",
        "result"       -> "Not found",
        "query"        -> "{\"name\":\"sautr\",\"value\":\"123\"}",
        "DataCallType" -> "request",
        "user"         -> "me"
      )
      event.request.tags("transactionName") mustBe "Paperless opt out search"

    }
  }

  "createOptoutEvent" should {

    "generate the correct event user is opted out" in {
      val event = searchService.createOptOutEvent(
        "me",
        validSaUtr,
        Some(optedInPreference(entityId)),
        Some(optedOutPreference(entityId)),
        OptedOut,
        "my optOut reason"
      )

      event.auditSource mustBe "preferences-admin-frontend"
      event.auditType mustBe "TxSucceeded"
      event.request.detail mustBe Map(
        "optOutReason"       -> "my optOut reason",
        "query"              -> "{\"name\":\"sautr\",\"value\":\"123456789\"}",
        "originalPreference" -> s"${Json.toJson(optedInPreference(entityId))}",
        "DataCallType"       -> "request",
        "newPreference"      -> s"${Json.toJson(optedOutPreference(entityId))}",
        "reasonOfFailure"    -> "Done",
        "user"               -> "me"
      )
      event.request.tags("transactionName") mustBe "Manual opt out from paperless"
    }

    "generate the correct event when the preference already opted out" in {
      val event = searchService.createOptOutEvent(
        "me",
        validSaUtr,
        Some(optedOutPreference(entityId)),
        Some(optedOutPreference(entityId)),
        AlreadyOptedOut,
        "my optOut reason"
      )

      event.auditSource mustBe "preferences-admin-frontend"
      event.auditType mustBe "TxFailed"
      event.request.detail mustBe Map(
        "optOutReason"       -> "my optOut reason",
        "query"              -> "{\"name\":\"sautr\",\"value\":\"123456789\"}",
        "originalPreference" -> s"${Json.toJson(optedOutPreference(entityId))}",
        "DataCallType"       -> "request",
        "newPreference"      -> s"${Json.toJson(optedOutPreference(entityId))}",
        "reasonOfFailure"    -> "Preference already opted out",
        "user"               -> "me"
      )
      event.request.tags("transactionName") mustBe "Manual opt out from paperless"

    }

    "generate the correct event when the preference does not exist" in {
      val event = searchService.createOptOutEvent("me", validSaUtr, None, None, PreferenceNotFound, "my optOut reason")

      event.auditSource mustBe "preferences-admin-frontend"
      event.auditType mustBe "TxFailed"
      event.request.detail mustBe Map(
        "optOutReason"       -> "my optOut reason",
        "query"              -> "{\"name\":\"sautr\",\"value\":\"123456789\"}",
        "originalPreference" -> "Not found",
        "DataCallType"       -> "request",
        "newPreference"      -> "Not found",
        "reasonOfFailure"    -> "Preference not found",
        "user"               -> "me"
      )
      event.request.tags("transactionName") mustBe "Manual opt out from paperless"

    }

    "generate the correct event when entity is not found" in {
      val event = searchService.createOptOutEvent("me", validSaUtr, None, None, PreferenceNotFound, "my optOut reason")

      event.auditSource mustBe "preferences-admin-frontend"
      event.auditType mustBe "TxFailed"
      event.request.tags("transactionName") mustBe "Manual opt out from paperless"
      event.request.detail mustBe Map(
        "optOutReason"       -> "my optOut reason",
        "query"              -> "{\"name\":\"sautr\",\"value\":\"123456789\"}",
        "originalPreference" -> "Not found",
        "DataCallType"       -> "request",
        "newPreference"      -> "Not found",
        "reasonOfFailure"    -> "Preference not found",
        "user"               -> "me"
      )

      event.response.tags("transactionName") mustBe "Manual opt out from paperless"
      event.response.detail mustBe Map(
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
}

trait SearchServiceTestCase extends SpecBase {

  val validSaUtr = TaxIdentifier("sautr", "123456789")
  val validNino = TaxIdentifier("nino", "CE067583D")
  val validItsa = TaxIdentifier("HMRC-MTD-IT", "testItsa123")
  val invalidNino = TaxIdentifier("nino", "123123456S")
  val validEmailid = TaxIdentifier("email", "test@test.com")
  val unknownEmailid = TaxIdentifier("email", "test9@test.com")

  val genericUpdatedAt = Some(ZonedDateTime.of(2018, 2, 15, 0, 0, 0, 0, ZoneOffset.UTC))
  val verifiedOn = genericUpdatedAt

  val verifiedEmail = Email(
    "john.doe@digital.hmrc.gov.uk",
    verified = true,
    verifiedOn = verifiedOn,
    language = Some("cy"),
    false,
    None
  )

  def preferenceDetails(genericPaperless: Boolean, entityId: String) = {
    val email = if (genericPaperless) Some(verifiedEmail) else None
    Some(
      PreferenceDetails(
        genericPaperless,
        genericUpdatedAt,
        None,
        email,
        entityId = Some(EntityId(entityId))
      )
    )
  }

  def preferenceDetailsList(genericPaperless: Boolean, entityId: String) = {
    val email = if (genericPaperless) Some(verifiedEmail) else None
    List(PreferenceDetails(genericPaperless, genericUpdatedAt, None, email, entityId = Some(EntityId(entityId))))
  }

  def multiplepreferenceDetails(genericPaperless: Boolean, entityId: String) = {
    val email = if (genericPaperless) Some(verifiedEmail) else None
    List(
      PreferenceDetails(genericPaperless, genericUpdatedAt, None, email, entityId = Some(EntityId(entityId))),
      PreferenceDetails(genericPaperless, genericUpdatedAt, None, email, entityId = Some(EntityId(entityId)))
    )
  }

  val optedInPreferenceDetails = (entityId: String) => preferenceDetails(genericPaperless = true, entityId)
  val optedOutPreferenceDetails = (entityId: String) => preferenceDetails(genericPaperless = false, entityId)
  val optedInPreferenceDetailsList = (entityId: String) => preferenceDetailsList(genericPaperless = true, entityId)
  val optedInPreferenceDetailsList2 = (entityId: String) => multiplepreferenceDetails(genericPaperless = true, entityId)

  val taxIdentifiers = Seq(validSaUtr, validNino, validItsa)

  def optedInPreference(entityId: String) = Preference(
    entityId = Some(EntityId(entityId)),
    genericPaperless = true,
    genericUpdatedAt = genericUpdatedAt,
    email = Some(verifiedEmail),
    taxIdentifiers = taxIdentifiers,
    "",
    List.empty[Event]
  )
  def optedInPreferenceList(entityId: String) = List(
    Preference(
      entityId = Some(EntityId(entityId)),
      genericPaperless = true,
      genericUpdatedAt = genericUpdatedAt,
      email = Some(verifiedEmail),
      taxIdentifiers = taxIdentifiers,
      "",
      List.empty[Event]
    ),
    Preference(
      entityId = Some(EntityId(entityId)),
      genericPaperless = true,
      genericUpdatedAt = genericUpdatedAt,
      email = Some(verifiedEmail),
      taxIdentifiers = taxIdentifiers,
      "",
      List.empty[Event]
    )
  )

  def optedOutPreference(entityId: String) = Preference(
    entityId = Some(EntityId(entityId)),
    genericPaperless = false,
    genericUpdatedAt = genericUpdatedAt,
    email = None,
    taxIdentifiers = taxIdentifiers,
    "",
    List.empty[Event]
  )
  val config = Configuration.from(Map("appName" -> "preferences-admin-frontend"))
  val searchService =
    new SearchService(entityResolverConnectorMock, preferencesConnectorMock, auditConnectorMock, config)
}
