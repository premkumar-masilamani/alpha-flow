package com.alphaflow.api.services;

import com.alphaflow.api.dtos.IndicatorConfigDTO;
import com.alphaflow.api.dtos.IndicatorSeriesDTO;
import com.alphaflow.api.mappers.IndicatorMapper;
import com.alphaflow.engine.configs.IndicatorConfig;
import com.alphaflow.persistence.entities.Indicator;
import com.alphaflow.persistence.entities.IndicatorDefinition;
import com.alphaflow.persistence.enums.Timeframe;
import com.alphaflow.persistence.exceptions.ResourceNotFoundException;
import com.alphaflow.persistence.repositories.DailyIndicatorRepository;
import com.alphaflow.persistence.repositories.DailyPriceRepository;
import com.alphaflow.persistence.repositories.TickerRepository;
import com.alphaflow.persistence.repositories.WeeklyIndicatorRepository;
import com.alphaflow.persistence.repositories.WeeklyPriceRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class IndicatorService {

  private final IndicatorConfig indicatorConfig;
  private final TickerRepository tickerRepository;
  private final DailyPriceRepository dailyPriceRepository;
  private final WeeklyPriceRepository weeklyPriceRepository;
  private final DailyIndicatorRepository dailyIndicatorRepository;
  private final WeeklyIndicatorRepository weeklyIndicatorRepository;

  public IndicatorService(
      IndicatorConfig indicatorConfig,
      TickerRepository tickerRepository,
      DailyPriceRepository dailyPriceRepository,
      WeeklyPriceRepository weeklyPriceRepository,
      DailyIndicatorRepository dailyIndicatorRepository,
      WeeklyIndicatorRepository weeklyIndicatorRepository) {
    this.indicatorConfig = indicatorConfig;
    this.tickerRepository = tickerRepository;
    this.dailyPriceRepository = dailyPriceRepository;
    this.weeklyPriceRepository = weeklyPriceRepository;
    this.dailyIndicatorRepository = dailyIndicatorRepository;
    this.weeklyIndicatorRepository = weeklyIndicatorRepository;
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

  public List<IndicatorSeriesDTO> getIndicatorSeries(
      String symbol, Timeframe timeframe, int page, int size) {
    log.debug(
        "Fetching {} indicators for ticker: {} (page={}, size={})", timeframe, symbol, page, size);
    if (!tickerRepository.existsByTickerSymbolIgnoreCase(symbol)) {
      log.warn("Ticker not found for symbol: {}", symbol);
      throw new ResourceNotFoundException("Ticker not found: " + symbol);
    }

    PageRequest pageRequest = PageRequest.of(page, size);
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

    List<IndicatorDefinition> definitions = indicatorConfig.forTimeframe(timeframe);
    if (definitions.isEmpty()) {
      return List.of();
    }
    List<Long> indicatorIds =
        definitions.stream().map(IndicatorDefinition::getIndicatorId).toList();

    List<? extends Indicator> rows =
        timeframe == Timeframe.DAILY
            ? dailyIndicatorRepository.findSeriesBetween(symbol, indicatorIds, start, end)
            : weeklyIndicatorRepository.findSeriesBetween(symbol, indicatorIds, start, end);
    return IndicatorMapper.toSeries(rows);
  }
}
