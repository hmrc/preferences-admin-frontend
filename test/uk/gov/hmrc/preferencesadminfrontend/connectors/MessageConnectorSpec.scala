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

import org.apache.pekko.actor.ActorSystem
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.*
import play.api.{ Configuration, Environment }
import uk.gov.hmrc.http.client.{ HttpClientV2, RequestBuilder }
import uk.gov.hmrc.http.{ HeaderCarrier, HttpResponse, StringContextOps }
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.preferencesadminfrontend.model.*

import java.util.concurrent.TimeoutException
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ ExecutionContext, Future }

class MessageConnectorSpec extends PlaySpec with ScalaFutures with GuiceOneAppPerSuite {

  import play.api.inject._

  val mockHttp: HttpClientV2 = mock[HttpClientV2]
  val requestBuilder: RequestBuilder = mock[RequestBuilder]
  implicit val hc: HeaderCarrier = HeaderCarrier()

  lazy val environment = app.injector.instanceOf[Environment]
  lazy val configuration = app.injector.instanceOf[Configuration]
  lazy val actorSystem = app.injector.instanceOf[ActorSystem]

  override lazy val app = new GuiceApplicationBuilder()
    .overrides(bind[HttpClientV2].toInstance(mockHttp))
    .overrides(bind[RequestBuilder].toInstance(requestBuilder))
    .build()

  def serviceUrl: String = app.injector.instanceOf[ServicesConfig].baseUrl("message")
  def secureMessageServiceUrl: String =
    app.injector.instanceOf[ServicesConfig].baseUrl("secure-message") + "/secure-messaging"

