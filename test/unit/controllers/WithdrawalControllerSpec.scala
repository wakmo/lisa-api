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

package unit.controllers

import org.joda.time.DateTime
import org.mockito.Matchers.any
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsJson, Result}
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.lisaapi.LisaConstants
import uk.gov.hmrc.lisaapi.config.LisaAuthConnector
import uk.gov.hmrc.lisaapi.controllers.{ErrorAccountAlreadyCancelled, ErrorAccountAlreadyVoided, ErrorAccountNotFound, ErrorInternalServerError, ErrorValidation, ErrorWithdrawalExists, ErrorWithdrawalNotFound, ErrorWithdrawalTimescalesExceeded, WithdrawalController}
import uk.gov.hmrc.lisaapi.models._
import uk.gov.hmrc.lisaapi.services.{AuditService, BonusOrWithdrawalService, CurrentDateService, WithdrawalService}
import uk.gov.hmrc.lisaapi.utils.WithdrawalChargeValidator

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.io.Source

class WithdrawalControllerSpec extends PlaySpec
  with MockitoSugar
  with OneAppPerSuite
  with BeforeAndAfterEach
  with LisaConstants {

  val acceptHeader: (String, String) = (HeaderNames.ACCEPT, "application/vnd.hmrc.1.0+json")
  val lisaManager = "Z019283"
  val accountId = "ABC/12345"
  val transactionId = "1234567890"
  val validWithdrawalJson = Source.fromInputStream(getClass().getResourceAsStream("/json/request.valid.withdrawal-charge.json")).mkString
  implicit val hc:HeaderCarrier = HeaderCarrier()

  override def beforeEach() {
    reset(mockAuditService)
    reset(mockDateTimeService)
    reset(mockValidator)

    when(mockAuthCon.authorise[Option[String]](any(),any())(any(), any())).thenReturn(Future(Some("1234")))
    when(mockDateTimeService.now()).thenReturn(new DateTime("2018-01-01"))
    when(mockValidator.validate(any())).thenReturn(Nil)
  }

  "the POST withdrawal charge endpoint" must {

    "return with status 201 created" when {

      "given a ReportWithdrawalChargeOnTimeResponse from the service layer" in {
        when(mockPostService.reportWithdrawalCharge(any(), any(), any())(any())).
          thenReturn(Future.successful(ReportWithdrawalChargeOnTimeResponse("1928374")))

        doRequest(validWithdrawalJson) { res =>
          status(res) mustBe CREATED
          (contentAsJson(res) \ "data" \ "transactionId").as[String] mustBe "1928374"
          (contentAsJson(res) \ "data" \ "message").as[String] mustBe "Unauthorised withdrawal transaction created"
        }
      }

      "given a ReportWithdrawalChargeLateResponse from the service layer" in {
        when(mockPostService.reportWithdrawalCharge(any(), any(), any())(any())).
          thenReturn(Future.successful(ReportWithdrawalChargeLateResponse("1928374")))

        doRequest(validWithdrawalJson) { res =>
          status(res) mustBe CREATED
          (contentAsJson(res) \ "data" \ "transactionId").as[String] mustBe "1928374"
          (contentAsJson(res) \ "data" \ "message").as[String] mustBe "Unauthorised withdrawal transaction created - late notification"
        }
      }

      "given a ReportWithdrawalChargeSupersededResponse from the service layer" in {
        when(mockPostService.reportWithdrawalCharge(any(), any(), any())(any())).
          thenReturn(Future.successful(ReportWithdrawalChargeSupersededResponse("1928374")))

        doRequest(validWithdrawalJson) { res =>
          status(res) mustBe CREATED
          (contentAsJson(res) \ "data" \ "transactionId").as[String] mustBe "1928374"
          (contentAsJson(res) \ "data" \ "message").as[String] mustBe "Unauthorised withdrawal transaction superseded"
        }
      }

    }

    "return with status 403 forbidden" when {

      "given a ReportWithdrawalChargeAccountClosed from the service layer" in {
        when(mockPostService.reportWithdrawalCharge(any(), any(), any())(any())).
          thenReturn(Future.successful(ReportWithdrawalChargeAccountCancelled))

        doRequest(validWithdrawalJson) { res =>
          status(res) mustBe FORBIDDEN
          (contentAsJson(res) \ "code").as[String] mustBe ErrorAccountAlreadyCancelled.errorCode
          (contentAsJson(res) \ "message").as[String] mustBe ErrorAccountAlreadyCancelled.message
        }
      }

      "given a ReportWithdrawalChargeAccountVoid from the service layer" in {
        when(mockPostService.reportWithdrawalCharge(any(), any(), any())(any())).
          thenReturn(Future.successful(ReportWithdrawalChargeAccountVoid))

        doRequest(validWithdrawalJson) { res =>
          status(res) mustBe FORBIDDEN
          (contentAsJson(res) \ "code").as[String] mustBe ErrorAccountAlreadyVoided.errorCode
          (contentAsJson(res) \ "message").as[String] mustBe ErrorAccountAlreadyVoided.message
        }
      }

      "given a ReportWithdrawalChargeAccountCancelled from the service layer" in {
        when(mockPostService.reportWithdrawalCharge(any(), any(), any())(any())).
          thenReturn(Future.successful(ReportWithdrawalChargeAccountCancelled))

        doRequest(validWithdrawalJson) { res =>
          status(res) mustBe FORBIDDEN
          (contentAsJson(res) \ "code").as[String] mustBe ErrorAccountAlreadyCancelled.errorCode
          (contentAsJson(res) \ "message").as[String] mustBe ErrorAccountAlreadyCancelled.message
        }
      }

      "given a ReportWithdrawalChargeReportingError from the service layer" in {
        when(mockPostService.reportWithdrawalCharge(any(), any(), any())(any())).
          thenReturn(Future.successful(ReportWithdrawalChargeReportingError))

        doRequest(validWithdrawalJson) { res =>
          status(res) mustBe FORBIDDEN
          (contentAsJson(res) \ "code").as[String] mustBe "WITHDRAWAL_REPORTING_ERROR"
          (contentAsJson(res) \ "message").as[String] mustBe "The withdrawal charge does not equal 25% of the withdrawal amount"
        }
      }

      "given a ReportWithdrawalChargeAlreadySuperseded from the service layer" in {
        when(mockPostService.reportWithdrawalCharge(any(), any(), any())(any())).
          thenReturn(Future.successful(ReportWithdrawalChargeAlreadySuperseded))

        doRequest(validWithdrawalJson) { res =>
          status(res) mustBe FORBIDDEN
          (contentAsJson(res) \ "code").as[String] mustBe "WITHDRAWAL_CHARGE_ALREADY_SUPERSEDED"
          (contentAsJson(res) \ "message").as[String] mustBe "This withdrawal charge has already been superseded"
        }
      }

      "given a ReportWithdrawalChargeSupersedeAmountMismatch from the service layer" in {
        when(mockPostService.reportWithdrawalCharge(any(), any(), any())(any())).
          thenReturn(Future.successful(ReportWithdrawalChargeSupersedeAmountMismatch))

        doRequest(validWithdrawalJson) { res =>
          status(res) mustBe FORBIDDEN
          (contentAsJson(res) \ "code").as[String] mustBe "SUPERSEDED_WITHDRAWAL_CHARGE_ID_AMOUNT_MISMATCH"
          (contentAsJson(res) \ "message").as[String] mustBe "originalTransactionId and the originalWithdrawalChargeAmount do not match the information in the original request"
        }
      }

      "given a ReportWithdrawalChargeSupersedeOutcomeError from the service layer" in {
        when(mockPostService.reportWithdrawalCharge(any(), any(), any())(any())).
          thenReturn(Future.successful(ReportWithdrawalChargeSupersedeOutcomeError))

        doRequest(validWithdrawalJson) { res =>
          status(res) mustBe FORBIDDEN
          (contentAsJson(res) \ "code").as[String] mustBe "SUPERSEDED_WITHDRAWAL_CHARGE_OUTCOME_ERROR"
          (contentAsJson(res) \ "message").as[String] mustBe "The calculation from your superseded withdrawal charge is incorrect"
        }
      }

      "the claimPeriodEndDate is more than 6 years and 14 days in the past" in {
        val now = new DateTime("2050-01-20")

        when(mockDateTimeService.now()).thenReturn(now)

        val testEndDate = now.minusYears(6).withDayOfMonth(5)
        val testStartDate = testEndDate.minusMonths(1).plusDays(1)

        val validWithdrawalCharge = Json.parse(validWithdrawalJson).as[SupersededWithdrawalChargeRequest]
        val request = validWithdrawalCharge.copy(claimPeriodStartDate = testStartDate, claimPeriodEndDate = testEndDate)
        val requestJson = Json.toJson(request).toString()

        doRequest(requestJson) { res =>
          status(res) mustBe FORBIDDEN

          val json = contentAsJson(res)

          (json \ "code").as[String] mustBe ErrorWithdrawalTimescalesExceeded.errorCode
          (json \ "message").as[String] mustBe ErrorWithdrawalTimescalesExceeded.message
        }
      }

      "the json request fails business validation" in {
        val errors = List(
          ErrorValidation(
            DATE_ERROR,
            "The claimPeriodStartDate must be the 6th day of the month",
            Some("/claimPeriodStartDate")
          ),
          ErrorValidation(
            DATE_ERROR,
            "The claimPeriodEndDate must be the 5th day of the month which occurs after the claimPeriodStartDate",
            Some("/claimPeriodEndDate")
          )
        )

        when(mockValidator.validate(any())).thenReturn(errors)

        val validWithdrawalCharge = Json.parse(validWithdrawalJson).as[SupersededWithdrawalChargeRequest]
        val requestJson = Json.toJson(validWithdrawalCharge).toString()

        doRequest(requestJson) { res =>
          status(res) mustBe FORBIDDEN

          val json = contentAsJson(res)

          (json \ "code").as[String] mustBe "FORBIDDEN"

          (json \ "errors" \ 0 \ "path").as[String] mustBe "/claimPeriodStartDate"
          (json \ "errors" \ 1 \ "path").as[String] mustBe "/claimPeriodEndDate"
        }
      }

    }

    "return with status 404 not found" when {

      "given a ReportWithdrawalChargeAccountNotFound from the service layer" in {
        when(mockPostService.reportWithdrawalCharge(any(), any(), any())(any())).
          thenReturn(Future.successful(ReportWithdrawalChargeAccountNotFound))

        doRequest(validWithdrawalJson) { res =>
          status(res) mustBe NOT_FOUND
          (contentAsJson(res) \ "code").as[String] mustBe ErrorAccountNotFound.errorCode
          (contentAsJson(res) \ "message").as[String] mustBe ErrorAccountNotFound.message
        }
      }

    }

    "return with status 409 conflict" when {

      "given a ReportWithdrawalChargeAlreadyExists from the service layer" in {
        when(mockPostService.reportWithdrawalCharge(any(), any(), any())(any())).
          thenReturn(Future.successful(ReportWithdrawalChargeAlreadyExists))

        doRequest(validWithdrawalJson) { res =>
          status(res) mustBe CONFLICT
          (contentAsJson(res) \ "code").as[String] mustBe ErrorWithdrawalExists.errorCode
          (contentAsJson(res) \ "message").as[String] mustBe ErrorWithdrawalExists.message
        }
      }

    }

    "return with status 500 internal server error" when {

      "an exception gets thrown" in {
        when(mockPostService.reportWithdrawalCharge(any(), any(), any())(any())).
          thenThrow(new RuntimeException("Test"))

        doRequest(validWithdrawalJson) { res =>
          reset(mockPostService) // removes the thenThrow

          status(res) mustBe INTERNAL_SERVER_ERROR
          (contentAsJson(res) \ "code").as[String] mustBe ErrorInternalServerError.errorCode
          (contentAsJson(res) \ "message").as[String] mustBe ErrorInternalServerError.message
        }
      }

      "given a RequestWithdrawalChargeError from the service layer" in {
        when(mockPostService.reportWithdrawalCharge(any(), any(), any())(any())).
          thenReturn(Future.successful(ReportWithdrawalChargeError))

        doRequest(validWithdrawalJson) { res =>
          status(res) mustBe INTERNAL_SERVER_ERROR
          (contentAsJson(res) \ "code").as[String] mustBe ErrorInternalServerError.errorCode
          (contentAsJson(res) \ "message").as[String] mustBe ErrorInternalServerError.message
        }
      }

    }

  }

  "the GET withdrawal charge endpoint" must {

    "return 200 success response" in {
      when(mockGetService.getBonusOrWithdrawal(any(), any(), any())(any())).thenReturn(Future.successful(GetWithdrawalResponse(
        periodStartDate = new DateTime("2017-05-06"),
        periodEndDate = new DateTime("2017-06-05"),
        automaticRecoveryAmount = Some(250),
        withdrawalAmount = 2000,
        withdrawalChargeAmount = 500,
        withdrawalChargeAmountYtd = 500,
        fundsDeductedDuringWithdrawal = true,
        withdrawalReason = "Superseded withdrawal",
        supersededBy = Some("1234567892"),
        supersede = Some(WithdrawalSuperseded("1234567890", 250, 250, "Additional withdrawal")),
        paymentStatus = "Collected",
        creationDate = new DateTime("2017-06-19")
      )))

      doGetRequest(res => {
        status(res) mustBe OK
        contentAsJson(res) mustBe Json.obj(
          "claimPeriodStartDate" -> "2017-05-06",
          "claimPeriodEndDate" -> "2017-06-05",
          "automaticRecoveryAmount" -> 250,
          "withdrawalAmount" -> 2000,
          "withdrawalChargeAmount" -> 500,
          "withdrawalChargeAmountYTD" -> 500,
          "fundsDeductedDuringWithdrawal" -> true,
          "withdrawalReason" -> "Superseded withdrawal",
          "supersededBy" -> "1234567892",
          "supersede" -> Json.obj(
            "originalTransactionId" -> "1234567890",
            "originalWithdrawalChargeAmount" -> 250,
            "transactionResult" -> 250,
            "reason" -> "Additional withdrawal"
          )
        )
      })
    }

    "return 404 status invalid lisa account (investor id not found)" in {
      when(mockGetService.getBonusOrWithdrawal(any(), any(), any())(any())).thenReturn(Future.successful(GetBonusOrWithdrawalInvestorNotFoundResponse))
      doGetRequest(res => {
        status(res) mustBe (NOT_FOUND)
        val json = contentAsJson(res)
        (json \ "code").as[String] mustBe ErrorAccountNotFound.errorCode
        (json \ "message").as[String] mustBe ErrorAccountNotFound.message
      })
    }

    "return 404 transaction not found" when {

      "given a transaction not found error from the connector" in {
        when(mockGetService.getBonusOrWithdrawal(any(), any(), any())(any())).thenReturn(Future.successful(GetBonusOrWithdrawalTransactionNotFoundResponse))
        doGetRequest(res => {
          status(res) mustBe (NOT_FOUND)
          val json = contentAsJson(res)
          (json \ "code").as[String] mustBe ErrorWithdrawalNotFound.errorCode
          (json \ "message").as[String] mustBe ErrorWithdrawalNotFound.message
        })
      }

      "given a withdrawal charge transaction from the connector" in {
        when(mockGetService.getBonusOrWithdrawal(any(), any(), any())(any())).thenReturn(Future.successful(GetBonusResponse(
          Some("1234567891"),
          new DateTime("2017-04-06"),
          new DateTime("2017-05-05"),
          Some(HelpToBuyTransfer(0, 10)),
          InboundPayments(Some(4000), 4000, 4000, 4000),
          Bonuses(1000, 1000, Some(1000), "Life Event"),
          Some("1234567892"),
          Some(BonusRecovery(100, "1234567890", 1100, -100)),
          "Paid",
          new DateTime("2017-05-20"))
        ))

        doGetRequest(res => {
          status(res) mustBe (NOT_FOUND)
          val json = contentAsJson(res)
          (json \ "code").as[String] mustBe ErrorWithdrawalNotFound.errorCode
          (json \ "message").as[String] mustBe ErrorWithdrawalNotFound.message
        })
      }

    }

    "return an internal server error response" in {
      when(mockGetService.getBonusOrWithdrawal(any(), any(), any())(any())).thenReturn(Future.successful(GetBonusOrWithdrawalErrorResponse))
      doGetRequest(res => {
        (contentAsJson(res) \ "code").as[String] mustBe "INTERNAL_SERVER_ERROR"
      })
    }

  }

  def doRequest(jsonString: String, lmrn: String = lisaManager)(callback: (Future[Result]) =>  Unit): Unit = {
    val res = SUT.reportWithdrawalCharge(lmrn, accountId).apply(FakeRequest(Helpers.PUT, "/").withHeaders(acceptHeader).
      withBody(AnyContentAsJson(Json.parse(jsonString))))

    callback(res)
  }

  def doGetRequest(callback: (Future[Result]) =>  Unit): Unit = {
    val res = SUT.getWithdrawalCharge(lisaManager, accountId, transactionId).apply(FakeRequest(Helpers.GET, "/").withHeaders(acceptHeader))

    callback(res)
  }

  val mockPostService: WithdrawalService = mock[WithdrawalService]
  val mockGetService: BonusOrWithdrawalService = mock[BonusOrWithdrawalService]
  val mockAuditService: AuditService = mock[AuditService]
  val mockAuthCon: LisaAuthConnector = mock[LisaAuthConnector]
  val mockDateTimeService: CurrentDateService = mock[CurrentDateService]
  val mockValidator: WithdrawalChargeValidator = mock[WithdrawalChargeValidator]

  val SUT = new WithdrawalController {
    override val postService: WithdrawalService = mockPostService
    override val getService: BonusOrWithdrawalService = mockGetService
    override val auditService: AuditService = mockAuditService
    override val authConnector: LisaAuthConnector = mockAuthCon
    override val dateTimeService: CurrentDateService = mockDateTimeService
    override val validator: WithdrawalChargeValidator = mockValidator
  }

}
