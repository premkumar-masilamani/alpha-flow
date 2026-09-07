package com.alphaflow.api.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.alphaflow.api.dtos.IndicatorConfigDto;
import com.alphaflow.api.dtos.IndicatorSeriesDto;
import com.alphaflow.common.enums.Timeframe;
import com.alphaflow.engine.configs.IndicatorConfig;
import com.alphaflow.engine.indicators.dtos.IndicatorParams;
import com.alphaflow.persistence.entities.DailyIndicator;
import com.alphaflow.persistence.entities.Indicator;
import com.alphaflow.persistence.entities.IndicatorDefinition;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.enums.IndicatorType;
import com.alphaflow.persistence.enums.PriceSource;
import com.alphaflow.persistence.repositories.DailyIndicatorRepository;
import com.alphaflow.persistence.repositories.DailyPriceRepository;
import com.alphaflow.persistence.repositories.TickerRepository;
import com.alphaflow.persistence.repositories.WeeklyIndicatorRepository;
import com.alphaflow.persistence.repositories.WeeklyPriceRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

class IndicatorServiceTest {

  private static final LocalDate D1 = LocalDate.of(2024, 1, 1);
  private static final LocalDate D2 = LocalDate.of(2024, 1, 2);
  private static final LocalDate D3 = LocalDate.of(2024, 1, 3);

  private IndicatorConfig indicatorConfig;
  private TickerRepository tickerRepository;
  private DailyPriceRepository dailyPriceRepository;
  private WeeklyPriceRepository weeklyPriceRepository;
  private DailyIndicatorRepository dailyIndicatorRepository;
  private WeeklyIndicatorRepository weeklyIndicatorRepository;
  private IndicatorService indicatorService;
  private Ticker ticker;

  private static IndicatorDefinition def(
      IndicatorType type, PriceSource source, Map<String, Integer> params) {
    return IndicatorDefinition.builder().indicatorType(type).source(source).params(params).build();
  }

  private static Indicator value(
      IndicatorType type, String params, LocalDate date, Map<String, String> values) {
    IndicatorDefinition indicatorDef =
        IndicatorDefinition.builder()
            .indicatorType(type)
            .source(PriceSource.CLOSE)
            .params(IndicatorParams.parse(params).getValues())
            .build();

    Map<String, BigDecimal> decimalValues = new LinkedHashMap<>();
    values.forEach((k, v) -> decimalValues.put(k, new BigDecimal(v)));

    return DailyIndicator.builder()
        .indicatorDefinition(indicatorDef)
        .priceDate(date)
        .values(decimalValues)
        .build();
  }

  @BeforeEach
  void setUp() {
    indicatorConfig = mock(IndicatorConfig.class);
    tickerRepository = mock(TickerRepository.class);
    dailyPriceRepository = mock(DailyPriceRepository.class);
    weeklyPriceRepository = mock(WeeklyPriceRepository.class);
    dailyIndicatorRepository = mock(DailyIndicatorRepository.class);
    weeklyIndicatorRepository = mock(WeeklyIndicatorRepository.class);

    indicatorService =
        new IndicatorService(
            indicatorConfig,
            tickerRepository,
            dailyPriceRepository,
            weeklyPriceRepository,
            dailyIndicatorRepository,
            weeklyIndicatorRepository);

    ticker = Ticker.builder().tickerSymbol("TEST").isActive(true).build();
  }

  @Test
  void discoveryFlattensConfiguredMatrix() {
    when(indicatorConfig.getDefinitions())
        .thenReturn(
            List.of(
                def(IndicatorType.EMA, PriceSource.CLOSE, Map.of("period", 5)),
                def(
                    IndicatorType.MACD,
                    PriceSource.CLOSE,
                    Map.of("fast", 12, "slow", 26, "signal", 9))));

    List<IndicatorConfigDto> configs = indicatorService.getConfiguredIndicators();

    assertEquals(4, configs.size());

    List<IndicatorConfigDto> dailyConfigs =
        configs.stream().filter(c -> c.timeframe().equals("DAILY")).toList();
    assertEquals(2, dailyConfigs.size());
    assertTrue(dailyConfigs.stream().anyMatch(c -> c.type().equals("EMA")));
    assertTrue(dailyConfigs.stream().anyMatch(c -> c.type().equals("MACD")));

    List<IndicatorConfigDto> weeklyConfigs =
        configs.stream().filter(c -> c.timeframe().equals("WEEKLY")).toList();
    assertEquals(2, weeklyConfigs.size());
    assertTrue(weeklyConfigs.stream().anyMatch(c -> c.type().equals("EMA")));
    assertTrue(weeklyConfigs.stream().anyMatch(c -> c.type().equals("MACD")));
  }

