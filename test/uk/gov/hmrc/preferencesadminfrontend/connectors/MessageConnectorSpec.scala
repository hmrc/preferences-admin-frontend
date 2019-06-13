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

import java.util.concurrent.TimeoutException

import akka.actor.ActorSystem
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status
import play.api.libs.json._
import play.api.{Configuration, Environment}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.preferencesadminfrontend.FrontendAuditConnector
import uk.gov.hmrc.preferencesadminfrontend.model._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class MessageConnectorSpec extends UnitSpec with ScalaFutures with GuiceOneAppPerSuite {

  "Rescindments" should {

    "addRescindments" should {
      "return a valid update result with status 200" in new TestCase {
        val result = messageConnectorHttpMock(expectedAddRescindmentsPath, rescindmentUpdateResultJson, Status.OK)
          .addRescindments(rescindmentRequest).futureValue
        result shouldBe rescindmentUpdateResult
      }
    }

    "sendRescindmentAlerts" should {
      "return a valid alert result with status 200" in new TestCase {
        val result = messageConnectorHttpMock(expectedSendRescindmentAlertsPath, rescindmentAlertsResultJson, Status.OK)
          .sendRescindmentAlerts().futureValue
        result shouldBe rescindmentAlertsResult
      }
    }
  }

  "GMC Batches Admin" should {

    "getWhitelist" should {
      "return a valid sequence of batches with status 200" in new TestCase {
        val result = messageConnectorHttpMock(expectedGetWhitelistPath, Json.obj(), Status.OK)
          .getWhitelist().futureValue
        result.status shouldBe Status.OK
      }

      "return a BAD GATEWAY with an error message when an error is thrown" in new TestCase {
        val result = messageConnectorHttpMock(expectedGetWhitelistPath, new TimeoutException("timeout error"))
          .getWhitelist().futureValue
        result.status shouldBe Status.BAD_GATEWAY
        result.body should include("timed out with message 'timeout error'")
      }
    }

    "addFormIdToWhitelist" should {
      "return a valid sequence of batches with status 200" in new TestCase {
        val result = messageConnectorHttpMock(expectedAddFormIdToWhitelistPath, Json.obj(), Status.OK)
          .addFormIdToWhitelist(WhitelistEntry("SA316", "reason")).futureValue
        result.status shouldBe Status.OK
      }

      "return a BAD GATEWAY with an error message when an error is thrown" in new TestCase {
        val result = messageConnectorHttpMock(expectedAddFormIdToWhitelistPath, new TimeoutException("timeout error"))
          .addFormIdToWhitelist(WhitelistEntry("SA316", "reason")).futureValue
        result.status shouldBe Status.BAD_GATEWAY
        result.body should include("timed out with message 'timeout error'")
      }
    }

    "deleteFormIdFromWhitelist" should {
      "return a valid sequence of batches with status 200" in new TestCase {
        val result = messageConnectorHttpMock(expectedDeleteFormIdFromWhitelistPath, Json.obj(), Status.OK)
          .deleteFormIdFromWhitelist(WhitelistEntry("SA316", "reason")).futureValue
        result.status shouldBe Status.OK
      }

      "return a BAD GATEWAY with an error message when an error is thrown" in new TestCase {
        val result = messageConnectorHttpMock(expectedDeleteFormIdFromWhitelistPath, new TimeoutException("timeout error"))
          .deleteFormIdFromWhitelist(WhitelistEntry("SA316", "reason")).futureValue
        result.status shouldBe Status.BAD_GATEWAY
        result.body should include("timed out with message 'timeout error'")
      }
    }

    "getGmcBatches" should {
      "return a valid sequence of batches with status 200" in new TestCase {
        val result = messageConnectorHttpMock(expectedGetGmcBatchesPath, getGmcBatchesResultJson, Status.OK)
          .getGmcBatches().futureValue
        result.status shouldBe Status.OK
        result.json shouldBe getGmcBatchesResultJson
      }

      "return a BAD GATEWAY with an error message when an error is thrown" in new TestCase {
        val result = messageConnectorHttpMock(expectedGetGmcBatchesPath, new TimeoutException("timeout error"))
          .getGmcBatches().futureValue
        result.status shouldBe Status.BAD_GATEWAY
        result.body should include("timed out with message 'timeout error'")
      }
    }

    "getRandomMessagePreview" should {
      "return a valid sequence of batches with status 200" in new TestCase {
        val result = messageConnectorHttpMock(expectedGetRandomMessagePreviewPath, getRandomMessagePreviewResultJson, Status.OK)
          .getRandomMessagePreview(gmcBatch).futureValue
        result.status shouldBe Status.OK
        result.json shouldBe getRandomMessagePreviewResultJson
      }

      "return a BAD GATEWAY with an error message when an error is thrown" in new TestCase {
        val result = messageConnectorHttpMock(expectedGetRandomMessagePreviewPath, new TimeoutException("timeout error"))
          .getRandomMessagePreview(gmcBatch).futureValue
        result.status shouldBe Status.BAD_GATEWAY
        result.body should include("timed out with message 'timeout error'")
      }
    }

    "approveGmcBatch" should {
      "return a valid sequence of batches with status 200" in new TestCase {
        val result = messageConnectorHttpMock(expectedApproveGmcBatchPath, Json.obj(), Status.OK)
          .approveGmcBatch(gmcBatchApproval).futureValue
        result.status shouldBe Status.OK
      }

      "return a BAD GATEWAY with an error message when an error is thrown" in new TestCase {
        val result = messageConnectorHttpMock(expectedApproveGmcBatchPath, new TimeoutException("timeout error"))
          .approveGmcBatch(gmcBatchApproval).futureValue
        result.status shouldBe Status.BAD_GATEWAY
        result.body should include("timed out with message 'timeout error'")
      }
    }

    "rejectGmcBatch" should {
      "return a valid sequence of batches with status 200" in new TestCase {
        val result = messageConnectorHttpMock(expectedRejectGmcBatchPath, Json.obj(), Status.OK)
          .rejectGmcBatch(gmcBatchApproval).futureValue
        result.status shouldBe Status.OK
      }

      "return a BAD GATEWAY with an error message when an error is thrown" in new TestCase {
        val result = messageConnectorHttpMock(expectedRejectGmcBatchPath, new TimeoutException("timeout error"))
          .rejectGmcBatch(gmcBatchApproval).futureValue
        result.status shouldBe Status.BAD_GATEWAY
        result.body should include("timed out with message 'timeout error'")
      }
    }
  }

  trait TestCase extends MockitoSugar {
    implicit val hc: HeaderCarrier = HeaderCarrier()

    lazy val environment = app.injector.instanceOf[Environment]
    lazy val configuration = app.injector.instanceOf[Configuration]
    lazy val frontendAuditConnector = app.injector.instanceOf[FrontendAuditConnector]
    lazy val actorSystem = app.injector.instanceOf[ActorSystem]

    val expectedAddRescindmentsPath = s"/admin/message/add-rescindments"
    val expectedGetWhitelistPath = s"/admin/message/brake/gmc/whitelist"
    val expectedAddFormIdToWhitelistPath = s"/admin/message/brake/gmc/whitelist/add"
    val expectedDeleteFormIdFromWhitelistPath = s"/admin/message/brake/gmc/whitelist/delete"
    val expectedGetGmcBatchesPath = s"/admin/message/brake/gmc/batches"
    val expectedGetRandomMessagePreviewPath = s"/admin/message/brake/random"
    val expectedApproveGmcBatchPath = s"/admin/message/brake/accept"
    val expectedRejectGmcBatchPath = s"/admin/message/brake/reject"
    val expectedSendRescindmentAlertsPath = s"/admin/send-rescindment-alerts"
    val emptyJson = Json.obj()
    val rescindmentRequest = RescindmentRequest(
      batchId = "1234567",
      formId = "SA316",
      date = "2017-03-16",
      reference = "ref-test",
      emailTemplateId = "rescindedMessageAlert"
    )
    val rescindmentUpdateResultJson = Json.obj(
      "tried" -> 1,
      "succeeded" -> 1,
      "alreadyUpdated" -> 0,
      "invalidState" -> 0
    )
    val rescindmentUpdateResult = RescindmentUpdateResult(
      tried = 1, succeeded = 1, alreadyUpdated = 0, invalidState = 0
    )
    val rescindmentAlertsResultJson = Json.obj(
      "alerts sent" -> 1,
      "ready for retrial" -> 1,
      "failed permanently" -> 0,
      "hard copy requested" -> 0
    )

    val getGmcBatchesResultJson = Json.parse("""
        |[
        |    {
        |        "formId": "SA302",
        |        "issueDate": "10 APR 2019",
        |        "batchId": "10034",
        |        "templateId": "newMessageAlert_SA302",
        |        "count": 43457
        |    },
        |    {
        |        "formId": "P800",
        |        "issueDate": "14 APR 2019",
        |        "batchId": "10896",
        |        "templateId": "newMessageAlert_P800",
        |        "count": 35408
        |    },
        |    {
        |        "formId": "SA312",
        |        "issueDate": "15 APR 2019",
        |        "batchId": "10087",
        |        "templateId": "newMessageAlert_SA312",
        |        "count": 23685
        |    }
        |]
      """.stripMargin)

    val getRandomMessagePreviewResultJson = Json.parse(
      """
        |{
        | "subject":"Reminder to file a Self Assessment return",
        | "content":"PHA+RGVhciBjdXN0b21lciw8L3A+CjxwPkhNUkMgaXMgb2ZmZXJpbmcgYSByYW5nZSBvZiBzdXBwb3J0IGFoZWFkIG9mIHRoZSBjaGFuZ2VzIGR1ZSBmcm9tIEHigIxwcuKAjGlsIDLigIwwMeKAjDkgYWZmZWN0aW5nIFZBVC1yZWdpc3RlcmVkIGJ1c2luZXNzZXMgd2l0aCBhIHRheGFibGUgdHVybm92ZXIgYWJvdmUgwqM4NSwwMDAuPC9wPgo8cD5IZXJl4oCZcyBhIHNlbGVjdGlvbiBmb3IgeW91LjwvcD4KPHA+PGI+TWFraW5nIFRheCBEaWdpdGFsPC9iPjwvcD4KPHA+UHJvdmlkaW5nIGFuIG92ZXJ2aWV3IG9mIE1ha2luZyBUYXggRGlnaXRhbCwgdGhpcyBsaXZlIHdlYmluYXIgaW5jbHVkZXMgZGlnaXRhbCByZWNvcmQga2VlcGluZywgY29tcGF0aWJsZSBzb2Z0d2FyZSwgc2lnbmluZyB1cCBmb3IgTWFraW5nIFRheCBEaWdpdGFsLCB3b3JraW5nIHdpdGggYWdlbnRzIGFuZCBzdWJtaXR0aW5nIFZBVCByZXR1cm5zIGRpcmVjdGx5IGZyb20gdGhlIGJ1c2luZXNzLjwvcD4KPHA+WW91IGNhbiBhc2sgcXVlc3Rpb25zIHVzaW5nIHRoZSBvbi1zY3JlZW4gdGV4dCBib3guPC9wPgo8cD48YSBocmVmPSJodHRwczovL2xpbmtzLmFkdmljZS5obXJjLmdvdi51ay90cmFjaz90eXBlPWNsaWNrJmVuaWQ9WldGelBURW1iWE5wWkQwbVlYVnBaRDBtYldGcGJHbHVaMmxrUFRJd01Ua3dNekF4TGpJME9ESTNOekVtYldWemMyRm5aV2xrUFUxRVFpMVFVa1F0UWxWTUxUSXdNVGt3TXpBeExqSTBPREkzTnpFbVpHRjBZV0poYzJWcFpEMHhNREF4Sm5ObGNtbGhiRDB4TnpBNE9UYzFNQ1psYldGcGJHbGtQWEp2WW10M1lXeHdiMnhsUUdkdFlXbHNMbU52YlNaMWMyVnlhV1E5Y205aWEzZGhiSEJ2YkdWQVoyMWhhV3d1WTI5dEpuUmhjbWRsZEdsa1BTWm1iRDBtWlhoMGNtRTlUWFZzZEdsMllYSnBZWFJsU1dROUppWW0mJiYxMDEmJiZodHRwczovL2F0dGVuZGVlLmdvdG93ZWJpbmFyLmNvbS9ydC8xNDg4NDY5NzYwMzI2MDI1NzI5P3NvdXJjZT1DYW1wYWlnbi1NYXItMmEiPkNob29zZSBhIGRhdGUgYW5kIHRpbWU8L2E+PC9wPgo8cD5UaGVyZSBhcmUgYWxzbyBzaG9ydCB2aWRlb3Mgb24gb3VyIFlvdVR1YmUgY2hhbm5lbCwgaW5jbHVkaW5nICc8YSBocmVmPSJodHRwczovL2xpbmtzLmFkdmljZS5obXJjLmdvdi51ay90cmFjaz90eXBlPWNsaWNrJmVuaWQ9WldGelBURW1iWE5wWkQwbVlYVnBaRDBtYldGcGJHbHVaMmxrUFRJd01Ua3dNekF4TGpJME9ESTNOekVtYldWemMyRm5aV2xrUFUxRVFpMVFVa1F0UWxWTUxUSXdNVGt3TXpBeExqSTBPREkzTnpFbVpHRjBZV0poYzJWcFpEMHhNREF4Sm5ObGNtbGhiRDB4TnpBNE9UYzFNQ1psYldGcGJHbGtQWEp2WW10M1lXeHdiMnhsUUdkdFlXbHNMbU52YlNaMWMyVnlhV1E5Y205aWEzZGhiSEJ2YkdWQVoyMWhhV3d1WTI5dEpuUmhjbWRsZEdsa1BTWm1iRDBtWlhoMGNtRTlUWFZzZEdsMllYSnBZWFJsU1dROUppWW0mJiYxMDImJiZodHRwczovL3d3dy55b3V0dWJlLmNvbS93YXRjaD92PUhTSGJEaldabDN3JmluZGV4PTQmbGlzdD1QTDhFY25oZUR0MXppMWlwazFxZXhyd2RBVTVPNkxTODRhJnV0bV9zb3VyY2U9SE1SQy1EQ1MtTWFyLTJhJnV0bV9jYW1wYWlnbj1EQ1MtQ2FtcGFpZ24mdXRtX21lZGl1bT1lbWFpbCI+SG93IGRvZXMgTWFraW5nIFRheCBEaWdpdGFsIGZvciBWQVQgYWZmZWN0IHlvdT88L2E+JyBhbmQgJzxhIGhyZWY9Imh0dHBzOi8vbGlua3MuYWR2aWNlLmhtcmMuZ292LnVrL3RyYWNrP3R5cGU9Y2xpY2smZW5pZD1aV0Z6UFRFbWJYTnBaRDBtWVhWcFpEMG1iV0ZwYkdsdVoybGtQVEl3TVRrd016QXhMakkwT0RJM056RW1iV1Z6YzJGblpXbGtQVTFFUWkxUVVrUXRRbFZNTFRJd01Ua3dNekF4TGpJME9ESTNOekVtWkdGMFlXSmhjMlZwWkQweE1EQXhKbk5sY21saGJEMHhOekE0T1RjMU1DWmxiV0ZwYkdsa1BYSnZZbXQzWVd4d2IyeGxRR2R0WVdsc0xtTnZiU1oxYzJWeWFXUTljbTlpYTNkaGJIQnZiR1ZBWjIxaGFXd3VZMjl0Sm5SaGNtZGxkR2xrUFNabWJEMG1aWGgwY21FOVRYVnNkR2wyWVhKcFlYUmxTV1E5SmlZbSYmJjEwMyYmJmh0dHBzOi8vd3d3LnlvdXR1YmUuY29tL3dhdGNoP3Y9a09LRDRrSHZsekkmaW5kZXg9MyZsaXN0PVBMOEVjbmhlRHQxemkxaXBrMXFleHJ3ZEFVNU82TFM4NGEmdXRtX3NvdXJjZT1ITVJDLURDUy1NYXItMmEmdXRtX2NhbXBhaWduPURDUy1DYW1wYWlnbiZ1dG1fbWVkaXVtPWVtYWlsIj5Ib3cgdG8gc2lnbiB1cCBmb3IgTWFraW5nIFRheCBEaWdpdGFsIGZvciBWQVQ/PC9hPicg4oCTIGF2YWlsYWJsZSB0byB2aWV3IGFueXRpbWUuPC9wPgo8cD5WaXNpdCBITVJD4oCZcyA8YSBocmVmPSJodHRwczovL2xpbmtzLmFkdmljZS5obXJjLmdvdi51ay90cmFjaz90eXBlPWNsaWNrJmVuaWQ9WldGelBURW1iWE5wWkQwbVlYVnBaRDBtYldGcGJHbHVaMmxrUFRJd01Ua3dNekF4TGpJME9ESTNOekVtYldWemMyRm5aV2xrUFUxRVFpMVFVa1F0UWxWTUxUSXdNVGt3TXpBeExqSTBPREkzTnpFbVpHRjBZV0poYzJWcFpEMHhNREF4Sm5ObGNtbGhiRDB4TnpBNE9UYzFNQ1psYldGcGJHbGtQWEp2WW10M1lXeHdiMnhsUUdkdFlXbHNMbU52YlNaMWMyVnlhV1E5Y205aWEzZGhiSEJ2YkdWQVoyMWhhV3d1WTI5dEpuUmhjbWRsZEdsa1BTWm1iRDBtWlhoMGNtRTlUWFZzZEdsMllYSnBZWFJsU1dROUppWW0mJiYxMDQmJiZodHRwczovL29ubGluZS5obXJjLmdvdi51ay93ZWJjaGF0cHJvZC9jb21tdW5pdHkvZm9ydW1zL3Nob3cvMTAzLnBhZ2UiPk9ubGluZSBDdXN0b21lciBGb3J1bTwvYT4gaWYgeW914oCZdmUgZ290IGEgcXVlc3Rpb24gYWJvdXQgTWFraW5nIFRheCBEaWdpdGFsIOKAkyBzZWUgd2hhdCBvdGhlcnMgYXJlIHRhbGtpbmcgYWJvdXQsIGFzayB5b3VyIG93biBxdWVzdGlvbnMgYW5kIGdldCBhbnN3ZXJzIGZyb20gdGhlIGV4cGVydHMuPC9wPgo8cD5ITVJDIG9ubGluZSBndWlkYW5jZSDigJMgaGVscGluZyB5b3UgZ2V0IGl0IHJpZ2h0LjwvcD4KPHA+QWxpc29uIFdhbHNoPC9wPgo8cD5IZWFkIG9mIERpZ2l0YWwgQ29tbXVuaWNhdGlvbiBTZXJ2aWNlczwvcD4=",
        | "externalRefId":"9834763878934",
        | "messageType":"mailout-batch",
        | "issueDate":"05 APR 2019",
        | "taxIdentifierName":"sautr"
        |}
      """.stripMargin)

    val rescindmentAlertsResult = RescindmentAlertsResult(
      sent = 1, requeued = 1, failed = 0, hardCopyRequested = 0
    )

    val gmcBatch = GmcBatch(
      "123456789",
      "SA359",
      "2017-03-16",
      "newMessageAlert_SA359",
      Some(15778)
    )

    val gmcBatchApproval = GmcBatchApproval(
      "123456789",
      "SA359",
      "2017-03-16",
      "newMessageAlert_SA359",
      "some reason"
    )

    def messageConnectorHttpMock(expectedPath: String, jsonBody: JsValue, status: Int): MessageConnector = {
      new MessageConnector(frontendAuditConnector, environment, configuration, actorSystem) {

        override def doGet(url: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
          url should include(expectedPath)
          Future.successful(HttpResponse(status, Some(jsonBody)))
        }

        override def doPost[A](url: String, body: A, headers: Seq[(String, String)])
                              (implicit rds: Writes[A], hc: HeaderCarrier): Future[HttpResponse] = {
          url should include(expectedPath)
          Future.successful(HttpResponse(status, Some(jsonBody)))
        }

        override def doEmptyPost[A](url: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
          url should include(expectedPath)
          Future.successful(HttpResponse(status, Some(jsonBody)))
        }
      }
    }

    def messageConnectorHttpMock(expectedPath: String, error: Throwable): MessageConnector = {
      new MessageConnector(frontendAuditConnector, environment, configuration, actorSystem) {

        override def doGet(url: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
          url should include(expectedPath)
          Future.failed(error)
        }

        override def doPost[A](url: String, body: A, headers: Seq[(String, String)])
                              (implicit rds: Writes[A], hc: HeaderCarrier): Future[HttpResponse] = {
          url should include(expectedPath)
          Future.failed(error)
        }

        override def doEmptyPost[A](url: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
          url should include(expectedPath)
          Future.failed(error)
        }
      }
    }
  }
}
