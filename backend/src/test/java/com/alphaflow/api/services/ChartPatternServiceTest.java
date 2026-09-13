package com.alphaflow.api.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.alphaflow.api.dtos.ChartPatternDto;
import com.alphaflow.common.enums.Timeframe;
import com.alphaflow.persistence.entities.ChartPatternPivot;
import com.alphaflow.persistence.entities.DailyChartPattern;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.entities.WeeklyChartPattern;
import com.alphaflow.persistence.enums.ChartPatternStatus;
import com.alphaflow.persistence.enums.ChartPatternType;
import com.alphaflow.persistence.repositories.DailyChartPatternRepository;
import com.alphaflow.persistence.repositories.WeeklyChartPatternRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

class ChartPatternServiceTest {

  private DailyChartPatternRepository dailyChartPatternRepository;
  private WeeklyChartPatternRepository weeklyChartPatternRepository;
  private ChartPatternService service;
  private Ticker ticker;

  @BeforeEach
  void setUp() {
    dailyChartPatternRepository = mock(DailyChartPatternRepository.class);
    weeklyChartPatternRepository = mock(WeeklyChartPatternRepository.class);
    service = new ChartPatternService(dailyChartPatternRepository, weeklyChartPatternRepository);
    ticker = Ticker.builder().tickerId(1L).tickerSymbol("AAPL").build();
  }

  @Test
  void testGetPatternsDailyWithStatus() {
    DailyChartPattern pattern =
        DailyChartPattern.builder()
            .id(10L)
            .ticker(ticker)
            .patternType(ChartPatternType.HEAD_AND_SHOULDERS)
            .sentiment(ChartPatternType.HEAD_AND_SHOULDERS.getSentiment())
            .status(ChartPatternStatus.COMPLETED)
            .startDate(LocalDate.of(2026, 1, 10))
            .endDate(LocalDate.of(2026, 2, 20))
            .breakoutDate(LocalDate.of(2026, 2, 22))
            .necklinePrice(new BigDecimal("150.0000"))
            .necklineSlope(new BigDecimal("0.0000"))
            .targetPrice(new BigDecimal("140.0000"))
            .stopLossPrice(new BigDecimal("158.0000"))
            .invalidationPrice(new BigDecimal("160.0000"))
            .pivotPoints(
                List.of(
                    ChartPatternPivot.builder()
                        .date(LocalDate.of(2026, 1, 10))
                        .price(new BigDecimal("155.0000"))
                        .type("HIGH")
                        .role("LEFT_SHOULDER")
                        .build()))
            .build();

    when(dailyChartPatternRepository.findByTickerAndStatusInOrderByEndDateDesc(
            eq(ticker), eq(List.of(ChartPatternStatus.COMPLETED)), any(PageRequest.class)))
        .thenReturn(new PageImpl<>(List.of(pattern)));

    List<ChartPatternDto> result =
        service.getPatterns(ticker, Timeframe.DAILY, List.of(ChartPatternStatus.COMPLETED), 0, 10);

    assertEquals(1, result.size());
    ChartPatternDto dto = result.getFirst();
    assertEquals("HNS", dto.shortName());
    assertEquals("Head and Shoulders", dto.displayName());
    assertEquals(ChartPatternStatus.COMPLETED, dto.status());
    assertEquals(new BigDecimal("140.0000"), dto.targetPrice());
    assertEquals(new BigDecimal("158.0000"), dto.stopLossPrice());
    assertEquals(new BigDecimal("160.0000"), dto.invalidationPrice());
    assertEquals(1, dto.pivotPoints().size());
  }

