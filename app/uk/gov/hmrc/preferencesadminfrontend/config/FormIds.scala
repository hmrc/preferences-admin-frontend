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

package uk.gov.hmrc.preferencesadminfrontend.config

//Added config list of valid formIds, since the environment configs doesn't support whitespace within the hmrc_config element values
object FormIds {
  val configList = Seq(
    "SA300",
    "SS300",
    "SA251",
    "SA359",
    "SA316",
    "SA316 2012",
    "SA316 2013",
    "SA316 2014",
    "SA316 2015",
    "SA316 2016",
    "SA316 2017",
    "SA316 2018",
    "SA316 2019",
    "SA316 2020",
    "SA316 2021",
    "SA316 2022",
    "SA316 2023",
    "SA326D",
    "SA328D",
    "SA370",
    "SA371",
    "SA372",
    "SA37230",
    "SA37260",
    "SA373",
    "SA37330",
    "SA37360",
    "R002",
    "R002A",
    "ATSV2",
    "P800 2011",
    "P800 2012",
    "P800 2013",
    "P800 2014",
    "P800 2015",
    "P800 2016",
    "P800 2017",
    "P800 2018",
    "P800 2019",
    "P800 2020",
    "P800 2021",
    "P800 2022",
    "P800 2023",
    "P800 2024",
    "P800 2025",
    "PA302",
    "PA302 2011",
    "PA302 2012",
    "PA302 2013",
    "PA302 2014",
    "PA302 2015",
    "PA302 2016",
    "PA302 2017",
    "PA302 2018",
    "PA302 2019",
    "PA302 2020",
    "PA302 2021",
    "PA302 2022",
    "PA302 2023",
    "ITSAQU1",
    "ITSAQU2",
    "ITSAEOPS1",
    "ITSAEOPS2",
    "ITSAEOPSF",
    "ITSAPOA1-1",
    "ITSAPOA1-2",
    "ITSAPOA2-1",
    "ITSAPOA2-2",
    "ITSAFD1",
    "ITSAFD2",
    "ITSAFD3",
    "ITSAPOA-CN",
    "ITSAUC1",
    "LPI1",
    "LPI1_CY",
    "LPP4",
    "LPP4_CY",
    "M01iOSS",
    "M01aiOSS",
    "M02aiOSS",
    "M02iOSS",
    "M04iOSS",
    "M05iOSS",
    "M05aiOSS",
    "M07aiOSS",
    "M06iOSS",
    "M06aiOSS",
    "M07iOSS",
    "M08aiOSS",
    "M08iOSS"
  )
}
