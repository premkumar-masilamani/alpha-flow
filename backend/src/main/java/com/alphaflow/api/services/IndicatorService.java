package com.alphaflow.api.services;

import com.alphaflow.api.configs.ChartConfig;
import com.alphaflow.api.dtos.IndicatorConfigDTO;
import com.alphaflow.api.dtos.IndicatorSeriesDTO;
import com.alphaflow.api.mappers.IndicatorMapper;
import com.alphaflow.engine.configs.IndicatorConfig;
import com.alphaflow.engine.configs.IndicatorConfig.IndicatorDefinition;
import com.alphaflow.persistence.entities.IndicatorValue;
import com.alphaflow.persistence.enums.Timeframe;
import com.alphaflow.persistence.exceptions.ResourceNotFoundException;
import com.alphaflow.persistence.repositories.DailyPriceRepository;
import com.alphaflow.persistence.repositories.IndicatorValueRepository;
import com.alphaflow.persistence.repositories.TickerRepository;
import com.alphaflow.persistence.repositories.WeeklyPriceRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serves the indicator discovery matrix and the per-ticker, per-timeframe indicator series.
 *
 * <p>The series window mirrors the price endpoint: indicators are returned for the same date range
 * as the most recent {@code window} candles, so chart overlays align exactly with the bars.
 */
@Service
@Transactional(readOnly = true)
public class IndicatorService {

  private static final Logger log = LoggerFactory.getLogger(IndicatorService.class);

  private final IndicatorConfig indicatorConfig;
  private final ChartConfig chartConfig;
  private final TickerRepository tickerRepository;
  private final DailyPriceRepository dailyPriceRepository;
  private final WeeklyPriceRepository weeklyPriceRepository;
  private final IndicatorValueRepository indicatorValueRepository;

  public IndicatorService(
      IndicatorConfig indicatorConfig,
      ChartConfig chartConfig,
      TickerRepository tickerRepository,
      DailyPriceRepository dailyPriceRepository,
      WeeklyPriceRepository weeklyPriceRepository,
      IndicatorValueRepository indicatorValueRepository) {
    this.indicatorConfig = indicatorConfig;
    this.chartConfig = chartConfig;
    this.tickerRepository = tickerRepository;
    this.dailyPriceRepository = dailyPriceRepository;
    this.weeklyPriceRepository = weeklyPriceRepository;
    this.indicatorValueRepository = indicatorValueRepository;
  }

  /** The configured indicator matrix across all timeframes. */
  public List<IndicatorConfigDTO> getConfiguredIndicators() {
    List<IndicatorConfigDTO> configs = new ArrayList<>();
    for (Timeframe timeframe : Timeframe.values()) {
      for (IndicatorDefinition definition : indicatorConfig.forTimeframe(timeframe)) {
        configs.add(IndicatorMapper.toConfigDTO(timeframe, definition));
      }
    }
    return configs;
  }

  public List<IndicatorSeriesDTO> getIndicatorSeries(String symbol, Timeframe timeframe) {
    return getIndicatorSeries(symbol, timeframe, 0, null);
  }

  public List<IndicatorSeriesDTO> getIndicatorSeries(
      String symbol, Timeframe timeframe, int page, Integer size) {
    log.debug(
        "Fetching {} indicators for ticker: {} (page={}, size={})", timeframe, symbol, page, size);
    if (!tickerRepository.existsByTickerSymbolIgnoreCase(symbol)) {
      log.warn("Ticker not found for symbol: {}", symbol);
      throw new ResourceNotFoundException("Ticker not found: " + symbol);
    }

    int window = chartConfig.getWindow();
    int actualSize = size != null ? Math.min(size, window * 5) : window;
    if (actualSize < 1) {
      actualSize = 1;
    }
    PageRequest pageRequest = PageRequest.of(page, actualSize);
    List<LocalDate> pageDates =
        timeframe == Timeframe.WEEKLY
            ? weeklyPriceRepository.findRecentPriceDates(symbol, pageRequest)
            : dailyPriceRepository.findRecentPriceDates(symbol, pageRequest);

    if (pageDates.isEmpty()) {
      return List.of();
    }

    // pageDates is ordered DESC, so the last element is the oldest and the first element is the
    // newest
    LocalDate start = pageDates.getLast();
    LocalDate end = pageDates.getFirst();

    List<IndicatorValue> rows =
        indicatorValueRepository.findSeriesBetween(symbol, timeframe, start, end);
    return IndicatorMapper.toSeries(rows);
  }
}
