package com.alphaflow.api.services;

import com.alphaflow.api.configs.ChartConfig;
import com.alphaflow.api.dtos.OhlcvDTO;
import com.alphaflow.api.mappers.OhlcvMapper;
import com.alphaflow.persistence.entities.WeeklyPrice;
import com.alphaflow.persistence.exceptions.ResourceNotFoundException;
import com.alphaflow.persistence.repositories.TickerRepository;
import com.alphaflow.persistence.repositories.WeeklyPriceRepository;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serves the weekly OHLCV series for a ticker — the weekly mirror of {@link DailyPriceService},
 * reading {@code weekly_prices} and capping at the configured weekly window.
 */
@Service
@Transactional(readOnly = true)
public class WeeklyPriceService {

  private static final Logger log = LoggerFactory.getLogger(WeeklyPriceService.class);

  private final WeeklyPriceRepository weeklyPriceRepository;
  private final TickerRepository tickerRepository;
  private final ChartConfig chartConfig;

  public WeeklyPriceService(
      WeeklyPriceRepository weeklyPriceRepository,
      TickerRepository tickerRepository,
      ChartConfig chartConfig) {
    this.weeklyPriceRepository = weeklyPriceRepository;
    this.tickerRepository = tickerRepository;
    this.chartConfig = chartConfig;
  }

  public List<OhlcvDTO> getWeeklyPriceByTickerName(String tickerName) {
    return getWeeklyPriceByTickerName(tickerName, 0, null);
  }

  public List<OhlcvDTO> getWeeklyPriceByTickerName(String tickerName, int page, Integer size) {
    int window = chartConfig.getWindow();
    int actualSize = size != null ? Math.min(size, window * 5) : window;
    if (actualSize < 1) {
      actualSize = 1;
    }
    log.debug(
        "Fetching weekly candle data for ticker: {} (page={}, size={})",
        tickerName,
        page,
        actualSize);
    if (!tickerRepository.existsByTickerSymbolIgnoreCase(tickerName)) {
      log.warn("Ticker not found for symbol: {}", tickerName);
      throw new ResourceNotFoundException("Ticker not found: " + tickerName);
    }
    return weeklyPriceRepository
        .findLatestByTickerName(tickerName, PageRequest.of(page, actualSize))
        .stream()
        .sorted(Comparator.comparing(WeeklyPrice::getPriceDate))
        .map(OhlcvMapper::toDTO)
        .toList();
  }
}
