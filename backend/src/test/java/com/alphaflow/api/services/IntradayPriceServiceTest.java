package com.alphaflow.api.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.alphaflow.api.dtos.OhlcvDto;
import com.alphaflow.common.enums.IntradayTimeframe;
import com.alphaflow.persistence.entities.IntradayPrice;
import com.alphaflow.persistence.entities.LiveQuote;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.enums.Country;
import com.alphaflow.persistence.repositories.IntradayPriceRepository;
import com.alphaflow.persistence.repositories.LiveQuoteRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

class IntradayPriceServiceTest {

  private static final ZoneOffset IST_OFFSET = ZoneOffset.ofHoursMinutes(5, 30);

  private IntradayPriceRepository intradayPriceRepository;
  private LiveQuoteRepository liveQuoteRepository;
  private IntradayPriceService intradayPriceService;
  private Ticker ticker;

  @BeforeEach
  void setUp() {
    intradayPriceRepository = mock(IntradayPriceRepository.class);
    liveQuoteRepository = mock(LiveQuoteRepository.class);
    intradayPriceService = new IntradayPriceService(intradayPriceRepository, liveQuoteRepository);
    ticker = Ticker.builder().tickerSymbol("NIFTY50").country(Country.IN).isActive(true).build();
  }

  @Test
  void testGetIntradayPriceWithoutLiveQuote() {
    OffsetDateTime firstTime = OffsetDateTime.of(2026, 9, 23, 9, 15, 0, 0, IST_OFFSET);
    OffsetDateTime secondTime = OffsetDateTime.of(2026, 9, 23, 9, 30, 0, 0, IST_OFFSET);

    IntradayPrice firstBar =
        IntradayPrice.builder()
            .ticker(ticker)
            .timeframe("15M")
            .priceTime(firstTime)
            .priceOpen(new BigDecimal("25000.0000"))
            .priceHigh(new BigDecimal("25050.0000"))
            .priceLow(new BigDecimal("24990.0000"))
            .priceClose(new BigDecimal("25040.0000"))
            .volume(BigDecimal.ZERO)
            .build();

    IntradayPrice secondBar =
        IntradayPrice.builder()
            .ticker(ticker)
            .timeframe("15M")
            .priceTime(secondTime)
            .priceOpen(new BigDecimal("25040.0000"))
            .priceHigh(new BigDecimal("25080.0000"))
            .priceLow(new BigDecimal("25030.0000"))
            .priceClose(new BigDecimal("25075.0000"))
            .volume(BigDecimal.ZERO)
            .build();

    when(intradayPriceRepository.findLatestByTickerAndTimeframe(
            ticker, IntradayTimeframe._15M.getValue(), PageRequest.of(0, 250)))
        .thenReturn(List.of(secondBar, firstBar));
    when(liveQuoteRepository.findByTicker(ticker)).thenReturn(Optional.empty());

    List<OhlcvDto> result =
        intradayPriceService.getIntradayPrice(ticker, IntradayTimeframe._15M, 0, 250);

    assertEquals(2, result.size());
    assertEquals(firstTime, result.getFirst().priceDate());
    assertEquals(secondTime, result.get(1).priceDate());

    List<OhlcvDto> resultNullEndDate =
        intradayPriceService.getIntradayPrice(
            ticker, IntradayTimeframe._15M, (LocalDate) null, 0, 250);
    assertEquals(2, resultNullEndDate.size());

    LocalDate endDate = LocalDate.of(2026, 9, 23);
    OffsetDateTime endDateTime =
        endDate.atTime(LocalTime.MAX).atZone(ticker.getCountry().getZoneId()).toOffsetDateTime();
    when(intradayPriceRepository.findLatestByTickerAndTimeframeAndEndDate(
            ticker, IntradayTimeframe._15M.getValue(), endDateTime, PageRequest.of(0, 250)))
        .thenReturn(List.of(firstBar));

    List<OhlcvDto> resultWithEndDate =
        intradayPriceService.getIntradayPrice(ticker, IntradayTimeframe._15M, endDate, 0, 250);
    assertEquals(1, resultWithEndDate.size());
    assertEquals(firstTime, resultWithEndDate.getFirst().priceDate());
  }