  "GMC Batches Admin" should {

    "getAllowlist" should {
      "return a valid sequence of batches with status 200" in new TestCase {
        val expectedPath = url"$serviceUrl/admin/message/brake/gmc/allowlist"
        when(mockHttp.get(any)(any)).thenReturn(requestBuilder)
        when(requestBuilder.execute[HttpResponse](any, any))
          .thenReturn(Future.successful(HttpResponse(Status.OK, Json.obj(), Map.empty)))

        val result = app.injector.instanceOf[MessageConnector].getAllowlist().futureValue
        result.status mustBe Status.OK
      }

      "return a BAD GATEWAY with an error message when an error is thrown" in new TestCase {
        val expectedPath = url"$serviceUrl/admin/message/brake/gmc/allowlist"
        when(mockHttp.get(any)(any)).thenReturn(requestBuilder)
        when(requestBuilder.execute[HttpResponse](any, any))
          .thenReturn(Future.failed(new TimeoutException("timeout error")))

        val result = app.injector.instanceOf[MessageConnector].getAllowlist().futureValue
        result.status mustBe Status.BAD_GATEWAY
        result.body must include("timeout error")
      }
    }

    "addFormIdToAllowlist" should {
      "return a valid sequence of batches with status 200" in new TestCase {
        val expectedPath = url"$serviceUrl/admin/message/brake/gmc/allowlist/add"
        when(mockHttp.post(any)(any)).thenReturn(requestBuilder)
        when(requestBuilder.withBody(any)(any, any, any)).thenReturn(requestBuilder)
        when(requestBuilder.execute[HttpResponse](any, any))
          .thenReturn(Future.successful(HttpResponse(Status.OK, Json.obj(), Map.empty)))

        val result = app.injector
          .instanceOf[MessageConnector]
          .addFormIdToAllowlist(AllowlistEntry("SA316", "reason"))
          .futureValue
        result.status mustBe Status.OK
      }

      "return a BAD GATEWAY with an error message when an error is thrown" in new TestCase {
        val expectedPath = url"$serviceUrl/admin/message/brake/gmc/allowlist/add"
        when(mockHttp.post(any)(any)).thenReturn(requestBuilder)
        when(requestBuilder.withBody(any)(any, any, any)).thenReturn(requestBuilder)
        when(requestBuilder.execute[HttpResponse](any, any))
          .thenReturn(Future.failed(new TimeoutException("timeout error")))

        val result = app.injector
          .instanceOf[MessageConnector]
          .addFormIdToAllowlist(AllowlistEntry("SA316", "reason"))
          .futureValue
        result.status mustBe Status.BAD_GATEWAY
        result.body must include("timeout error")
      }
    }

    "deleteFormIdFromAllowlist" should {
      "return a valid sequence of batches with status 200" in new TestCase {
        val expectedPath = url"$serviceUrl/admin/message/brake/gmc/allowlist/delete"

        when(mockHttp.post(any)(any)).thenReturn(requestBuilder)
        when(requestBuilder.withBody(any)(any, any, any)).thenReturn(requestBuilder)
        when(requestBuilder.execute[HttpResponse](any, any))
          .thenReturn(Future.successful(HttpResponse(Status.OK, Json.obj(), Map.empty)))

        val result = app.injector
          .instanceOf[MessageConnector]
          .deleteFormIdFromAllowlist(AllowlistEntry("SA316", "reason"))
          .futureValue
        result.status mustBe Status.OK
      }

      "return a BAD GATEWAY with an error message when an error is thrown" in new TestCase {
        val expectedPath = url"$serviceUrl/admin/message/brake/gmc/allowlist/delete"
        when(mockHttp.post(any)(any)).thenReturn(requestBuilder)
        when(requestBuilder.withBody(any)(any, any, any)).thenReturn(requestBuilder)
        when(requestBuilder.execute[HttpResponse](any, any))
          .thenReturn(Future.failed(new TimeoutException("timeout error")))

        val result = app.injector
          .instanceOf[MessageConnector]
          .deleteFormIdFromAllowlist(AllowlistEntry("SA316", "reason"))
          .futureValue
        result.status mustBe Status.BAD_GATEWAY
        result.body must include("timeout error")
      }
    }

    "getGmcBatches" should {
      "return a valid sequence of batches with status 200" in new TestCase {
        val expectedPath = url"$serviceUrl/admin/message/brake/gmc/batches"
        when(mockHttp.get(any)(any)).thenReturn(requestBuilder)
        when(requestBuilder.execute[HttpResponse](any, any))
          .thenReturn(Future.successful(HttpResponse(Status.OK, getGmcBatchesResultJson, Map.empty)))

        val result = app.injector.instanceOf[MessageConnector].getGmcBatches("v3").futureValue
        result.status mustBe Status.OK
        result.json mustBe getGmcBatchesResultJson
      }

      "return a BAD GATEWAY with an error message when an error is thrown" in new TestCase {
        val expectedPath = url"$serviceUrl/admin/message/brake/gmc/batches"
        when(mockHttp.get(any)(any)).thenReturn(requestBuilder)
        when(requestBuilder.execute[HttpResponse](any, any))
          .thenReturn(Future.failed(new TimeoutException("timeout error")))

        val result = app.injector.instanceOf[MessageConnector].getGmcBatches("v3").futureValue
        result.status mustBe Status.BAD_GATEWAY
        result.body must include("timeout error")
      }
    }

    "getRandomMessagePreview" should {
      "return a valid sequence of batches with status 200" in new TestCase {
        val expectedPath = url"$serviceUrl/admin/message/brake/random"
        when(mockHttp.post(any)(any)).thenReturn(requestBuilder)
        when(requestBuilder.withBody(any)(any, any, any)).thenReturn(requestBuilder)
        when(requestBuilder.execute[HttpResponse](any, any))
          .thenReturn(Future.successful(HttpResponse(Status.OK, getRandomMessagePreviewResultJson, Map.empty)))

        val result = app.injector.instanceOf[MessageConnector].getRandomMessagePreview(gmcBatch).futureValue
        result.status mustBe Status.OK
        result.json mustBe getRandomMessagePreviewResultJson
      }

      "return a BAD GATEWAY with an error message when an error is thrown" in new TestCase {
        val expectedPath = url"$serviceUrl/admin/message/brake/random"
        when(mockHttp.post(any)(any)).thenReturn(requestBuilder)
        when(requestBuilder.withBody(any)(any, any, any)).thenReturn(requestBuilder)
        when(requestBuilder.execute[HttpResponse](any, any))
          .thenReturn(Future.failed(new TimeoutException("timeout error")))

        val result = app.injector.instanceOf[MessageConnector].getRandomMessagePreview(gmcBatch).futureValue
        result.status mustBe Status.BAD_GATEWAY
        result.body must include("timeout error")
      }
    }

    "approveGmcBatch" should {
      "return a valid sequence of batches with status 200" in new TestCase {
        val expectedPath = url"$serviceUrl/admin/message/brake/accept"
        when(mockHttp.post(any)(any)).thenReturn(requestBuilder)
        when(requestBuilder.withBody(any)(any, any, any)).thenReturn(requestBuilder)
        when(requestBuilder.execute[HttpResponse](any, any))
          .thenReturn(Future.successful(HttpResponse(Status.OK, Json.obj(), Map.empty)))

        val result = app.injector.instanceOf[MessageConnector].approveGmcBatch(gmcBatchApproval).futureValue
        result.status mustBe Status.OK
      }

      "return a BAD GATEWAY with an error message when an error is thrown" in new TestCase {
        val expectedPath = url"$serviceUrl/admin/message/brake/accept"
        when(mockHttp.post(any)(any)).thenReturn(requestBuilder)
        when(requestBuilder.withBody(any)(any, any, any)).thenReturn(requestBuilder)
        when(requestBuilder.execute[HttpResponse](any, any))
          .thenReturn(Future.failed(new TimeoutException("timeout error")))

        val result = app.injector.instanceOf[MessageConnector].approveGmcBatch(gmcBatchApproval).futureValue
        result.status mustBe Status.BAD_GATEWAY
        result.body must include("timeout error")
      }
    }

    "rejectGmcBatch" should {
      "return a valid sequence of batches with status 200" in new TestCase {
        val expectedPath = url"$serviceUrl/admin/message/brake/reject"
        when(mockHttp.post(any)(any)).thenReturn(requestBuilder)
        when(requestBuilder.withBody(any)(any, any, any)).thenReturn(requestBuilder)
        when(requestBuilder.execute[HttpResponse](any, any))
          .thenReturn(Future.successful(HttpResponse(Status.OK, Json.obj(), Map.empty)))

        val result = app.injector.instanceOf[MessageConnector].rejectGmcBatch(gmcBatchApproval).futureValue
        result.status mustBe Status.OK
      }

      "return a BAD GATEWAY with an error message when an error is thrown" in new TestCase {
        val expectedPath = url"$serviceUrl/admin/message/brake/reject"

        when(mockHttp.post(any)(any)).thenReturn(requestBuilder)
        when(requestBuilder.withBody(any)(any, any, any)).thenReturn(requestBuilder)
        when(requestBuilder.execute[HttpResponse](any, any))
          .thenReturn(Future.failed(new TimeoutException("timeout error")))

        val result = app.injector.instanceOf[MessageConnector].rejectGmcBatch(gmcBatchApproval).futureValue
        result.status mustBe Status.BAD_GATEWAY
        result.body must include("timeout error")
      }
    }

    "getGmcBatches - v4 message" should {
      "return a valid sequence of batches with status 200" in new TestCase {
        val expectedPath = url"$secureMessageServiceUrl/admin/message/brake/gmc/batches"
        when(mockHttp.get(any)(any)).thenReturn(requestBuilder)
        when(requestBuilder.execute[HttpResponse](any, any))
          .thenReturn(Future.successful(HttpResponse(Status.OK, getGmcBatchesResultJson, Map.empty)))

        val result = app.injector.instanceOf[MessageConnector].getGmcBatches("v4").futureValue
        result.status mustBe Status.OK
        result.json mustBe getGmcBatchesResultJson
      }

      "return a BAD GATEWAY with an error message when an error is thrown" in new TestCase {
        val expectedPath = url"$secureMessageServiceUrl/admin/message/brake/gmc/batches"
        when(mockHttp.get(any)(any)).thenReturn(requestBuilder)
        when(requestBuilder.execute[HttpResponse](any, any))
          .thenReturn(Future.failed(new TimeoutException("timeout error")))

        val result = app.injector.instanceOf[MessageConnector].getGmcBatches("v4").futureValue
        result.status mustBe Status.BAD_GATEWAY
        result.body must include("timeout error")
      }
    }

    "getRandomMessagePreview - v4 message" should {
      "return a valid sequence of batches with status 200" in new TestCase {
        val expectedPath = url"$secureMessageServiceUrl/admin/message/brake/random"

        when(mockHttp.post(any)(any)).thenReturn(requestBuilder)
        when(requestBuilder.withBody(any)(any, any, any)).thenReturn(requestBuilder)
        when(requestBuilder.execute[HttpResponse](any, any))
          .thenReturn(Future.successful(HttpResponse(Status.OK, getRandomMessagePreviewResultJson, Map.empty)))

        val result = app.injector.instanceOf[MessageConnector].getRandomMessagePreview(gmcBatchV4).futureValue
        result.status mustBe Status.OK
        result.json mustBe getRandomMessagePreviewResultJson
      }

      "return a BAD GATEWAY with an error message when an error is thrown" in new TestCase {
        val expectedPath = url"$secureMessageServiceUrl/admin/message/brake/random"

        when(mockHttp.post(any)(any)).thenReturn(requestBuilder)
        when(requestBuilder.withBody(any)(any, any, any)).thenReturn(requestBuilder)
        when(requestBuilder.execute[HttpResponse](any, any))
          .thenReturn(Future.failed(new TimeoutException("timeout error")))

        val result = app.injector.instanceOf[MessageConnector].getRandomMessagePreview(gmcBatchV4).futureValue
        result.status mustBe Status.BAD_GATEWAY
        result.body must include("timeout error")
      }
    }

    "approveGmcBatch - v4 message" should {
      "return a valid sequence of batches with status 200" in new TestCase {
        val expectedPath = url"$secureMessageServiceUrl/admin/message/brake/accept"

        when(mockHttp.post(any)(any)).thenReturn(requestBuilder)
        when(requestBuilder.withBody(any)(any, any, any)).thenReturn(requestBuilder)
        when(requestBuilder.execute[HttpResponse](any, any))
          .thenReturn(Future.successful(HttpResponse(Status.OK, Json.obj(), Map.empty)))

        val result = app.injector.instanceOf[MessageConnector].approveGmcBatch(gmcBatchApprovalV4).futureValue
        result.status mustBe Status.OK
      }

      "return a BAD GATEWAY with an error message when an error is thrown" in new TestCase {
        val expectedPath = url"$secureMessageServiceUrl/admin/message/brake/accept"

        when(mockHttp.post(any)(any)).thenReturn(requestBuilder)
        when(requestBuilder.withBody(any)(any, any, any)).thenReturn(requestBuilder)
        when(requestBuilder.execute[HttpResponse](any, any))
          .thenReturn(Future.failed(new TimeoutException("timeout error")))

        val result = app.injector.instanceOf[MessageConnector].approveGmcBatch(gmcBatchApprovalV4).futureValue
        result.status mustBe Status.BAD_GATEWAY
        result.body must include("timeout error")
      }
    }

    "rejectGmcBatch - v4 message" should {
      "return a valid sequence of batches with status 200" in new TestCase {
        val expectedPath = url"$secureMessageServiceUrl/admin/message/brake/reject"

        when(mockHttp.post(any)(any)).thenReturn(requestBuilder)
        when(requestBuilder.withBody(any)(any, any, any)).thenReturn(requestBuilder)
        when(requestBuilder.execute[HttpResponse](any, any))
          .thenReturn(Future.successful(HttpResponse(Status.OK, Json.obj(), Map.empty)))

        val result = app.injector.instanceOf[MessageConnector].rejectGmcBatch(gmcBatchApprovalV4).futureValue
        result.status mustBe Status.OK
      }

      "return a BAD GATEWAY with an error message when an error is thrown" in new TestCase {
        val expectedPath = url"$secureMessageServiceUrl/admin/message/brake/reject"

        when(mockHttp.post(any)(any)).thenReturn(requestBuilder)
        when(requestBuilder.withBody(any)(any, any, any)).thenReturn(requestBuilder)
        when(requestBuilder.execute[HttpResponse](any, any))
          .thenReturn(Future.failed(new TimeoutException("timeout error")))

        val result = app.injector.instanceOf[MessageConnector].rejectGmcBatch(gmcBatchApprovalV4).futureValue
        result.status mustBe Status.BAD_GATEWAY
        result.body must include("timeout error")
      }
    }
  }

