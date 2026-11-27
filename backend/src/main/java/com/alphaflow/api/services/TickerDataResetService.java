package com.alphaflow.api.services;

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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class TickerDataResetService {

  private final TickerRepository tickerRepository;
  private final DailyPriceRepository dailyPriceRepository;
  private final WeeklyPriceRepository weeklyPriceRepository;
  private final DailyIndicatorRepository dailyIndicatorRepository;
  private final WeeklyIndicatorRepository weeklyIndicatorRepository;
  private final DailySupportResistanceRepository dailySupportResistanceRepository;
  private final WeeklySupportResistanceRepository weeklySupportResistanceRepository;
  private final DailyCandlestickPatternRepository dailyCandlestickPatternRepository;
  private final WeeklyCandlestickPatternRepository weeklyCandlestickPatternRepository;
  private final DailyChartPatternRepository dailyChartPatternRepository;
  private final WeeklyChartPatternRepository weeklyChartPatternRepository;

  public TickerDataResetService(
      TickerRepository tickerRepository,
      DailyPriceRepository dailyPriceRepository,
      WeeklyPriceRepository weeklyPriceRepository,
      DailyIndicatorRepository dailyIndicatorRepository,
      WeeklyIndicatorRepository weeklyIndicatorRepository,
      DailySupportResistanceRepository dailySupportResistanceRepository,
      WeeklySupportResistanceRepository weeklySupportResistanceRepository,
      DailyCandlestickPatternRepository dailyCandlestickPatternRepository,
      WeeklyCandlestickPatternRepository weeklyCandlestickPatternRepository,
      DailyChartPatternRepository dailyChartPatternRepository,
      WeeklyChartPatternRepository weeklyChartPatternRepository) {
    this.tickerRepository = tickerRepository;
    this.dailyPriceRepository = dailyPriceRepository;
    this.weeklyPriceRepository = weeklyPriceRepository;
    this.dailyIndicatorRepository = dailyIndicatorRepository;
    this.weeklyIndicatorRepository = weeklyIndicatorRepository;
    this.dailySupportResistanceRepository = dailySupportResistanceRepository;
    this.weeklySupportResistanceRepository = weeklySupportResistanceRepository;
    this.dailyCandlestickPatternRepository = dailyCandlestickPatternRepository;
    this.weeklyCandlestickPatternRepository = weeklyCandlestickPatternRepository;
    this.dailyChartPatternRepository = dailyChartPatternRepository;
    this.weeklyChartPatternRepository = weeklyChartPatternRepository;
  }

  @Transactional
  public void resetTickerData(String symbol) {
    Ticker ticker =
        tickerRepository
            .findByTickerSymbolIgnoreCase(symbol)
            .orElseThrow(() -> {
              log.warn("Ticker not found for data reset: {}", symbol);
              return new ResourceNotFoundException("Ticker not found: " + symbol);
            });

    if (ticker.isStale()) {
      log.info(
          "Clearing all price and analytics data for stale ticker {}", ticker.getTickerSymbol());
      dailyChartPatternRepository.deleteByTicker(ticker);
      weeklyChartPatternRepository.deleteByTicker(ticker);
      dailyCandlestickPatternRepository.deleteByTicker(ticker);
      weeklyCandlestickPatternRepository.deleteByTicker(ticker);
      dailySupportResistanceRepository.deleteByTicker(ticker);
      weeklySupportResistanceRepository.deleteByTicker(ticker);
      dailyIndicatorRepository.deleteByTicker(ticker);
      weeklyIndicatorRepository.deleteByTicker(ticker);
      weeklyPriceRepository.deleteByTicker(ticker);
      dailyPriceRepository.deleteByTicker(ticker);

      ticker.setStale(false);
      tickerRepository.save(ticker);
      log.info(
          "Successfully cleared all data and reset stale flag for ticker {}",
          ticker.getTickerSymbol());
    } else {
      log.info("No data reset needed for ticker {}: already up to date.", ticker.getTickerSymbol());
    }
  }
}
