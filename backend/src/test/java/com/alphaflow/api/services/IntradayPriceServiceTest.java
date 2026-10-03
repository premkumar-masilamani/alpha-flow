package com.alphaflow.api.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.alphaflow.api.dtos.OhlcvDto;
import com.alphaflow.common.enums.Timeframe;
import com.alphaflow.persistence.entities.IntradayPrice;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.repositories.IntradayPriceRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

class IntradayPriceServiceTest {

  @Test
  void testGetIntradayPriceWithValidTimeframe() {
    IntradayPriceRepository repo = mock(IntradayPriceRepository.class);
    IntradayPriceService service = new IntradayPriceService(repo);

    Ticker ticker = Ticker.builder().tickerSymbol("NIFTY50").isActive(true).build();

    OffsetDateTime t1 =
        OffsetDateTime.of(2026, 9, 23, 9, 15, 0, 0, ZoneOffset.ofHoursMinutes(5, 30));
    OffsetDateTime t2 =
        OffsetDateTime.of(2026, 9, 23, 9, 30, 0, 0, ZoneOffset.ofHoursMinutes(5, 30));

    IntradayPrice p1 =
        IntradayPrice.builder()
            .ticker(ticker)
            .timeframe("15M")
            .priceTime(t1)
            .priceOpen(new BigDecimal("25000.0000"))
            .priceHigh(new BigDecimal("25050.0000"))
            .priceLow(new BigDecimal("24990.0000"))
            .priceClose(new BigDecimal("25040.0000"))
            .volume(BigDecimal.ZERO)
            .build();

    IntradayPrice p2 =
        IntradayPrice.builder()
            .ticker(ticker)
            .timeframe("15M")
            .priceTime(t2)
            .priceOpen(new BigDecimal("25040.0000"))
            .priceHigh(new BigDecimal("25080.0000"))
            .priceLow(new BigDecimal("25030.0000"))
            .priceClose(new BigDecimal("25075.0000"))
            .volume(BigDecimal.ZERO)
            .build();

    when(repo.findLatestByTickerAndTimeframe(
            ticker, Timeframe._15M.getValue(), PageRequest.of(0, 250)))
        .thenReturn(List.of(p2, p1));

    List<OhlcvDto> result = service.getIntradayPrice(ticker, Timeframe._15M, 0, 250);

    assertEquals(2, result.size());
    assertEquals(t1, result.getFirst().priceDate());
    assertEquals(t2, result.get(1).priceDate());
  }

  @Test
  void testGetIntradayPriceThrowsWhenTimeframeNull() {
    IntradayPriceRepository repo = mock(IntradayPriceRepository.class);
    IntradayPriceService service = new IntradayPriceService(repo);

    Ticker ticker = Ticker.builder().tickerSymbol("NIFTY50").isActive(true).build();

    assertThrows(
        IllegalArgumentException.class,
        () ->
            service.getIntradayPrice(ticker, (com.alphaflow.common.enums.Timeframe) null, 0, 250));
  }
}