  @Test
  void seriesGroupsByComboAndDate() {
    when(dailyPriceRepository.findRecentPriceDatesUpTo(eq(ticker), any(LocalDate.class), any()))
        .thenReturn(List.of(D3, D2, D1));

    when(indicatorConfig.getDefinitions())
        .thenReturn(List.of(def(IndicatorType.EMA, PriceSource.CLOSE, Map.of("period", 5))));

    doReturn(
            List.of(
                value(IndicatorType.EMA, "period=5", D1, Map.of("value", "10.0")),
                value(
                    IndicatorType.MACD,
                    "fast=12,signal=9,slow=26",
                    D1,
                    Map.of("macd", "1.0", "signal", "0.5", "histogram", "0.5")),
                value(IndicatorType.EMA, "period=5", D2, Map.of("value", "11.0")),
                value(
                    IndicatorType.MACD,
                    "fast=12,signal=9,slow=26",
                    D2,
                    Map.of("macd", "1.2", "signal", "0.6", "histogram", "0.6"))))
        .when(dailyIndicatorRepository)
        .findSeriesBetween(eq(ticker), any(), eq(D1), eq(D3));

    List<IndicatorSeriesDto> series =
        indicatorService.getIndicatorSeries(ticker, Timeframe.DAILY, 0, 250);

    assertEquals(2, series.size());

    IndicatorSeriesDto ema =
        series.stream().filter(s -> s.type().equals("EMA")).findFirst().orElseThrow();

    assertEquals(2, ema.points().size());
    assertEquals(Map.of("value", new BigDecimal("10.0")), ema.points().getFirst().values());

    IndicatorSeriesDto macd =
        series.stream().filter(s -> s.type().equals("MACD")).findFirst().orElseThrow();

    assertEquals("MACD (12,26,9)", macd.label());
    assertEquals(2, macd.points().size());

    Map<String, BigDecimal> firstBar = macd.points().getFirst().values();

    assertEquals(3, firstBar.size());
    assertTrue(firstBar.keySet().containsAll(List.of("macd", "signal", "histogram")));
  }

  @Test
  void seriesReturnsEmptyWhenNoPriceHistory() {
    when(dailyPriceRepository.findRecentPriceDatesUpTo(eq(ticker), any(LocalDate.class), any()))
        .thenReturn(List.of());

    assertTrue(indicatorService.getIndicatorSeries(ticker, Timeframe.DAILY, 0, 250).isEmpty());

    verify(dailyIndicatorRepository, never()).findSeriesBetween(any(), any(), any(), any());
    verify(weeklyIndicatorRepository, never()).findSeriesBetween(any(), any(), any(), any());
  }

  @Test
  void seriesSupportsWeeklyTimeframe() {
    when(weeklyPriceRepository.findRecentPriceDatesUpTo(eq(ticker), any(LocalDate.class), any()))
        .thenReturn(List.of(D1));

    when(indicatorConfig.getDefinitions())
        .thenReturn(List.of(def(IndicatorType.EMA, PriceSource.CLOSE, Map.of("period", 2))));

    doReturn(List.of())
        .when(weeklyIndicatorRepository)
        .findSeriesBetween(eq(ticker), any(), eq(D1), eq(D1));

    List<IndicatorSeriesDto> result =
        indicatorService.getIndicatorSeries(ticker, Timeframe.WEEKLY, 0, 250);

    assertTrue(result.isEmpty());
    verify(weeklyPriceRepository).findRecentPriceDatesUpTo(eq(ticker), any(LocalDate.class), any());
  }

  @Test
  void seriesSupportsCustomPageAndSize() {
    when(dailyPriceRepository.findRecentPriceDatesUpTo(
            eq(ticker), any(LocalDate.class), eq(PageRequest.of(1, 10))))
        .thenReturn(List.of(D1));

    when(indicatorConfig.getDefinitions())
        .thenReturn(List.of(def(IndicatorType.EMA, PriceSource.CLOSE, Map.of("period", 2))));

    doReturn(List.of())
        .when(dailyIndicatorRepository)
        .findSeriesBetween(eq(ticker), any(), eq(D1), eq(D1));

    List<IndicatorSeriesDto> result =
        indicatorService.getIndicatorSeries(ticker, Timeframe.DAILY, 1, 10);

    assertTrue(result.isEmpty());
    verify(dailyPriceRepository)
        .findRecentPriceDatesUpTo(eq(ticker), any(LocalDate.class), eq(PageRequest.of(1, 10)));
  }

  @Test
  void seriesReturnsEmptyWhenNoDefinitions() {
    when(dailyPriceRepository.findRecentPriceDatesUpTo(eq(ticker), any(LocalDate.class), any()))
        .thenReturn(List.of(D1));
    when(indicatorConfig.getDefinitions()).thenReturn(List.of());

    assertTrue(indicatorService.getIndicatorSeries(ticker, Timeframe.DAILY, 0, 250).isEmpty());

    verify(dailyIndicatorRepository, never()).findSeriesBetween(any(), any(), any(), any());
    verify(weeklyIndicatorRepository, never()).findSeriesBetween(any(), any(), any(), any());
  }

  @Test
  void seriesThrowsOnUnsupportedTimeframe() {
    org.junit.jupiter.api.Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> indicatorService.getIndicatorSeries(ticker, null, 0, 250));
  }
}