  @Test
  void testGetIntradayPriceAppendsNewInProgressCandleWhenQuoteInNewBucket() {
    OffsetDateTime firstTime = OffsetDateTime.of(2026, 9, 23, 9, 15, 0, 0, IST_OFFSET);
    IntradayPrice firstBar =
        IntradayPrice.builder()
            .ticker(ticker)
            .timeframe("15M")
            .priceTime(firstTime)
            .priceOpen(new BigDecimal("25000.0000"))
            .priceHigh(new BigDecimal("25050.0000"))
            .priceLow(new BigDecimal("24990.0000"))
            .priceClose(new BigDecimal("25040.0000"))
            .volume(BigDecimal.ZERO)
            .build();

    OffsetDateTime quoteTime = OffsetDateTime.of(2026, 9, 23, 9, 37, 25, 0, IST_OFFSET);
    LiveQuote liveQuote =
        LiveQuote.builder()
            .ticker(ticker)
            .lastPrice(new BigDecimal("25120.5000"))
            .volume(new BigDecimal("100.0000"))
            .quoteTime(quoteTime)
            .build();

    when(intradayPriceRepository.findLatestByTickerAndTimeframe(
            ticker, IntradayTimeframe._15M.getValue(), PageRequest.of(0, 250)))
        .thenReturn(List.of(firstBar));
    when(liveQuoteRepository.findByTicker(ticker)).thenReturn(Optional.of(liveQuote));

    List<OhlcvDto> result =
        intradayPriceService.getIntradayPrice(ticker, IntradayTimeframe._15M, 0, 250);

    assertEquals(2, result.size());
    OhlcvDto liveCandle = result.getLast();
    assertEquals(OffsetDateTime.of(2026, 9, 23, 9, 30, 0, 0, IST_OFFSET), liveCandle.priceDate());
    assertEquals(new BigDecimal("25120.5000"), liveCandle.priceOpen());
    assertEquals(new BigDecimal("25120.5000"), liveCandle.priceHigh());
    assertEquals(new BigDecimal("25120.5000"), liveCandle.priceLow());
    assertEquals(new BigDecimal("25120.5000"), liveCandle.priceClose());
    assertEquals(new BigDecimal("100.0000"), liveCandle.volume());
  }

  @Test
  void testGetIntradayPriceSynthesizesCandleWhenClosedCandlesListEmpty() {
    OffsetDateTime quoteTime = OffsetDateTime.of(2026, 9, 23, 9, 18, 0, 0, IST_OFFSET);
    LiveQuote liveQuote =
        LiveQuote.builder()
            .ticker(ticker)
            .lastPrice(new BigDecimal("25010.0000"))
            .volume(BigDecimal.ZERO)
            .quoteTime(quoteTime)
            .build();

    when(intradayPriceRepository.findLatestByTickerAndTimeframe(
            ticker, IntradayTimeframe._15M.getValue(), PageRequest.of(0, 250)))
        .thenReturn(List.of());
    when(liveQuoteRepository.findByTicker(ticker)).thenReturn(Optional.of(liveQuote));

    List<OhlcvDto> result =
        intradayPriceService.getIntradayPrice(ticker, IntradayTimeframe._15M, 0, 250);

    assertEquals(1, result.size());
    assertEquals(
        OffsetDateTime.of(2026, 9, 23, 9, 15, 0, 0, IST_OFFSET), result.getFirst().priceDate());
    assertEquals(new BigDecimal("25010.0000"), result.getFirst().priceClose());
  }

