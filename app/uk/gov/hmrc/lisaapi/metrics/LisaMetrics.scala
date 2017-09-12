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

package uk.gov.hmrc.lisaapi.metrics

import java.util.concurrent.TimeUnit

import com.codahale.metrics.MetricRegistry
import uk.gov.hmrc.play.graphite.MicroserviceMetrics

trait LisaMetrics {
  def timer (diff: Long, unit: TimeUnit, metricType: String) : Unit
}

object LisaMetrics extends LisaMetrics with MicroserviceMetrics {

  val registry: MetricRegistry = metrics.defaultRegistry

  override def timer(diff: Long, unit: TimeUnit, metricType: String) =   registry.timer(s"${metricType}").update(diff, unit)

  def startMetrics(startTime: Long, api: String): Unit =  LisaMetrics.timer(startTime, TimeUnit.MILLISECONDS, api.toString)

  def incrementMetrics(startTime: Long, api: String): Unit = {
    LisaMetrics.timer(System.currentTimeMillis() - startTime, TimeUnit.MILLISECONDS, api.toString)
  }

}

trait LisaMetricKeys  {
  val INVESTOR = "LISA_INVESTOR"
  val ACCOUNT = "CREATE_OR_TRANSFER_ACCOUNT"
 val  CLOSE = "CLOSE_ACCOUNT"
  val EVENT = "LIFE_EVENT"
  val BONUS_PAYMENT = "BONUS_PAYMENT"

  def lisaError(status:Int, name:String):String =  s"${name}_${status}"
  val BAD_REQUEST = "LISA_400"
}
object LisaMetricKeys extends LisaMetricKeys