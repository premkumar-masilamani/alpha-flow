package com.alphaflow.api.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.alphaflow.api.configs.ChartConfig;
import com.alphaflow.api.dtos.IndicatorConfigDTO;
import com.alphaflow.api.dtos.IndicatorSeriesDTO;
import com.alphaflow.engine.configs.IndicatorConfig;
import com.alphaflow.engine.configs.IndicatorConfig.IndicatorDefinition;
import com.alphaflow.persistence.entities.IndicatorValue;
import com.alphaflow.persistence.enums.IndicatorType;
import com.alphaflow.persistence.enums.PriceSource;
import com.alphaflow.persistence.enums.Timeframe;
import com.alphaflow.persistence.exceptions.ResourceNotFoundException;
import com.alphaflow.persistence.repositories.DailyPriceRepository;
import com.alphaflow.persistence.repositories.IndicatorValueRepository;
import com.alphaflow.persistence.repositories.TickerRepository;
import com.alphaflow.persistence.repositories.WeeklyPriceRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
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

  private IndicatorValueRepository indicatorValueRepository;

  private IndicatorService service;

  private static IndicatorDefinition def(
      IndicatorType type, PriceSource source, Map<String, Integer> params) {

    IndicatorDefinition d = new IndicatorDefinition();

    d.setType(type);

    d.setSource(source);

    d.setParams(params);

    return d;
  }

  private static IndicatorValue value(
      IndicatorType type, String params, String output, LocalDate date, String value) {

    return IndicatorValue.builder()
        .indicatorType(type)
        .source(PriceSource.CLOSE)
        .params(params)
        .outputName(output)
        .priceDate(date)
        .value(new BigDecimal(value))
        .build();
  }

  @BeforeEach
  void setUp() {

    indicatorConfig = new IndicatorConfig();

    tickerRepository = mock(TickerRepository.class);

    dailyPriceRepository = mock(DailyPriceRepository.class);

    weeklyPriceRepository = mock(WeeklyPriceRepository.class);

    indicatorValueRepository = mock(IndicatorValueRepository.class);

    service =
        new IndicatorService(
            indicatorConfig,
            new ChartConfig(),
            tickerRepository,
            dailyPriceRepository,
            weeklyPriceRepository,
            indicatorValueRepository);
  }

  @Test
  void discoveryFlattensConfiguredMatrix() {

    indicatorConfig.setTimeframes(
        Map.of(
            Timeframe.DAILY,
            List.of(def(IndicatorType.EMA, PriceSource.CLOSE, Map.of("period", 5))),
            Timeframe.WEEKLY,
            List.of(
                def(
                    IndicatorType.MACD,
                    PriceSource.CLOSE,
                    Map.of("fast", 12, "slow", 26, "signal", 9)))));

    List<IndicatorConfigDTO> configs = service.getConfiguredIndicators();

    assertEquals(2, configs.size());

    IndicatorConfigDTO ema =
        configs.stream().filter(c -> c.type().equals("EMA")).findFirst().orElseThrow();

    assertEquals("DAILY", ema.timeframe());

    assertEquals("period=5", ema.params());

    assertEquals("EMA(5)", ema.label());

    IndicatorConfigDTO macd =
        configs.stream().filter(c -> c.type().equals("MACD")).findFirst().orElseThrow();

    assertEquals("WEEKLY", macd.timeframe());

    assertEquals("MACD(12,26,9)", macd.label());
  }

  @Test
  void seriesGroupsByComboAndDate() {

    when(tickerRepository.existsByTickerSymbolIgnoreCase("TEST")).thenReturn(true);

    when(dailyPriceRepository.findRecentPriceDates(eq("TEST"), any()))
        .thenReturn(List.of(D3, D2, D1));

    when(indicatorValueRepository.findSeriesBetween(
            eq("TEST"), eq(Timeframe.DAILY), eq(D1), eq(D3)))
        .thenReturn(
            List.of(
                value(IndicatorType.EMA, "period=5", "value", D1, "10.0"),
                value(IndicatorType.MACD, "fast=12,signal=9,slow=26", "macd", D1, "1.0"),
                value(IndicatorType.MACD, "fast=12,signal=9,slow=26", "signal", D1, "0.5"),
                value(IndicatorType.MACD, "fast=12,signal=9,slow=26", "histogram", D1, "0.5"),
                value(IndicatorType.EMA, "period=5", "value", D2, "11.0"),
                value(IndicatorType.MACD, "fast=12,signal=9,slow=26", "macd", D2, "1.2"),
                value(IndicatorType.MACD, "fast=12,signal=9,slow=26", "signal", D2, "0.6"),
                value(IndicatorType.MACD, "fast=12,signal=9,slow=26", "histogram", D2, "0.6")));

    List<IndicatorSeriesDTO> series = service.getIndicatorSeries("TEST", Timeframe.DAILY);

    assertEquals(2, series.size());

    IndicatorSeriesDTO ema =
        series.stream().filter(s -> s.type().equals("EMA")).findFirst().orElseThrow();

    assertEquals(2, ema.points().size());

    assertEquals(Map.of("value", new BigDecimal("10.0")), ema.points().getFirst().values());

    IndicatorSeriesDTO macd =
        series.stream().filter(s -> s.type().equals("MACD")).findFirst().orElseThrow();

    assertEquals("MACD(12,26,9)", macd.label());

    assertEquals(2, macd.points().size());

    // Each MACD bar carries all three plots together.

    Map<String, BigDecimal> firstBar = macd.points().getFirst().values();

    assertEquals(3, firstBar.size());

    assertTrue(firstBar.keySet().containsAll(List.of("macd", "signal", "histogram")));
  }

  @Test
  void seriesThrowsWhenTickerMissing() {

    when(tickerRepository.existsByTickerSymbolIgnoreCase("NOPE")).thenReturn(false);

    assertThrows(
        ResourceNotFoundException.class, () -> service.getIndicatorSeries("NOPE", Timeframe.DAILY));

    verify(indicatorValueRepository, never()).findSeriesBetween(any(), any(), any(), any());
  }

  // ---- helpers --------------------------------------------------------

  @Test
  void seriesReturnsEmptyWhenNoPriceHistory() {

    when(tickerRepository.existsByTickerSymbolIgnoreCase("TEST")).thenReturn(true);

    when(dailyPriceRepository.findRecentPriceDates(eq("TEST"), any())).thenReturn(List.of());

    assertTrue(service.getIndicatorSeries("TEST", Timeframe.DAILY).isEmpty());

    verify(indicatorValueRepository, never()).findSeriesBetween(any(), any(), any(), any());
  }

  @Test
  void seriesSupportsWeeklyTimeframe() {

    when(tickerRepository.existsByTickerSymbolIgnoreCase("TEST")).thenReturn(true);

    when(weeklyPriceRepository.findRecentPriceDates(eq("TEST"), any())).thenReturn(List.of(D1));

    when(indicatorValueRepository.findSeriesBetween(
            eq("TEST"), eq(Timeframe.WEEKLY), eq(D1), eq(D1)))
        .thenReturn(List.of());

    List<IndicatorSeriesDTO> result = service.getIndicatorSeries("TEST", Timeframe.WEEKLY);

    assertTrue(result.isEmpty());

    verify(weeklyPriceRepository).findRecentPriceDates(eq("TEST"), any());
  }

  @Test
  void seriesSupportsCustomPageAndSize() {

    when(tickerRepository.existsByTickerSymbolIgnoreCase("TEST")).thenReturn(true);

    when(dailyPriceRepository.findRecentPriceDates(eq("TEST"), eq(PageRequest.of(1, 10))))
        .thenReturn(List.of(D1));

    when(indicatorValueRepository.findSeriesBetween(
            eq("TEST"), eq(Timeframe.DAILY), eq(D1), eq(D1)))
        .thenReturn(List.of());

    List<IndicatorSeriesDTO> result = service.getIndicatorSeries("TEST", Timeframe.DAILY, 1, 10);

    assertTrue(result.isEmpty());

    verify(dailyPriceRepository).findRecentPriceDates(eq("TEST"), eq(PageRequest.of(1, 10)));
  }
}