  @Test
  void testGetIntradayPriceUpdatesExistingCandleWhenQuoteInSameBucket() {
    OffsetDateTime bucketTime = OffsetDateTime.of(2026, 9, 23, 9, 15, 0, 0, IST_OFFSET);
    IntradayPrice existingBar =
        IntradayPrice.builder()
            .ticker(ticker)
            .timeframe("15M")
            .priceTime(bucketTime)
            .priceOpen(new BigDecimal("25000.0000"))
            .priceHigh(new BigDecimal("25050.0000"))
            .priceLow(new BigDecimal("24990.0000"))
            .priceClose(new BigDecimal("25040.0000"))
            .volume(new BigDecimal("500.0000"))
            .build();

    OffsetDateTime quoteTime = OffsetDateTime.of(2026, 9, 23, 9, 22, 0, 0, IST_OFFSET);
    LiveQuote liveQuote =
        LiveQuote.builder()
            .ticker(ticker)
            .lastPrice(new BigDecimal("25080.0000"))
            .volume(BigDecimal.ZERO)
            .quoteTime(quoteTime)
            .build();

    when(intradayPriceRepository.findLatestByTickerAndTimeframe(
            ticker, IntradayTimeframe._15M.getValue(), PageRequest.of(0, 250)))
        .thenReturn(List.of(existingBar));
    when(liveQuoteRepository.findByTicker(ticker)).thenReturn(Optional.of(liveQuote));

    List<OhlcvDto> result =
        intradayPriceService.getIntradayPrice(ticker, IntradayTimeframe._15M, 0, 250);

    assertEquals(1, result.size());
    OhlcvDto updatedCandle = result.getFirst();
    assertEquals(bucketTime, updatedCandle.priceDate());
    assertEquals(new BigDecimal("25000.0000"), updatedCandle.priceOpen());
    assertEquals(new BigDecimal("25080.0000"), updatedCandle.priceHigh());
    assertEquals(new BigDecimal("24990.0000"), updatedCandle.priceLow());
    assertEquals(new BigDecimal("25080.0000"), updatedCandle.priceClose());
    assertEquals(new BigDecimal("500.0000"), updatedCandle.volume());
  }

  @Test
  void testGetIntradayPriceIgnoresStaleLiveQuoteOlderThanOrEqualLatestClosedCandle() {
    OffsetDateTime latestBarTime = OffsetDateTime.of(2026, 9, 23, 10, 0, 0, 0, IST_OFFSET);
    IntradayPrice latestBar =
        IntradayPrice.builder()
            .ticker(ticker)
            .timeframe("15M")
            .priceTime(latestBarTime)
            .priceOpen(new BigDecimal("25000.0000"))
            .priceHigh(new BigDecimal("25050.0000"))
            .priceLow(new BigDecimal("24990.0000"))
            .priceClose(new BigDecimal("25040.0000"))
            .volume(BigDecimal.ZERO)
            .build();

    OffsetDateTime olderQuoteTime = OffsetDateTime.of(2026, 9, 23, 9, 35, 0, 0, IST_OFFSET);
    LiveQuote staleQuote =
        LiveQuote.builder()
            .ticker(ticker)
            .lastPrice(new BigDecimal("24950.0000"))
            .volume(BigDecimal.ZERO)
            .quoteTime(olderQuoteTime)
            .build();

    when(intradayPriceRepository.findLatestByTickerAndTimeframe(
            ticker, IntradayTimeframe._15M.getValue(), PageRequest.of(0, 250)))
        .thenReturn(List.of(latestBar));
    when(liveQuoteRepository.findByTicker(ticker)).thenReturn(Optional.of(staleQuote));

    List<OhlcvDto> result =
        intradayPriceService.getIntradayPrice(ticker, IntradayTimeframe._15M, 0, 250);

    assertEquals(1, result.size());
    assertEquals(latestBarTime, result.getFirst().priceDate());
    assertEquals(new BigDecimal("25040.0000"), result.getFirst().priceClose());
  }

  @Test
  void testGetIntradayPriceThrowsWhenTimeframeNull() {
    assertThrows(
        IllegalArgumentException.class,
        () -> intradayPriceService.getIntradayPrice(ticker, null, 0, 250));

    assertThrows(
        IllegalArgumentException.class,
        () -> intradayPriceService.getIntradayPrice(ticker, null, LocalDate.now(), 0, 250));
  }
}
