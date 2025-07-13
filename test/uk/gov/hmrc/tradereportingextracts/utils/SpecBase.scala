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

package uk.gov.hmrc.tradereportingextracts.utils

import com.codahale.metrics.MetricRegistry
import org.apache.pekko.stream.Materializer
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.{BeforeAndAfterEach, OptionValues}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads}
import uk.gov.hmrc.play.bootstrap.metrics.Metrics

import java.net.URL
import scala.concurrent.{ExecutionContext, Future}

class SpecBase
    extends AnyWordSpecLike
    with GuiceOneAppPerSuite
    with MockitoSugar
    with Matchers
    with FutureAwaits
    with DefaultAwaitTimeout
    with OptionValues
    with BeforeAndAfterEach
    with ScalaFutures
    with IntegrationPatience {

  implicit val mat: Materializer   = app.materializer
  val mockHttpClient: HttpClientV2 = mock[HttpClientV2]

  def application: GuiceApplicationBuilder = new GuiceApplicationBuilder()
    .overrides(
      bind[Metrics].toInstance(new FakeMetrics)
    )
    .configure("metrics.enabled" -> "false")

  def executeGet[A]: Future[A] = {
    val mockGetRequestBuilder: RequestBuilder = mock[RequestBuilder]
    when(mockGetRequestBuilder.setHeader(any[(String, String)])).thenReturn(mockGetRequestBuilder)
    when(mockHttpClient.get(any[URL])(any[HeaderCarrier])).thenReturn(mockGetRequestBuilder)
    mockGetRequestBuilder.execute[A](any[HttpReads[A]], any[ExecutionContext])
  }

  class FakeMetrics extends Metrics {
    override val defaultRegistry: MetricRegistry = new MetricRegistry
  }
}
