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

package uk.gov.hmrc.lisaapi.connectors

import uk.gov.hmrc.lisaapi.config.WSHttp
import uk.gov.hmrc.lisaapi.controllers.JsonFormats
import uk.gov.hmrc.lisaapi.models.CreateLisaInvestorRequest
import uk.gov.hmrc.lisaapi.models.des.DesCreateInvestorResponse
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpPost, HttpResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

trait DesConnector extends ServicesConfig with JsonFormats {

  val httpPost:HttpPost = WSHttp
  lazy val desUrl = baseUrl("des")
  lazy val lisaServiceUrl = s"$desUrl/lifetime-isa/manager"

  /**
    * Attempts to create a new LISA investor
    *
    * @return A tuple of the http status code and an (optional) data response
    */
  def createInvestor(lisaManager: String, request: CreateLisaInvestorRequest)(implicit hc: HeaderCarrier): Future[(Int, Option[DesCreateInvestorResponse])] = {
    val uri = s"$lisaServiceUrl/$lisaManager/investors"

    val result = httpPost.POST[CreateLisaInvestorRequest, HttpResponse](uri, request)

    result.map(r => {
      // catch any NullPointerExceptions that may occur from r.json being a null
      Try(r.json.asOpt[DesCreateInvestorResponse]) match {
        case Success(data) => (r.status, data)
        case Failure(_) => (r.status, None)
      }
    })
  }

}


object DesConnector extends DesConnector {

}