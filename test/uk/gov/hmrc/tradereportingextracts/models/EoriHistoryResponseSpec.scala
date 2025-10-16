package uk.gov.hmrc.tradereportingextracts.models

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

import java.time.LocalDate

class EoriHistoryResponseSpec extends AnyFreeSpec with Matchers {

  "EoriHistoryResponse" - {
    "filterByDateRange must correctly filter EoriHistory entries by date range" in {
      val eoriHistories = Seq(EoriHistory("GB250520228000", Some("2009-05-16"), Some("2025-10-07")))

      //left outside
      EoriHistoryResponse(eoriHistories).filterByDateRange(
        LocalDate.parse("2000-04-16"),
        LocalDate.parse("2009-04-16")
      ) mustBe Seq.empty

      // left edge
      EoriHistoryResponse(eoriHistories).filterByDateRange(
        LocalDate.parse("2009-04-17"),
        LocalDate.parse("2009-05-17")
      ) mustBe eoriHistories

      // inside
      EoriHistoryResponse(eoriHistories).filterByDateRange(
        LocalDate.parse("2025-04-16"),
        LocalDate.parse("2025-05-16")
      ) mustBe eoriHistories

      // right edge
      EoriHistoryResponse(eoriHistories).filterByDateRange(
        LocalDate.parse("2025-10-06"),
        LocalDate.parse("2025-10-08")
      ) mustBe eoriHistories

      // right out side
      EoriHistoryResponse(eoriHistories).filterByDateRange(
        LocalDate.parse("2025-10-08"),
        LocalDate.parse("2025-11-16")
      ) mustBe Seq.empty
    }

  }
}
