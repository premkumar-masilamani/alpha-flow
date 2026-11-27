package com.alphaflow.api.services;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.alphaflow.common.exceptions.ResourceNotFoundException;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.repositories.DailyCandlestickPatternRepository;
import com.alphaflow.persistence.repositories.DailyChartPatternRepository;
import com.alphaflow.persistence.repositories.DailyIndicatorRepository;
import com.alphaflow.persistence.repositories.DailyPriceRepository;
import com.alphaflow.persistence.repositories.DailySupportResistanceRepository;
import com.alphaflow.persistence.repositories.TickerRepository;
import com.alphaflow.persistence.repositories.WeeklyCandlestickPatternRepository;
import com.alphaflow.persistence.repositories.WeeklyChartPatternRepository;
import com.alphaflow.persistence.repositories.WeeklyIndicatorRepository;
import com.alphaflow.persistence.repositories.WeeklyPriceRepository;
import com.alphaflow.persistence.repositories.WeeklySupportResistanceRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TickerDataResetServiceTest {

  private TickerRepository tickerRepository;
  private DailyPriceRepository dailyPriceRepository;
  private WeeklyPriceRepository weeklyPriceRepository;
  private DailyIndicatorRepository dailyIndicatorRepository;
  private WeeklyIndicatorRepository weeklyIndicatorRepository;
  private DailySupportResistanceRepository dailySupportResistanceRepository;
  private WeeklySupportResistanceRepository weeklySupportResistanceRepository;
  private DailyCandlestickPatternRepository dailyCandlestickPatternRepository;
  private WeeklyCandlestickPatternRepository weeklyCandlestickPatternRepository;
  private DailyChartPatternRepository dailyChartPatternRepository;
  private WeeklyChartPatternRepository weeklyChartPatternRepository;
  private TickerDataResetService service;

  @BeforeEach
  void setUp() {
    tickerRepository = mock(TickerRepository.class);
    dailyPriceRepository = mock(DailyPriceRepository.class);
    weeklyPriceRepository = mock(WeeklyPriceRepository.class);
    dailyIndicatorRepository = mock(DailyIndicatorRepository.class);
    weeklyIndicatorRepository = mock(WeeklyIndicatorRepository.class);
    dailySupportResistanceRepository = mock(DailySupportResistanceRepository.class);
    weeklySupportResistanceRepository = mock(WeeklySupportResistanceRepository.class);
    dailyCandlestickPatternRepository = mock(DailyCandlestickPatternRepository.class);
    weeklyCandlestickPatternRepository = mock(WeeklyCandlestickPatternRepository.class);
    dailyChartPatternRepository = mock(DailyChartPatternRepository.class);
    weeklyChartPatternRepository = mock(WeeklyChartPatternRepository.class);

    service =
        new TickerDataResetService(
            tickerRepository,
            dailyPriceRepository,
            weeklyPriceRepository,
            dailyIndicatorRepository,
            weeklyIndicatorRepository,
            dailySupportResistanceRepository,
            weeklySupportResistanceRepository,
            dailyCandlestickPatternRepository,
            weeklyCandlestickPatternRepository,
            dailyChartPatternRepository,
            weeklyChartPatternRepository);
  }

  @Test
  void testResetStaleActiveTickerClearsAllTablesAndResetsFlag() {
    Ticker ticker =
        Ticker.builder()
            .tickerId(1L)
            .tickerSymbol("AAPL")
            .tickerName("Apple Inc.")
            .isActive(true)
            .isStale(true)
            .build();
    when(tickerRepository.findByTickerSymbolIgnoreCase("aapl")).thenReturn(Optional.of(ticker));

    service.resetTickerData("aapl");

    assertFalse(ticker.isStale());

    verify(dailyChartPatternRepository).deleteByTicker(ticker);
    verify(weeklyChartPatternRepository).deleteByTicker(ticker);
    verify(dailyCandlestickPatternRepository).deleteByTicker(ticker);
    verify(weeklyCandlestickPatternRepository).deleteByTicker(ticker);
    verify(dailySupportResistanceRepository).deleteByTicker(ticker);
    verify(weeklySupportResistanceRepository).deleteByTicker(ticker);
    verify(dailyIndicatorRepository).deleteByTicker(ticker);
    verify(weeklyIndicatorRepository).deleteByTicker(ticker);
    verify(weeklyPriceRepository).deleteByTicker(ticker);
    verify(dailyPriceRepository).deleteByTicker(ticker);
    verify(tickerRepository).save(ticker);
  }

  @Test
  void testResetStaleInactiveTickerAlsoClearsAllTables() {
    Ticker ticker =
        Ticker.builder()
            .tickerId(2L)
            .tickerSymbol("OLD")
            .tickerName("Inactive Corp")
            .isActive(false)
            .isStale(true)
            .build();
    when(tickerRepository.findByTickerSymbolIgnoreCase("OLD")).thenReturn(Optional.of(ticker));

    service.resetTickerData("OLD");

    assertFalse(ticker.isStale());
    verify(dailyPriceRepository).deleteByTicker(ticker);
    verify(tickerRepository).save(ticker);
  }

  @Test
  void testResetUpToDateTickerReturnsNoOp() {
    Ticker ticker =
        Ticker.builder()
            .tickerId(3L)
            .tickerSymbol("MSFT")
            .tickerName("Microsoft")
            .isActive(true)
            .isStale(false)
            .build();
    when(tickerRepository.findByTickerSymbolIgnoreCase("MSFT")).thenReturn(Optional.of(ticker));

    service.resetTickerData("MSFT");

    verify(dailyPriceRepository, never()).deleteByTicker(any());
    verify(weeklyPriceRepository, never()).deleteByTicker(any());
    verify(tickerRepository, never()).save(any());
  }

  @Test
  void testResetUnknownTickerThrowsResourceNotFoundException() {
    when(tickerRepository.findByTickerSymbolIgnoreCase("UNKNOWN")).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> service.resetTickerData("UNKNOWN"));
  }
}