  @Test
  void testGetPatternsDailyWithoutStatusDefaultsToInProgressAndCompleted() {
    DailyChartPattern pattern =
        DailyChartPattern.builder()
            .id(11L)
            .ticker(ticker)
            .patternType(ChartPatternType.DOUBLE_TOP)
            .sentiment(ChartPatternType.DOUBLE_TOP.getSentiment())
            .status(ChartPatternStatus.IN_PROGRESS)
            .startDate(LocalDate.of(2026, 3, 1))
            .endDate(LocalDate.of(2026, 3, 15))
            .targetPrice(new BigDecimal("90.0000"))
            .stopLossPrice(new BigDecimal("105.0000"))
            .pivotPoints(null)
            .build();

    when(dailyChartPatternRepository.findByTickerAndStatusInOrderByEndDateDesc(
            eq(ticker),
            eq(List.of(ChartPatternStatus.IN_PROGRESS, ChartPatternStatus.COMPLETED)),
            any(PageRequest.class)))
        .thenReturn(new PageImpl<>(List.of(pattern)));

    List<ChartPatternDto> result = service.getPatterns(ticker, Timeframe.DAILY, null, 0, 10);

    assertEquals(1, result.size());
    ChartPatternDto dto = result.getFirst();
    assertEquals("DTP", dto.shortName());
    assertEquals(new BigDecimal("105.0000"), dto.stopLossPrice());
    assertNotNull(dto.pivotPoints());
    assertEquals(0, dto.pivotPoints().size());
  }

  @Test
  void testGetPatternsWeeklyWithStatus() {
    WeeklyChartPattern pattern =
        WeeklyChartPattern.builder()
            .id(20L)
            .ticker(ticker)
            .patternType(ChartPatternType.DOUBLE_BOTTOM)
            .sentiment(ChartPatternType.DOUBLE_BOTTOM.getSentiment())
            .status(ChartPatternStatus.TARGET_REACHED)
            .startDate(LocalDate.of(2026, 1, 5))
            .endDate(LocalDate.of(2026, 2, 9))
            .stopLossPrice(new BigDecimal("98.0000"))
            .pivotPoints(
                List.of(
                    ChartPatternPivot.builder()
                        .date(LocalDate.of(2026, 1, 5))
                        .price(new BigDecimal("100.0000"))
                        .type("LOW")
                        .role("TROUGH_1")
                        .build()))
            .build();

    when(weeklyChartPatternRepository.findByTickerAndStatusInOrderByEndDateDesc(
            eq(ticker), eq(List.of(ChartPatternStatus.TARGET_REACHED)), any(PageRequest.class)))
        .thenReturn(new PageImpl<>(List.of(pattern)));

    List<ChartPatternDto> result =
        service.getPatterns(
            ticker, Timeframe.WEEKLY, List.of(ChartPatternStatus.TARGET_REACHED), 0, 10);

    assertEquals(1, result.size());
    assertEquals("DBM", result.getFirst().shortName());
    assertEquals(new BigDecimal("98.0000"), result.getFirst().stopLossPrice());
  }

  @Test
  void testGetPatternsWeeklyWithoutStatusDefaultsToInProgressAndCompleted() {
    WeeklyChartPattern pattern =
        WeeklyChartPattern.builder()
            .id(21L)
            .ticker(ticker)
            .patternType(ChartPatternType.ASCENDING_TRIANGLE)
            .sentiment(ChartPatternType.ASCENDING_TRIANGLE.getSentiment())
            .status(ChartPatternStatus.IN_PROGRESS)
            .startDate(LocalDate.of(2026, 2, 2))
            .endDate(LocalDate.of(2026, 3, 2))
            .stopLossPrice(new BigDecimal("112.0000"))
            .pivotPoints(null)
            .build();

    when(weeklyChartPatternRepository.findByTickerAndStatusInOrderByEndDateDesc(
            eq(ticker),
            eq(List.of(ChartPatternStatus.IN_PROGRESS, ChartPatternStatus.COMPLETED)),
            any(PageRequest.class)))
        .thenReturn(new PageImpl<>(List.of(pattern)));

    List<ChartPatternDto> result = service.getPatterns(ticker, Timeframe.WEEKLY, null, 0, 10);

    assertEquals(1, result.size());
    assertEquals("AST", result.getFirst().shortName());
    assertEquals(new BigDecimal("112.0000"), result.getFirst().stopLossPrice());
    assertEquals(0, result.getFirst().pivotPoints().size());
  }

  @Test
  void testGetPatternsUnsupportedTimeframe() {
    assertThrows(
        IllegalArgumentException.class, () -> service.getPatterns(ticker, null, null, 0, 10));
  }
}
