package uk.gov.hmrc.tradereportingextracts.controllers

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.test.Helpers.*
import play.api.test.{FakeRequest, Helpers}
import play.api.{Application, inject}
import uk.gov.hmrc.tradereportingextracts.connectors.CustomsDataStoreConnector
import uk.gov.hmrc.tradereportingextracts.models.{AddressInformation, CompanyInformation}
import uk.gov.hmrc.tradereportingextracts.utils.SpecBase

import scala.concurrent.Future

class CompanyInformationControllerSpec extends SpecBase {

  "getCompanyInformation" should {

    "return company information" in new Setup {
      when(mockCustomsDataStoreConnector.getCompanyInformation(any())(any()))
        .thenReturn(Future.successful(companyInformation))

      running(app) {
        val request = FakeRequest(GET, routes.CompanyInformationController.companyInformation("AB00029").url)

        val result = route(app, request).value

        contentAsJson(result).as[CompanyInformation] mustBe companyInformation
      }
    }
  }

  trait Setup {

    val addressInformation: AddressInformation =
      AddressInformation("12 Example Street", "Example", Some("AA00 0AA"), "GB")

    val companyInformation: CompanyInformation = CompanyInformation("Example Ltd", "1", addressInformation)


    val mockCustomsDataStoreConnector: CustomsDataStoreConnector = mock[CustomsDataStoreConnector]

    val app: Application = application
      .overrides(
        inject.bind[CustomsDataStoreConnector].toInstance(mockCustomsDataStoreConnector)
      )
      .build()
  }
}
