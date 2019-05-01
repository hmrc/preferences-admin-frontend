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

package uk.gov.hmrc.preferencesadminfrontend


import org.scalatest.mockito.MockitoSugar
import play.api.{Configuration, Environment}
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.preferencesadminfrontend.controllers.model.User
import uk.gov.hmrc.preferencesadminfrontend.services.{LoginService, LoginServiceConfiguration}

class LoginServiceSpec extends UnitSpec with MockitoSugar {

  "login" should {
    "allow an authorised user into the system" in new TestCase {
      lazy val loginService = new LoginService(loginServiceConfiguration)
      val user = new User("username", "password")

      loginService.isAuthorised(user) shouldBe true
    }

    "not allow an user which is not included" in new TestCase {
      lazy val loginService = new LoginService(loginServiceConfiguration)
      val user = new User("anotherUser", "password")

      loginService.isAuthorised(user) shouldBe false
    }

    "not allow if the password is wrong" in new TestCase {
      lazy val loginService = new LoginService(loginServiceConfiguration)
      val user = new User("username", "wrongPassword")

      loginService.isAuthorised(user) shouldBe false
    }
  }

  trait TestCase {
    val loginServiceConfiguration = new LoginServiceConfiguration(mock[Configuration], mock[Environment]){
      override lazy val authorisedUsers: Seq[User] = Seq(User("username", "password"))
    }
  }
}