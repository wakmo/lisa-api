/*
 * Copyright 2018 HM Revenue & Customs
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

package uk.gov.hmrc.lisaapi.services

import play.api.Logger
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.lisaapi.connectors.DesConnector
import uk.gov.hmrc.lisaapi.models._
import uk.gov.hmrc.lisaapi.models.des.DesFailureResponse

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait InvestorService  {
  val desConnector: DesConnector


  def createInvestor(lisaManager: String, request: CreateLisaInvestorRequest)(implicit hc: HeaderCarrier) : Future[CreateLisaInvestorResponse] = {
    val response = desConnector.createInvestor(lisaManager, request)

    response map {
      case successResponse: CreateLisaInvestorSuccessResponse => successResponse
      case existsResponse: CreateLisaInvestorAlreadyExistsResponse => existsResponse
      case error: DesFailureResponse => {
        error.code match {
          case "INVESTOR_NOT_FOUND" => CreateLisaInvestorInvestorNotFoundResponse
          case _ => {
            Logger.warn(s"Create investor returned error code ${error.code}")
            CreateLisaInvestorErrorResponse
          }
        }
      }
    }
  }

}

object InvestorService extends InvestorService {
  override val desConnector: DesConnector = DesConnector
}