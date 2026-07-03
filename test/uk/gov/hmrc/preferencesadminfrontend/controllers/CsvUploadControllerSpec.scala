/*
 * Copyright 2026 HM Revenue & Customs
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
import org.apache.pekko.stream.scaladsl.Framing.FramingException
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.*
import org.mockito.Mockito.*
import org.scalactic.source.Position
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import org.scalatestplus.play.{ HtmlUnitFactory, PlaySpec }
import play.api.http.*
import play.api.i18n.MessagesApi
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.Files.TemporaryFile
import play.api.mvc.MultipartFormData.FilePart
import play.api.mvc.{ MultipartFormData, Result }
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import play.api.{ Application, inject }
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.preferencesadminfrontend.connectors.{ AlreadyOptedOut, OptedOut, PreferenceNotFound }
import uk.gov.hmrc.preferencesadminfrontend.controllers.model.User
import uk.gov.hmrc.preferencesadminfrontend.services.*
import uk.gov.hmrc.preferencesadminfrontend.utils.SpecBase

import java.nio.file.Path
import scala.concurrent.{ ExecutionContext, Future }

class CsvUploadControllerSpec
    extends PlaySpec with GuiceOneAppPerSuite with SpecBase with ScalaFutures with MockitoSugar with HtmlUnitFactory {

  implicit lazy val materializer: Materializer = app.materializer
  override implicit lazy val app: Application = GuiceApplicationBuilder().build()
  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  "showUploadPage (GET /csv-upload)" should {
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

  "upload (POST /csv-upload/confirmation)" should {
    "return 400 when form binding fails" in new TestCase {
      val request = FakeRequest("POST", "/csv-upload/confirmation")
        .withSession(User.sessionKey -> "admin")

      val result = controller.upload()(request)

      status(result) mustBe BAD_REQUEST
    }

    "return 200 when service returns nil" in new TestCase {
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

      when(mockUploadService.process(any())(any(), any(), any()))
        .thenReturn(Future.successful("success"))

      val request = FakeRequest("POST", "/multi-search/results")
        .withSession(User.sessionKey -> "admin")
        .withBody(formData)

      val result = controller.upload()(request)

      status(result) mustBe OK
    }
  }

  "showBulkOptOutsUploadPage (GET /csv-upload-bulk-opt-outs)" should {
    "return HTML" in new TestCase {
      val result: Future[Result] =
        controller.showBulkOptOutsUploadPage()(FakeRequest("", "").withSession(User.sessionKey -> "admin"))

      contentType(result) mustBe Some("text/html")
      charset(result) mustBe Some("utf-8")
      status(result) mustBe OK

      extractBulkOptOutErrors(contentAsString(result)) mustBe List.empty
    }

    "redirect to login page for non-admin user" in new TestCase {
      val result: Future[Result] =
        controller.showBulkOptOutsUploadPage()(FakeRequest("GET", "/decode"))
      status(result) mustBe Status.SEE_OTHER

    }
  }

  def extractBulkOptOutErrors(body: String): List[String] = {
    val possibleErrorMessages = List(
      "The following uploaded entries were not fully opted in",
      "The following uploaded entries were not found",
      "The following uploaded entries were invalid",
      "The following entries failed for unexpected reasons",
      "The uploaded file had no entries",
      "Too many entries were uploaded",
      "Invalid File Format. The uploaded file format could not be processed. Please enter a valid .csv file format."
    )

    possibleErrorMessages.collect {
      case possibleError if body.contains(possibleError) =>
        possibleError
    }
  }

  "uploadBulkOptOuts (POST /csv-upload-bulk-opt-outs/confirmation)" should {
    def filePart: FilePart[TemporaryFile] = FilePart(
      key = "csvFile",
      filename = "test.csv",
      contentType = Some("text/csv"),
      ref = mock[TemporaryFile]
    )

    "return 400 when form binding fails" in new TestCase {
      val request = FakeRequest("", "")
        .withSession(User.sessionKey -> "admin")

      val result = controller.uploadBulkOptOuts()(request)

      status(result) mustBe BAD_REQUEST
    }

    "return 200 but display a message saying the file could not be processed when it fails processing" in new TestCase {
      val mockTemporaryFile = mock[TemporaryFile]
      val mockPath = mock[Path]

      when(mockTemporaryFile.path).thenReturn(mockPath)

      when(mockBulkOptOutsService.readNinoBulkOptOutsFromFile(any())(any()))
        .thenReturn(
          Future.successful(
            Left(
              new FramingException("failed reading file")
            )
          )
        )

      val formData = MultipartFormData[TemporaryFile](
        dataParts = Map.empty,
        files = Seq(filePart),
        badParts = Seq.empty
      )

      val request = FakeRequest("", "")
        .withSession(User.sessionKey -> "admin")
        .withBody(formData)

      val result = controller.uploadBulkOptOuts()(request)

      status(result) mustBe OK
      val body: String = contentAsString(result)

      extractBulkOptOutErrors(body) mustBe List(
        "Invalid File Format. The uploaded file format could not be processed. Please enter a valid .csv file format."
      )
    }

    "return 200 but display errors when some could not be validated" in new TestCase {
      val mockTemporaryFile = mock[TemporaryFile]
      val mockPath = mock[Path]

      when(mockTemporaryFile.path).thenReturn(mockPath)

      when(mockBulkOptOutsService.readNinoBulkOptOutsFromFile(any())(any()))
        .thenReturn(
          Future.successful(
            Right(
              List(
                Right("nino1"),
                Left("error1,sss"),
                Right("nino2"),
                Left("error2,sss")
              )
            )
          )
        )

      val formData = MultipartFormData[TemporaryFile](
        dataParts = Map.empty,
        files = Seq(filePart),
        badParts = Seq.empty
      )

      val request = FakeRequest("", "")
        .withSession(User.sessionKey -> "admin")
        .withBody(formData)

      val result = controller.uploadBulkOptOuts()(request)

      status(result) mustBe OK
      val body: String = contentAsString(result)

      extractBulkOptOutErrors(body) mustBe List(
        "The following uploaded entries were invalid"
      )

      body must include("error1,sss")
      body must include("error2,sss")

      body must not include "nino1"
      body must not include "nino2"

    }

    "return 200 but display no entries were found if file was empty" in new TestCase {
      val mockTemporaryFile = mock[TemporaryFile]
      val mockPath = mock[Path]

      when(mockTemporaryFile.path).thenReturn(mockPath)

      when(mockBulkOptOutsService.readNinoBulkOptOutsFromFile(any())(any()))
        .thenReturn(
          Future.successful(Right(List.empty))
        )

      val formData = MultipartFormData[TemporaryFile](
        dataParts = Map.empty,
        files = Seq(filePart),
        badParts = Seq.empty
      )

      val request = FakeRequest("", "")
        .withSession(User.sessionKey -> "admin")
        .withBody(formData)

      val result = controller.uploadBulkOptOuts()(request)

      status(result) mustBe OK
      val body: String = contentAsString(result)

      extractBulkOptOutErrors(body) mustBe List(
        "The uploaded file had no entries"
      )
    }

    "return 200 but display too many entries if the limit is exceeded" in new TestCase {
      val mockTemporaryFile = mock[TemporaryFile]
      val mockPath = mock[Path]

      val ninos = (1 to 101).map { index =>
        s"nino-$index"
      }.toList

      when(mockTemporaryFile.path).thenReturn(mockPath)

      when(mockBulkOptOutsService.readNinoBulkOptOutsFromFile(any())(any()))
        .thenReturn(
          Future.successful(Right(ninos.map(Right.apply)))
        )

      val formData = MultipartFormData[TemporaryFile](
        dataParts = Map.empty,
        files = Seq(filePart),
        badParts = Seq.empty
      )

      val request = FakeRequest("", "")
        .withSession(User.sessionKey -> "admin")
        .withBody(formData)

      val result = controller.uploadBulkOptOuts()(request)

      status(result) mustBe OK
      val body: String = contentAsString(result)

      extractBulkOptOutErrors(body) mustBe List(
        "Too many entries were uploaded"
      )
    }

    def testValidParsing(
      controller: CsvUploadController,
      mockBulkOptOutsService: BulkUploadOptOutsService,
      ninos: List[String],
      serverResponse: List[BulkOptOutResult],
      expectedErrors: List[String],
      successCount: Int
    )(implicit position: Position): Unit = {
      val mockTemporaryFile = mock[TemporaryFile]
      val mockPath = mock[Path]

      when(mockTemporaryFile.path).thenReturn(mockPath)

      val formData = MultipartFormData[TemporaryFile](
        dataParts = Map.empty,
        files = Seq(filePart),
        badParts = Seq.empty
      )

      when(mockBulkOptOutsService.readNinoBulkOptOutsFromFile(any())(any()))
        .thenReturn(
          Future.successful(
            Right(ninos.map(Right.apply))
          )
        )

      when(mockBulkOptOutsService.processBulkOptOuts(ArgumentMatchers.eq(ninos))(any(), any(), any()))
        .thenReturn(
          Future.successful(
            serverResponse
          )
        )

      val request = FakeRequest("", "")
        .withSession(User.sessionKey -> "admin")
        .withBody(formData)

      val result = controller.uploadBulkOptOuts()(request)

      status(result) mustBe OK
      val body: String = contentAsString(result)

      extractBulkOptOutErrors(body) mustBe expectedErrors
      body must include(s"Successfully opted out $successCount records")
    }

    "return 200 when only valid entries that were at maximum upload count were parsed and processed successfully as opted out" in new TestCase {
      val ninos = (1 to 100).map { index =>
        s"nino-$index"
      }.toList

      testValidParsing(
        controller = controller,
        mockBulkOptOutsService = mockBulkOptOutsService,
        ninos = ninos,
        serverResponse = ninos.map(nino => ProcessedBulkOptOutResult(nino, OptedOut)),
        expectedErrors = List.empty,
        successCount = ninos.size
      )
    }

    "return 200 when only valid entries were parsed but were already opted out" in new TestCase {
      val ninos = List(
        "nino1",
        "nino2"
      )

      testValidParsing(
        controller = controller,
        mockBulkOptOutsService = mockBulkOptOutsService,
        ninos = ninos,
        serverResponse = List(
          ProcessedBulkOptOutResult("nino1", AlreadyOptedOut),
          ProcessedBulkOptOutResult("nino2", AlreadyOptedOut)
        ),
        expectedErrors = List("The following uploaded entries were not fully opted in"),
        successCount = 0
      )
    }

    "return 200 when only valid entries were parsed but were not found" in new TestCase {
      val ninos = List(
        "nino1",
        "nino2"
      )

      testValidParsing(
        controller = controller,
        mockBulkOptOutsService = mockBulkOptOutsService,
        ninos = ninos,
        serverResponse = List(
          ProcessedBulkOptOutResult("nino1", PreferenceNotFound),
          ProcessedBulkOptOutResult("nino2", PreferenceNotFound)
        ),
        expectedErrors = List("The following uploaded entries were not found"),
        successCount = 0
      )
    }

    "return 200 when only valid entries were parsed but failed for unknown reasons" in new TestCase {
      val ninos = List(
        "nino1",
        "nino2"
      )

      testValidParsing(
        controller = controller,
        mockBulkOptOutsService = mockBulkOptOutsService,
        ninos = ninos,
        serverResponse = List(
          FailedCallBulkOptOutResult("nino1"),
          FailedCallBulkOptOutResult("nino2")
        ),
        expectedErrors = List("The following entries failed for unexpected reasons"),
        successCount = 0
      )
    }

    "return 200 and display all combinations of reponses" in new TestCase {
      val ninos = List(
        "nino1",
        "nino2",
        "nino3",
        "nino4",
        "nino5",
        "nino6",
        "nino7",
        "nino8"
      )

      testValidParsing(
        controller = controller,
        mockBulkOptOutsService = mockBulkOptOutsService,
        ninos = ninos,
        serverResponse = List(
          ProcessedBulkOptOutResult("nino1", PreferenceNotFound),
          ProcessedBulkOptOutResult("nino2", OptedOut),
          FailedCallBulkOptOutResult("nino3"),
          FailedCallBulkOptOutResult("nino4"),
          ProcessedBulkOptOutResult("nino5", AlreadyOptedOut),
          ProcessedBulkOptOutResult("nino6", AlreadyOptedOut),
          ProcessedBulkOptOutResult("nino7", OptedOut),
          ProcessedBulkOptOutResult("nino8", OptedOut)
        ),
        expectedErrors = List(
          "The following uploaded entries were not fully opted in",
          "The following uploaded entries were not found",
          "The following entries failed for unexpected reasons"
        ),
        successCount = 3
      )
    }

  }

  class TestCase {
    lazy val mockUploadService: UploadService = mock[UploadService]
    lazy val mockBulkOptOutsService: BulkUploadOptOutsService = mock[BulkUploadOptOutsService]

    implicit lazy val app: Application = GuiceApplicationBuilder()
      .overrides(inject.bind[UploadService].toInstance(mockUploadService))
      .overrides(inject.bind[BulkUploadOptOutsService].toInstance(mockBulkOptOutsService))
      .build()

    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit lazy val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]
    implicit lazy val materializer: Materializer = app.materializer
    implicit lazy val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

    lazy val controller: CsvUploadController = app.injector.instanceOf[CsvUploadController]
  }

}
