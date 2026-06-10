package com.alphaflow.persistence.repositories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.alphaflow.api.dtos.OhlcvDTO;
import com.alphaflow.persistence.entities.DailyPrice;
import com.alphaflow.persistence.entities.Ticker;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

class DailyPriceRepositoryTest {

  @Test
  void testGetDailyPriceDefaultMethod() {
    // We mock the interface but configure it to call real implementations for default methods.
    DailyPriceRepository dailyRepo = mock(DailyPriceRepository.class, CALLS_REAL_METHODS);

    Ticker ticker = Ticker.builder().tickerSymbol("AAPL").isActive(true).build();

    DailyPrice dp1 =
        DailyPrice.builder()
            .priceDate(LocalDate.of(2026, 5, 29))
            .priceOpen(new BigDecimal("100.0000"))
            .priceHigh(new BigDecimal("105.0000"))
            .priceLow(new BigDecimal("99.0000"))
            .priceClose(new BigDecimal("102.0000"))
            .volume(new BigDecimal("1000.0000"))
            .build();

    DailyPrice dp2 =
        DailyPrice.builder()
            .priceDate(LocalDate.of(2026, 5, 28))
            .priceOpen(new BigDecimal("98.0000"))
            .priceHigh(new BigDecimal("101.0000"))
            .priceLow(new BigDecimal("97.0000"))
            .priceClose(new BigDecimal("99.0000"))
            .volume(new BigDecimal("800.0000"))
            .build();

    // Stub the non-default query method findLatestByTickerAndEndDate
    when(dailyRepo.findLatestByTickerAndEndDate(
            eq(ticker), any(LocalDate.class), eq(PageRequest.of(0, 250))))
        .thenReturn(List.of(dp1, dp2));

    // Call the default method
    List<OhlcvDTO> result = dailyRepo.getDailyPrice(ticker, 0, 250);

    assertEquals(2, result.size());
    // Result should be sorted ascending by date (dp2, then dp1)
    assertEquals(LocalDate.of(2026, 5, 28), result.getFirst().priceDate());
    assertEquals(LocalDate.of(2026, 5, 29), result.get(1).priceDate());
  }

  @Test
  void testGetDailyPriceWithCustomPageAndSizeSuccess() {
    DailyPriceRepository dailyRepo = mock(DailyPriceRepository.class, CALLS_REAL_METHODS);

    Ticker ticker = Ticker.builder().tickerSymbol("AAPL").isActive(true).build();

    DailyPrice dp =
        DailyPrice.builder()
            .priceDate(LocalDate.of(2026, 5, 29))
            .priceOpen(new BigDecimal("100.0000"))
            .priceHigh(new BigDecimal("105.0000"))
            .priceLow(new BigDecimal("99.0000"))
            .priceClose(new BigDecimal("102.0000"))
            .volume(new BigDecimal("1000.0000"))
            .build();

    when(dailyRepo.findLatestByTickerAndEndDate(
            eq(ticker), any(LocalDate.class), eq(PageRequest.of(1, 10))))
        .thenReturn(List.of(dp));

    List<OhlcvDTO> result = dailyRepo.getDailyPrice(ticker, 1, 10);

    assertEquals(1, result.size());
    assertEquals(LocalDate.of(2026, 5, 29), result.getFirst().priceDate());
  }
}
