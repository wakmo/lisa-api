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

package unit.models

import org.joda.time.DateTime
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsError, JsSuccess, Json}
import uk.gov.hmrc.lisaapi.models._

class GetBulkPaymentResponseSpec extends PlaySpec {

  val paidResponse = """{
                       |  "clearedAmount": -1000,
                       |  "sapDocumentNumber": "ABC123456789",
                       |  "items": [
                       |    {
                       |      "clearingDate": "2017-06-01"
                       |    }
                       |  ]
                       |}""".stripMargin

  val pendingResponse = """{
                       |  "outstandingAmount": -1000,
                       |  "items": [
                       |    {
                       |      "dueDate": "2017-06-01"
                       |    }
                       |  ]
                       |}""".stripMargin

  val collectedResponse = """{
                       |  "clearedAmount": 1000,
                       |  "sapDocumentNumber": "ABC123456789",
                       |  "items": [
                       |    {
                       |      "clearingDate": "2017-06-01"
                       |    }
                       |  ]
                       |}""".stripMargin

  val dueResponse = """{
                          |  "outstandingAmount": 1000,
                          |  "items": [
                          |    {
                          |      "dueDate": "2017-06-01"
                          |    }
                          |  ]
                          |}""".stripMargin

  "BulkPayment" must {

    "serialize to the correct type" in {
      val paid = Json.parse(paidResponse).as[BulkPayment]

      paid match {
        case _:BulkPaymentPaid =>
        case _ => fail("Parsed a paid response as a pending")
      }

      val pending = Json.parse(pendingResponse).as[BulkPayment]

      pending match {
        case _:BulkPaymentPending =>
        case _ => fail("Parsed a pending response as a paid")
      }

      val collected = Json.parse(collectedResponse).as[BulkPayment]

      collected match {
        case _:BulkPaymentCollected =>
        case _ => fail("Parsed a paid response as a pending")
      }

      val due = Json.parse(dueResponse).as[BulkPayment]

      due match {
        case _:BulkPaymentDue =>
        case _ => fail("Parsed a pending response as a paid")
      }
    }

  }

  "BulkPaymentPaid" must {

    "serialize from json" in {
      val res = Json.parse(paidResponse).validate[BulkPaymentPaid]

      res match {
        case errors: JsError => fail(s"Json validation failed: ${JsError.toFlatForm(errors)}")
        case JsSuccess(data, _) => {
          data.paymentAmount mustBe 1000.0
          data.paymentDate mustBe new DateTime("2017-06-01")
          data.paymentReference mustBe "ABC123456789"
        }
      }
    }

    "deserialize to json" in {
      val data = BulkPaymentPaid(1000.0, new DateTime("2017-06-01"), "ABC123")
      val json = Json.toJson(data)
      (json \ "paymentAmount").as[BigDecimal] mustBe 1000.0
      (json \ "paymentDate").as[String] mustBe "2017-06-01"
      (json \ "paymentReference").as[String] mustBe "ABC123"
      (json \ "transactionType").as[String] mustBe "Payment"
      (json \ "status").as[String] mustBe "Paid"
    }

  }

  "BulkPaymentPending" must {

    "serialize from json" in {
      val res = Json.parse(pendingResponse).validate[BulkPaymentPending]

      res match {
        case errors: JsError => fail(s"Json validation failed: ${JsError.toFlatForm(errors)}")
        case JsSuccess(data, _) => {
          data.paymentAmount mustBe 1000.0
          data.dueDate mustBe new DateTime("2017-06-01")
        }
      }
    }

    "deserialize to json" in {
      val data = BulkPaymentPending(1000.0, new DateTime("2017-06-01"))
      val json = Json.toJson(data)
      (json \ "paymentAmount").as[BigDecimal] mustBe 1000.0
      (json \ "dueDate").as[String] mustBe "2017-06-01"
      (json \ "transactionType").as[String] mustBe "Payment"
      (json \ "status").as[String] mustBe "Pending"
    }

  }

  "BulkPaymentCollected" must {

    "serialize from json" in {
      val res = Json.parse(collectedResponse).validate[BulkPaymentCollected]

      res match {
        case errors: JsError => fail(s"Json validation failed: ${JsError.toFlatForm(errors)}")
        case JsSuccess(data, _) => {
          data.paymentAmount mustBe 1000.0
          data.paymentDate mustBe new DateTime("2017-06-01")
          data.paymentReference mustBe "ABC123456789"
        }
      }
    }

    "deserialize to json" in {
      val data = BulkPaymentCollected(1000.0, new DateTime("2017-06-01"), "ABC123")
      val json = Json.toJson(data)
      (json \ "paymentAmount").as[BigDecimal] mustBe 1000.0
      (json \ "paymentDate").as[String] mustBe "2017-06-01"
      (json \ "paymentReference").as[String] mustBe "ABC123"
      (json \ "transactionType").as[String] mustBe "Debt"
      (json \ "status").as[String] mustBe "Collected"
    }

  }

  "BulkPaymentDue" must {

    "serialize from json" in {
      val res = Json.parse(dueResponse).validate[BulkPaymentDue]

      res match {
        case errors: JsError => fail(s"Json validation failed: ${JsError.toFlatForm(errors)}")
        case JsSuccess(data, _) => {
          data.paymentAmount mustBe 1000.0
          data.dueDate mustBe new DateTime("2017-06-01")
        }
      }
    }

    "deserialize to json" in {
      val data = BulkPaymentDue(1000.0, new DateTime("2017-06-01"))
      val json = Json.toJson(data)
      (json \ "paymentAmount").as[BigDecimal] mustBe 1000.0
      (json \ "dueDate").as[String] mustBe "2017-06-01"
      (json \ "transactionType").as[String] mustBe "Debt"
      (json \ "status").as[String] mustBe "Due"
    }

  }

}
