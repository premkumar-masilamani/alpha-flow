package com.alphaflow.engine.calculators;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.alphaflow.persistence.entities.*;
import com.alphaflow.persistence.enums.*;
import com.alphaflow.persistence.repositories.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SupportResistanceCalculatorTest {

  private TickerRepository tickerRepository;
  private DailyPriceRepository dailyPriceRepository;
  private WeeklyPriceRepository weeklyPriceRepository;
  private DailySupportResistanceRepository dailySrRepo;
  private WeeklySupportResistanceRepository weeklySrRepo;
  private SupportResistanceCalculator calculator;

  @BeforeEach
  void setUp() {
    tickerRepository = mock(TickerRepository.class);
    dailyPriceRepository = mock(DailyPriceRepository.class);
    weeklyPriceRepository = mock(WeeklyPriceRepository.class);
    dailySrRepo = mock(DailySupportResistanceRepository.class);
    weeklySrRepo = mock(WeeklySupportResistanceRepository.class);

    calculator =
        new SupportResistanceCalculator(
            tickerRepository,
            dailyPriceRepository,
            weeklyPriceRepository,
            dailySrRepo,
            weeklySrRepo);
  }

  private DailyPrice mockPrice(LocalDate date, double high, double low, double close) {
    DailyPrice dp = mock(DailyPrice.class);
    when(dp.getPriceDate()).thenReturn(date);
    when(dp.getPriceHigh()).thenReturn(BigDecimal.valueOf(high));
    when(dp.getPriceLow()).thenReturn(BigDecimal.valueOf(low));
    when(dp.getPriceClose()).thenReturn(BigDecimal.valueOf(close));
    when(dp.getPriceOpen()).thenReturn(BigDecimal.valueOf(close));
    when(dp.getVolume()).thenReturn(new BigDecimal("1000"));
    return dp;
  }

  @Test
  void testComputeForTicker_NoData() {
    Ticker ticker = new Ticker();
    ticker.setTickerSymbol("AAPL");
    when(dailyPriceRepository.findByTickerOrderByPriceDateAsc(ticker)).thenReturn(List.of());
    when(weeklyPriceRepository.findByTickerOrderByPriceDateAsc(ticker)).thenReturn(List.of());

    calculator.computeForTicker(ticker);

    verify(dailySrRepo, times(1)).deleteByTicker_TickerSymbol("AAPL");
    verify(weeklySrRepo, times(1)).deleteByTicker_TickerSymbol("AAPL");
    verify(dailySrRepo, never()).saveAll(any());
  }

  @Test
  @SuppressWarnings("unchecked")
  void testComputeForTicker_HorizontalResistance() {
    Ticker ticker = new Ticker();
    ticker.setTickerSymbol("AAPL");
    
    List<DailyPrice> prices = new ArrayList<>();
    LocalDate startDate = LocalDate.now().minusDays(100);
    
    for (int i = 0; i < 45; i++) {
        double high = 140;
        double low = 130;
        double close = 135;
        
        // Create 3 pivot highs at index 11, 22, 33
        if (i == 11 || i == 22 || i == 33) {
            high = 150;
            close = 145;
        }
        
        // Final bar needs a close near 150 to pass the 20% circuit breaker for the 150 line
        if (i == 44) {
            close = 145;
        }
        
        prices.add(mockPrice(startDate.plusDays(i), high, low, close));
    }

    when(dailyPriceRepository.findByTickerOrderByPriceDateAsc(ticker)).thenReturn(prices);
    when(weeklyPriceRepository.findByTickerOrderByPriceDateAsc(ticker)).thenReturn(List.of());

    calculator.computeForTicker(ticker);

    ArgumentCaptor<List<DailySupportResistance>> captor = ArgumentCaptor.forClass(List.class);
    verify(dailySrRepo, times(1)).saveAll(captor.capture());
    
    List<DailySupportResistance> savedLines = captor.getValue();
    assertFalse(savedLines.isEmpty());
    
    // Check if we have our horizontal resistance line
    boolean foundResistance = false;
    for (DailySupportResistance sr : savedLines) {
        if (sr.getFilterReason() == null && sr.getSlope().compareTo(BigDecimal.ZERO) == 0 && sr.getCurrentType() == SRCurrentType.RESISTANCE) {
            if (sr.getImportance() >= 3) {
                foundResistance = true;
            }
        }
    }
    assertTrue(foundResistance, "Should have identified at least 1 valid horizontal resistance line with 3 touches");
  }

  @Test
  @SuppressWarnings("unchecked")
  void testComputeForTicker_BreakInLastWindow() {
    Ticker ticker = new Ticker();
    ticker.setTickerSymbol("AAPL");
    
    List<DailyPrice> prices = new ArrayList<>();
    LocalDate startDate = LocalDate.now().minusDays(100);
    
    for (int i = 0; i < 45; i++) {
        double high = 140;
        double low = 130;
        double close = 135;
        
        // Create 3 pivot highs at index 11, 22, 33 (Price 150)
        if (i == 11 || i == 22 || i == 33) {
            high = 150;
            close = 145;
        }
        
        // Final bar needs a close > 150 to break the resistance, 
        // but within 20% of 150 to pass the circuit breaker (e.g. 160)
        if (i == 44) {
            high = 165;
            close = 160;
        }
        
        prices.add(mockPrice(startDate.plusDays(i), high, low, close));
    }

    when(dailyPriceRepository.findByTickerOrderByPriceDateAsc(ticker)).thenReturn(prices);
    when(weeklyPriceRepository.findByTickerOrderByPriceDateAsc(ticker)).thenReturn(List.of());

    calculator.computeForTicker(ticker);

    ArgumentCaptor<List<DailySupportResistance>> captor = ArgumentCaptor.forClass(List.class);
    verify(dailySrRepo, times(1)).saveAll(captor.capture());
    
    List<DailySupportResistance> savedLines = captor.getValue();
    
    // Due to the break at index 43 (which is inside the last window), the line should have flipped to SUPPORT
    // and/or recorded a break count.
    System.out.println("DEBUG SAVED LINES:");
    for (DailySupportResistance sr : savedLines) {
        System.out.println(sr.getIntercept() + " " + sr.getCurrentType() + " " + sr.getFilterReason() + " " + sr.getBreakCount() + " " + sr.getImportance());
    }

    boolean foundFlippedSupport = false;
    for (DailySupportResistance sr : savedLines) {
        if (sr.getFilterReason() == null && sr.getSlope().compareTo(BigDecimal.ZERO) == 0) {
            if (sr.getCurrentType() == SRCurrentType.SUPPORT && sr.getBreakCount() > 0) {
                foundFlippedSupport = true;
            }
        }
    }
    assertTrue(foundFlippedSupport, "The resistance line should have flipped to support due to the break in the last window");
  }
}
