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

import org.mockito.Mockito.verify
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.mvc.{ AnyContent, Request, Result }
import uk.gov.hmrc.preferencesadminfrontend.controllers.model.User

import scala.concurrent.Future

class RoleAuthorisedActionSpec extends PlaySpec with MockitoSugar with ScalaFutures {

  val mockAuthorisedAction = mock[AuthorisedAction]

  class TestAction extends RoleAuthorisedAction(mockAuthorisedAction) {
    override def role: Role = Role.Admin
  }

  "authorisedAction" must {
    "delegate to authorisedActionService with the correct role" in {
      val action = new TestAction
      val block: Request[AnyContent] => User => Future[Result] = _ => _ => Future.successful(mock[Result])

      action.authorisedAction(block)

      verify(mockAuthorisedAction).async(Role.Admin)(block)
    }
  }

  "Role.fromString" must {
    "return Admin for 'admin'" in {
      Role.fromString("admin") mustBe Role.Admin
    }

    "return Generic for 'generic'" in {
      Role.fromString("generic") mustBe Role.Generic
    }

    "throw IllegalArgumentException for invalid roles" in {
      val ex = intercept[IllegalArgumentException](Role.fromString("invalid"))
      ex.getMessage mustBe "Invalid argument for the role invalid"
    }
  }
}