  "sendMessage" should {
    "return 200 on successful call to message" in new TestCase {
      val expectedPath = url"$serviceUrl/messages"

      when(mockHttp.post(any)(any)).thenReturn(requestBuilder)
      when(requestBuilder.withBody(any)(any, any, any)).thenReturn(requestBuilder)
      when(requestBuilder.execute[HttpResponse](any, any))
        .thenReturn(Future.successful(HttpResponse(Status.OK, Json.obj(), Map.empty)))

      val result = app.injector.instanceOf[MessageConnector].sendMessage(JsString("")).futureValue
      result.status mustBe Status.OK
    }

    "return a BAD GATEWAY with an error message when an error is thrown" in new TestCase {
      val expectedPath = url"$serviceUrl/messages"

      when(mockHttp.post(any)(any)).thenReturn(requestBuilder)
      when(requestBuilder.withBody(any)(any, any, any)).thenReturn(requestBuilder)
      when(requestBuilder.execute[HttpResponse](any, any))
        .thenReturn(Future.failed(new TimeoutException("timeout error")))

      val result = app.injector.instanceOf[MessageConnector].sendMessage(JsString("")).futureValue
      result.status mustBe Status.BAD_GATEWAY
      result.body must include("timeout error")
    }
  }

  trait TestCase {

