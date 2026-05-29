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

package uk.gov.hmrc.preferencesadminfrontend.controllers

import org.apache.pekko.stream.Materializer
import org.mockito.ArgumentMatchers.*
import org.mockito.Mockito.*
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.*
import play.api.i18n.MessagesApi
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.Files.TemporaryFile
import play.api.mvc.MultipartFormData.FilePart
import play.api.mvc.Results.Ok
import play.api.mvc.{ MultipartFormData, Result }
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import play.api.{ Application, inject }
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.preferencesadminfrontend.controllers.model.User
import uk.gov.hmrc.preferencesadminfrontend.services.UploadService
import uk.gov.hmrc.preferencesadminfrontend.utils.SpecBase

import java.nio.file.Path
import scala.concurrent.{ ExecutionContext, Future }

class CsvUploadControllerSpec
    extends PlaySpec with GuiceOneAppPerSuite with SpecBase with ScalaFutures with MockitoSugar {

  implicit lazy val materializer: Materializer = app.materializer
  override implicit lazy val app: Application = GuiceApplicationBuilder().build()
  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  "GET /csv-upload" should {
    "return 200" in new TestCase {
      val result: Future[Result] =
        controller.showUploadPage()(FakeRequest("GET", "/decode").withSession(User.sessionKey -> "admin"))
      status(result) mustBe Status.OK
    }

    "return HTML" in new TestCase {
      val result: Future[Result] =
        controller.showUploadPage()(FakeRequest("GET", "/decode").withSession(User.sessionKey -> "admin"))
      contentType(result) mustBe Some("text/html")
      charset(result) mustBe Some("utf-8")
    }

    "redirect to login page for non-admin user" in new TestCase {
      val result: Future[Result] =
        controller.showUploadPage()(FakeRequest("GET", "/decode").withSession(User.sessionKey -> "user"))
      status(result) mustBe Status.SEE_OTHER
    }
  }

  "POST /csv-upload/confirmation" should {
    "return 400 when form binding fails" in new TestCase {
      val request = FakeRequest("POST", "/csv-upload/confirmation")
        .withSession(User.sessionKey -> "admin")

      val result = controller.upload()(request)

      status(result) mustBe BAD_REQUEST
    }

    "return 200 when service returns nil " in new TestCase {
      val mockTemporaryFile = mock[TemporaryFile]
      val mockPath = mock[Path]

      when(mockTemporaryFile.path).thenReturn(mockPath)

      val filePart = FilePart(
        key = "csvFile",
        filename = "test.csv",
        contentType = Some("text/csv"),
        ref = mockTemporaryFile
      )

      val formData = MultipartFormData[TemporaryFile](
        dataParts = Map.empty,
        files = Seq(filePart),
        badParts = Seq.empty
      )
      when(mockUploadService.readFromFile(any())(any()))
        .thenReturn(Future.successful(Nil))
      when(mockUploadService.process(any())(any()))
        .thenReturn(Future.successful(Ok("success")))

      val request = FakeRequest("POST", "/multi-search/results")
        .withSession(User.sessionKey -> "admin")
        .withBody(formData)

      val result = controller.upload()(request)

      status(result) mustBe OK
    }
  }

  class TestCase {
    lazy val mockUploadService: UploadService = mock[UploadService]

    implicit lazy val app: Application = GuiceApplicationBuilder()
      .overrides(inject.bind[UploadService].toInstance(mockUploadService))
      .build()

    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit lazy val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]
    implicit lazy val materializer: Materializer = app.materializer
    implicit lazy val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

    lazy val controller: CsvUploadController = app.injector.instanceOf[CsvUploadController]
  }
}
