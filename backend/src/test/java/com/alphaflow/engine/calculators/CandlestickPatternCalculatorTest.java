package com.alphaflow.engine.calculators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.alphaflow.persistence.entities.DailyCandlestickPattern;
import com.alphaflow.persistence.entities.DailyPrice;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.entities.WeeklyCandlestickPattern;
import com.alphaflow.persistence.entities.WeeklyPrice;
import com.alphaflow.persistence.enums.CandlestickPattern;
import com.alphaflow.persistence.repositories.DailyCandlestickPatternRepository;
import com.alphaflow.persistence.repositories.DailyPriceRepository;
import com.alphaflow.persistence.repositories.TickerRepository;
import com.alphaflow.persistence.repositories.WeeklyCandlestickPatternRepository;
import com.alphaflow.persistence.repositories.WeeklyPriceRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CandlestickPatternCalculatorTest {

  private final Ticker ticker =
      Ticker.builder().tickerId(1L).tickerSymbol("AAPL").tickerName("Apple").build();

  @Mock private TickerRepository tickerRepository;
  @Mock private DailyPriceRepository dailyPriceRepository;
  @Mock private WeeklyPriceRepository weeklyPriceRepository;
  @Mock private DailyCandlestickPatternRepository dailyCandlestickPatternRepository;
  @Mock private WeeklyCandlestickPatternRepository weeklyCandlestickPatternRepository;

  private CandlestickPatternCalculator calculator;

  @BeforeEach
  void setUp() {
    calculator =
        new CandlestickPatternCalculator(
            tickerRepository,
            dailyPriceRepository,
            weeklyPriceRepository,
            dailyCandlestickPatternRepository,
            weeklyCandlestickPatternRepository);
  }

  private DailyPrice daily(LocalDate date, String open, String high, String low, String close) {
    return DailyPrice.builder()
        .ticker(ticker)
        .priceDate(date)
        .priceOpen(new BigDecimal(open))
        .priceHigh(new BigDecimal(high))
        .priceLow(new BigDecimal(low))
        .priceClose(new BigDecimal(close))
        .volume(BigDecimal.TEN)
        .build();
  }

  private WeeklyPrice weekly(LocalDate date, String open, String high, String low, String close) {
    return WeeklyPrice.builder()
        .ticker(ticker)
        .priceDate(date)
        .priceOpen(new BigDecimal(open))
        .priceHigh(new BigDecimal(high))
        .priceLow(new BigDecimal(low))
        .priceClose(new BigDecimal(close))
        .volume(BigDecimal.TEN)
        .build();
  }

  @Test
  void testComputePatternsOrchestratorSuccessAndErrorIsolation() {
    Ticker activeTicker1 = Ticker.builder().tickerId(1L).tickerSymbol("T1").isActive(true).build();
    Ticker activeTicker2 = Ticker.builder().tickerId(2L).tickerSymbol("T2").isActive(true).build();

    when(tickerRepository.findByIsActiveTrue()).thenReturn(List.of(activeTicker1, activeTicker2));

    CandlestickPatternCalculator calculatorSpy = spy(calculator);
    doThrow(new RuntimeException("Calculators fail"))
        .when(calculatorSpy)
        .computeCandleStickPatternsForTicker(activeTicker1);
    doNothing().when(calculatorSpy).computeCandleStickPatternsForTicker(activeTicker2);

    calculatorSpy.computeCandleStickPatterns();

    verify(calculatorSpy).computeCandleStickPatternsForTicker(activeTicker1);
    verify(calculatorSpy).computeCandleStickPatternsForTicker(activeTicker2);
  }

  @Test
  void testNoPatternsCalculatedWithInsufficientData() {
    when(dailyPriceRepository.findByTickerOrderByPriceDateAsc(ticker))
        .thenReturn(List.of(daily(LocalDate.now(), "100", "105", "98", "102")));

    calculator.computeCandleStickPatternsForTicker(ticker);

    verify(dailyCandlestickPatternRepository, never()).saveAll(any());
  }

  @Test
  void testBullishMarubozuDetection() {
    List<DailyPrice> prices = new ArrayList<>();
    LocalDate base = LocalDate.of(2026, 1, 1);
    // Add 14 baseline candles
    for (int j = 0; j < 14; j++) {
      prices.add(daily(base.plusDays(j), "100", "101", "99", "100")); // body = 0, avgBody = 0
    }
    // Now add a Bullish Marubozu
    // Body = 10, range = 10, open=100, close=110, high=110, low=100
    prices.add(daily(base.plusDays(14), "100", "110", "100", "110"));

    when(dailyPriceRepository.findByTickerOrderByPriceDateAsc(ticker)).thenReturn(prices);
    when(dailyCandlestickPatternRepository.findFirstByTickerOrderByPriceDateDesc(ticker))
        .thenReturn(Optional.empty());

    calculator.computeCandleStickPatternsForTicker(ticker);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<DailyCandlestickPattern>> captor = ArgumentCaptor.forClass(List.class);
    verify(dailyCandlestickPatternRepository).saveAll(captor.capture());

    List<DailyCandlestickPattern> saved = captor.getValue();
    assertThat(saved).anyMatch(p -> p.getPattern() == CandlestickPattern.BULLISH_MARUBOZU);
  }

  @Test
  void testBearishMarubozuDetection() {
    List<DailyPrice> prices = new ArrayList<>();
    LocalDate base = LocalDate.of(2026, 1, 1);
    for (int j = 0; j < 14; j++) {
      prices.add(daily(base.plusDays(j), "100", "101", "99", "100"));
    }
    // Bearish Marubozu: open=110, close=100, high=110, low=100, range=10, body=10
    prices.add(daily(base.plusDays(14), "110", "110", "100", "100"));

    when(dailyPriceRepository.findByTickerOrderByPriceDateAsc(ticker)).thenReturn(prices);
    when(dailyCandlestickPatternRepository.findFirstByTickerOrderByPriceDateDesc(ticker))
        .thenReturn(Optional.empty());

    calculator.computeCandleStickPatternsForTicker(ticker);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<DailyCandlestickPattern>> captor = ArgumentCaptor.forClass(List.class);
    verify(dailyCandlestickPatternRepository).saveAll(captor.capture());
    assertThat(captor.getValue())
        .anyMatch(p -> p.getPattern() == CandlestickPattern.BEARISH_MARUBOZU);
  }

  @Test
  void testBullishEngulfingDetection() {
    List<DailyPrice> prices = new ArrayList<>();
    LocalDate base = LocalDate.of(2026, 1, 1);
    for (int j = 0; j < 14; j++) {
      prices.add(daily(base.plusDays(j), "100", "102", "98", "100"));
    }
    // Red candle: open=100, close=95
    prices.add(daily(base.plusDays(14), "100", "101", "94", "95"));
    // Green candle engulfing: open=94, close=101
    prices.add(daily(base.plusDays(15), "94", "102", "93", "101"));

    when(dailyPriceRepository.findByTickerOrderByPriceDateAsc(ticker)).thenReturn(prices);
    when(dailyCandlestickPatternRepository.findFirstByTickerOrderByPriceDateDesc(ticker))
        .thenReturn(Optional.empty());

    calculator.computeCandleStickPatternsForTicker(ticker);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<DailyCandlestickPattern>> captor = ArgumentCaptor.forClass(List.class);
    verify(dailyCandlestickPatternRepository).saveAll(captor.capture());
    assertThat(captor.getValue())
        .anyMatch(p -> p.getPattern() == CandlestickPattern.BULLISH_ENGULFING);
  }

  @Test
  void testBearishEngulfingDetection() {
    List<DailyPrice> prices = new ArrayList<>();
    LocalDate base = LocalDate.of(2026, 1, 1);
    for (int j = 0; j < 14; j++) {
      prices.add(daily(base.plusDays(j), "100", "102", "98", "100"));
    }
    // Green candle: open=95, close=100
    prices.add(daily(base.plusDays(14), "95", "101", "94", "100"));
    // Red candle engulfing: open=101, close=94
    prices.add(daily(base.plusDays(15), "101", "102", "93", "94"));

    when(dailyPriceRepository.findByTickerOrderByPriceDateAsc(ticker)).thenReturn(prices);
    when(dailyCandlestickPatternRepository.findFirstByTickerOrderByPriceDateDesc(ticker))
        .thenReturn(Optional.empty());

    calculator.computeCandleStickPatternsForTicker(ticker);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<DailyCandlestickPattern>> captor = ArgumentCaptor.forClass(List.class);
    verify(dailyCandlestickPatternRepository).saveAll(captor.capture());
    assertThat(captor.getValue())
        .anyMatch(p -> p.getPattern() == CandlestickPattern.BEARISH_ENGULFING);
  }

  @Test
  void testHammerDetection() {
    List<DailyPrice> prices = new ArrayList<>();
    LocalDate base = LocalDate.of(2026, 1, 1);
    // Create SMA trend baseline (high prices) with body = 10
    for (int j = 0; j < 14; j++) {
      prices.add(daily(base.plusDays(j), "190", "215", "185", "200"));
    }
    // Downtrend price + Hammer
    // Body is 0.1 (small: open=110, close=110.1). AvgBody is 10.
    // Lower shadow is 2.5 (>= 2x body), upper shadow is 0 (<= 0.1x body).
    // Close is 110.1, which is < SMA20 (~194).
    prices.add(daily(base.plusDays(14), "110", "110.1", "107.5", "110.1"));

    when(dailyPriceRepository.findByTickerOrderByPriceDateAsc(ticker)).thenReturn(prices);
    when(dailyCandlestickPatternRepository.findFirstByTickerOrderByPriceDateDesc(ticker))
        .thenReturn(Optional.empty());

    calculator.computeCandleStickPatternsForTicker(ticker);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<DailyCandlestickPattern>> captor = ArgumentCaptor.forClass(List.class);
    verify(dailyCandlestickPatternRepository).saveAll(captor.capture());
    assertThat(captor.getValue()).anyMatch(p -> p.getPattern() == CandlestickPattern.HAMMER);
  }

  @Test
  void testInvertedHammerDetection() {
    List<DailyPrice> prices = new ArrayList<>();
    LocalDate base = LocalDate.of(2026, 1, 1);
    for (int j = 0; j < 14; j++) {
      prices.add(daily(base.plusDays(j), "190", "215", "185", "200"));
    }
    // Downtrend price + Inverted Hammer
    // Body is 0.1 (open=110.1, close=110). Upper shadow is 2.4, lower shadow is 0.
    prices.add(daily(base.plusDays(14), "110.1", "112.5", "110", "110"));

    when(dailyPriceRepository.findByTickerOrderByPriceDateAsc(ticker)).thenReturn(prices);
    when(dailyCandlestickPatternRepository.findFirstByTickerOrderByPriceDateDesc(ticker))
        .thenReturn(Optional.empty());

    calculator.computeCandleStickPatternsForTicker(ticker);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<DailyCandlestickPattern>> captor = ArgumentCaptor.forClass(List.class);
    verify(dailyCandlestickPatternRepository).saveAll(captor.capture());
    assertThat(captor.getValue())
        .anyMatch(p -> p.getPattern() == CandlestickPattern.INVERTED_HAMMER);
  }

  @Test
  void testHangingManDetection() {
    List<DailyPrice> prices = new ArrayList<>();
    LocalDate base = LocalDate.of(2026, 1, 1);
    // Create SMA trend baseline (low prices) with body = 10
    for (int j = 0; j < 14; j++) {
      prices.add(daily(base.plusDays(j), "90", "115", "85", "100"));
    }
    // Uptrend prices: Close > SMA20 (~107)
    // Index 14: Hanging man candidate: open=200, close=200.1, high=200.1, low=197.5 (body = 0.1)
    prices.add(daily(base.plusDays(14), "200", "200.1", "197.5", "200.1"));
    // Index 15: Red confirmation candle: open=200, close=195
    prices.add(daily(base.plusDays(15), "200", "201", "194", "195"));

    when(dailyPriceRepository.findByTickerOrderByPriceDateAsc(ticker)).thenReturn(prices);
    when(dailyCandlestickPatternRepository.findFirstByTickerOrderByPriceDateDesc(ticker))
        .thenReturn(Optional.empty());

    calculator.computeCandleStickPatternsForTicker(ticker);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<DailyCandlestickPattern>> captor = ArgumentCaptor.forClass(List.class);
    verify(dailyCandlestickPatternRepository).saveAll(captor.capture());
    assertThat(captor.getValue()).anyMatch(p -> p.getPattern() == CandlestickPattern.HANGING_MAN);
  }

  @Test
  void testMorningStarDetection() {
    List<DailyPrice> prices = new ArrayList<>();
    LocalDate base = LocalDate.of(2026, 1, 1);
    for (int j = 0; j < 14; j++) {
      prices.add(daily(base.plusDays(j), "190", "215", "185", "200"));
    }
    // Downtrend + Morning Star (3 candles)
    // 1st: Large Red: open=115, close=105 (body = 10)
    prices.add(daily(base.plusDays(14), "115", "116", "104", "105"));
    // 2nd: Star (Gaps down): open=99, close=98.9 (body = 0.1)
    prices.add(daily(base.plusDays(15), "99", "99.1", "98.8", "98.9"));
    // 3rd: Large Green: open=99.5, close=112 (body = 12.5, closes above midpoint 110)
    prices.add(daily(base.plusDays(16), "99.5", "113", "99", "112"));

    when(dailyPriceRepository.findByTickerOrderByPriceDateAsc(ticker)).thenReturn(prices);
    when(dailyCandlestickPatternRepository.findFirstByTickerOrderByPriceDateDesc(ticker))
        .thenReturn(Optional.empty());

    calculator.computeCandleStickPatternsForTicker(ticker);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<DailyCandlestickPattern>> captor = ArgumentCaptor.forClass(List.class);
    verify(dailyCandlestickPatternRepository).saveAll(captor.capture());
    assertThat(captor.getValue()).anyMatch(p -> p.getPattern() == CandlestickPattern.MORNING_STAR);
  }

  @Test
  void testEveningStarDetection() {
    List<DailyPrice> prices = new ArrayList<>();
    LocalDate base = LocalDate.of(2026, 1, 1);
    for (int j = 0; j < 14; j++) {
      prices.add(daily(base.plusDays(j), "90", "115", "85", "100"));
    }
    // Uptrend + Evening Star (3 candles)
    // 1st: Large Green: open=185, close=195 (body = 10)
    prices.add(daily(base.plusDays(14), "185", "196", "184", "195"));
    // 2nd: Star (Gaps up): open=201, close=201.1 (body = 0.1)
    prices.add(daily(base.plusDays(15), "201", "201.2", "200.9", "201.1"));
    // 3rd: Large Red: open=200.5, close=188 (body = 12.5, closes below midpoint 190)
    prices.add(daily(base.plusDays(16), "200.5", "201", "187", "188"));

    when(dailyPriceRepository.findByTickerOrderByPriceDateAsc(ticker)).thenReturn(prices);
    when(dailyCandlestickPatternRepository.findFirstByTickerOrderByPriceDateDesc(ticker))
        .thenReturn(Optional.empty());

    calculator.computeCandleStickPatternsForTicker(ticker);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<DailyCandlestickPattern>> captor = ArgumentCaptor.forClass(List.class);
    verify(dailyCandlestickPatternRepository).saveAll(captor.capture());
    assertThat(captor.getValue()).anyMatch(p -> p.getPattern() == CandlestickPattern.EVENING_STAR);
  }

  @Test
  void testWeeklyCalculations() {
    List<WeeklyPrice> prices = new ArrayList<>();
    LocalDate base = LocalDate.of(2026, 1, 1);
    for (int j = 0; j < 14; j++) {
      prices.add(weekly(base.plusDays(j * 7), "100", "101", "99", "100"));
    }
    // Bullish Marubozu
    prices.add(weekly(base.plusDays(14 * 7), "100", "110", "100", "110"));

    when(weeklyPriceRepository.findByTickerOrderByPriceDateAsc(ticker)).thenReturn(prices);
    when(weeklyCandlestickPatternRepository.findFirstByTickerOrderByPriceDateDesc(ticker))
        .thenReturn(Optional.empty());

    calculator.computeCandleStickPatternsForTicker(ticker);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<WeeklyCandlestickPattern>> captor = ArgumentCaptor.forClass(List.class);
    verify(weeklyCandlestickPatternRepository).saveAll(captor.capture());
    assertThat(captor.getValue())
        .anyMatch(p -> p.getPattern() == CandlestickPattern.BULLISH_MARUBOZU);
  }

  @Test
  void testIncrementalCalculationWithLastDate() {
    List<DailyPrice> prices = new ArrayList<>();
    LocalDate base = LocalDate.of(2026, 1, 1);
    for (int j = 0; j < 14; j++) {
      prices.add(daily(base.plusDays(j), "100", "101", "99", "100"));
    }
    prices.add(daily(base.plusDays(14), "100", "110", "100", "110"));

    when(dailyPriceRepository.findByTickerOrderByPriceDateAsc(ticker)).thenReturn(prices);
    // Last pattern computed was on base.plusDays(14)
    DailyCandlestickPattern lastPat =
        DailyCandlestickPattern.builder().priceDate(base.plusDays(14)).build();
    when(dailyCandlestickPatternRepository.findFirstByTickerOrderByPriceDateDesc(ticker))
        .thenReturn(Optional.of(lastPat));

    calculator.computeCandleStickPatternsForTicker(ticker);

    verify(dailyCandlestickPatternRepository, never()).saveAll(any());
  }
}