    val expectedGetAllowlistPath = s"/admin/message/brake/gmc/allowlist"
    val expectedAddFormIdToAllowlistPath = s"/admin/message/brake/gmc/allowlist/add"
    val expectedDeleteFormIdFromAllowlistPath = s"/admin/message/brake/gmc/allowlist/delete"
    val expectedGetGmcBatchesPath = s"/admin/message/brake/gmc/batches"
    val expectedGetRandomMessagePreviewPath = s"/admin/message/brake/random"
    val expectedApproveGmcBatchPath = s"/admin/message/brake/accept"
    val expectedRejectGmcBatchPath = s"/admin/message/brake/reject"
    val emptyJson = Json.obj()
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
      """.stripMargin
    )

    val gmcBatch = GmcBatch(
      "123456789",
      "SA359",
      "2017-03-16",
      "newMessageAlert_SA359",
      Some(15778),
      None
    )

    val gmcBatchV4 = GmcBatch(
      "123456789",
      "SA359",
      "2017-03-16",
      "newMessageAlert_SA359",
      Some(15778),
      Some("v4")
    )

    val gmcBatchApproval = GmcBatchApproval(
      "123456789",
      "SA359",
      "2017-03-16",
      "newMessageAlert_SA359",
      "some reason",
      None
    )

    val gmcBatchApprovalV4 = GmcBatchApproval(
      "123456789",
      "SA359",
      "2017-03-16",
      "newMessageAlert_SA359",
      "some reason",
      Some("v4")
    )

    lazy val mockServicesConfig: ServicesConfig = mock[ServicesConfig]

    def messageConnectorHttpMock(expectedPath: String): MessageConnector = {
      val mockHttp: HttpClientV2 = mock[HttpClientV2]
      val requestBuilder: RequestBuilder = mock[RequestBuilder]

      when(mockHttp.get(any)(any)).thenReturn(requestBuilder)
      when(requestBuilder.execute[HttpResponse](any, any))
        .thenReturn(Future.successful(HttpResponse(Status.OK, Json.obj(), Map.empty)))

      when(mockHttp.post(any)(any)).thenReturn(requestBuilder)
      when(requestBuilder.withBody(any)(any, any, any)).thenReturn(requestBuilder)
      when(requestBuilder.execute[HttpResponse](any, any))
        .thenReturn(Future.successful(HttpResponse(Status.OK, Json.obj(), Map.empty)))

      new MessageConnector(mockHttp, mockServicesConfig)
    }

    def messageConnectorHttpMock(expectedPath: String, error: Throwable): MessageConnector = {
      val mockHttp: HttpClientV2 = mock[HttpClientV2]

      when(mockHttp.get(any)(any)).thenReturn(requestBuilder)
      when(requestBuilder.execute[HttpResponse](any, any)).thenReturn(Future.failed(error))

      when(mockHttp.post(any)(any)).thenReturn(requestBuilder)
      when(requestBuilder.withBody(any)(any, any, any)).thenReturn(requestBuilder)
      when(requestBuilder.execute[HttpResponse](any, any))
        .thenReturn(Future.failed(error))

      new MessageConnector(mockHttp, mockServicesConfig)
    }
  }

}
