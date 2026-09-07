package com.alphaflow.api.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.alphaflow.api.dtos.CandlestickPatternDto;
import com.alphaflow.common.enums.Timeframe;
import com.alphaflow.persistence.entities.DailyCandlestickPattern;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.entities.WeeklyCandlestickPattern;
import com.alphaflow.persistence.enums.CandlestickPattern;
import com.alphaflow.persistence.enums.PatternSentiment;
import com.alphaflow.persistence.repositories.DailyCandlestickPatternRepository;
import com.alphaflow.persistence.repositories.DailyPriceRepository;
import com.alphaflow.persistence.repositories.WeeklyCandlestickPatternRepository;
import com.alphaflow.persistence.repositories.WeeklyPriceRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

class CandlestickPatternServiceTest {

  private DailyPriceRepository dailyPriceRepository;
  private WeeklyPriceRepository weeklyPriceRepository;
  private DailyCandlestickPatternRepository dailyCandlestickPatternRepository;
  private WeeklyCandlestickPatternRepository weeklyCandlestickPatternRepository;
  private CandlestickPatternService service;
  private Ticker ticker;

  @BeforeEach
  void setUp() {
    dailyPriceRepository = mock(DailyPriceRepository.class);
    weeklyPriceRepository = mock(WeeklyPriceRepository.class);
    dailyCandlestickPatternRepository = mock(DailyCandlestickPatternRepository.class);
    weeklyCandlestickPatternRepository = mock(WeeklyCandlestickPatternRepository.class);

    service =
        new CandlestickPatternService(
            dailyPriceRepository,
            weeklyPriceRepository,
            dailyCandlestickPatternRepository,
            weeklyCandlestickPatternRepository);

    ticker = Ticker.builder().tickerId(1L).tickerSymbol("AAPL").build();
  }

  @Test
  void testGetPatternsDaily() {
    LocalDate date = LocalDate.of(2026, 5, 29);
    when(dailyPriceRepository.findRecentPriceDatesUpTo(
            eq(ticker), any(LocalDate.class), eq(PageRequest.of(0, 50))))
        .thenReturn(List.of(date));

    DailyCandlestickPattern pattern =
        DailyCandlestickPattern.builder()
            .ticker(ticker)
            .priceDate(date)
            .pattern(CandlestickPattern.HAMMER)
            .sentiment(PatternSentiment.BULLISH_REVERSAL)
            .build();

    when(dailyCandlestickPatternRepository.findSeriesBetween(ticker, date, date))
        .thenReturn(List.of(pattern));

    List<CandlestickPatternDto> result = service.getPatterns(ticker, Timeframe.DAILY, 0, 50);

    assertEquals(1, result.size());
    assertEquals(date, result.getFirst().date());
    assertEquals(CandlestickPattern.HAMMER.getShortName(), result.getFirst().shortName());
    assertEquals(CandlestickPattern.HAMMER.getLongName(), result.getFirst().longName());
    assertEquals(PatternSentiment.BULLISH_REVERSAL, result.getFirst().sentiment());
  }

  @Test
  void testGetPatternsWeekly() {
    LocalDate date = LocalDate.of(2026, 5, 29);
    when(weeklyPriceRepository.findRecentPriceDatesUpTo(
            eq(ticker), any(LocalDate.class), eq(PageRequest.of(0, 50))))
        .thenReturn(List.of(date));

    WeeklyCandlestickPattern pattern =
        WeeklyCandlestickPattern.builder()
            .ticker(ticker)
            .priceDate(date)
            .pattern(CandlestickPattern.SHOOTING_STAR)
            .sentiment(PatternSentiment.BEARISH_REVERSAL)
            .build();

    when(weeklyCandlestickPatternRepository.findSeriesBetween(ticker, date, date))
        .thenReturn(List.of(pattern));

    List<CandlestickPatternDto> result = service.getPatterns(ticker, Timeframe.WEEKLY, 0, 50);

    assertEquals(1, result.size());
    assertEquals(date, result.getFirst().date());
    assertEquals(CandlestickPattern.SHOOTING_STAR.getShortName(), result.getFirst().shortName());
    assertEquals(CandlestickPattern.SHOOTING_STAR.getLongName(), result.getFirst().longName());
    assertEquals(PatternSentiment.BEARISH_REVERSAL, result.getFirst().sentiment());
  }

  @Test
  void testGetPatternsEmptyDates() {
    when(dailyPriceRepository.findRecentPriceDatesUpTo(
            eq(ticker), any(LocalDate.class), eq(PageRequest.of(0, 50))))
        .thenReturn(List.of());

    List<CandlestickPatternDto> result = service.getPatterns(ticker, Timeframe.DAILY, 0, 50);
    assertTrue(result.isEmpty());
  }

  @Test
  void testGetPatternsUnsupportedTimeframe() {
    assertThrows(IllegalArgumentException.class, () -> service.getPatterns(ticker, null, 0, 50));
  }
}
