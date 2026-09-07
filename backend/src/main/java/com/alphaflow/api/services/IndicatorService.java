package com.alphaflow.api.services;

import com.alphaflow.api.dtos.IndicatorConfigDto;
import com.alphaflow.api.dtos.IndicatorSeriesDto;
import com.alphaflow.api.mappers.IndicatorMapper;
import com.alphaflow.common.enums.Timeframe;
import com.alphaflow.engine.configs.IndicatorConfig;
import com.alphaflow.persistence.entities.Indicator;
import com.alphaflow.persistence.entities.IndicatorDefinition;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.repositories.DailyIndicatorRepository;
import com.alphaflow.persistence.repositories.DailyPriceRepository;
import com.alphaflow.persistence.repositories.WeeklyIndicatorRepository;
import com.alphaflow.persistence.repositories.WeeklyPriceRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@Slf4j
public class IndicatorService {

  private final IndicatorConfig indicatorConfig;
  private final DailyPriceRepository dailyPriceRepository;
  private final WeeklyPriceRepository weeklyPriceRepository;
  private final DailyIndicatorRepository dailyIndicatorRepository;
  private final WeeklyIndicatorRepository weeklyIndicatorRepository;

  public IndicatorService(
      IndicatorConfig indicatorConfig,
      DailyPriceRepository dailyPriceRepository,
      WeeklyPriceRepository weeklyPriceRepository,
      DailyIndicatorRepository dailyIndicatorRepository,
      WeeklyIndicatorRepository weeklyIndicatorRepository) {
    this.indicatorConfig = indicatorConfig;
    this.dailyPriceRepository = dailyPriceRepository;
    this.weeklyPriceRepository = weeklyPriceRepository;
    this.dailyIndicatorRepository = dailyIndicatorRepository;
    this.weeklyIndicatorRepository = weeklyIndicatorRepository;
  }

  public List<IndicatorConfigDto> getConfiguredIndicators() {
    List<IndicatorConfigDto> configs = new ArrayList<>();
    List<IndicatorDefinition> definitions = indicatorConfig.getDefinitions();
    for (Timeframe timeframe : Timeframe.values()) {
      for (IndicatorDefinition definition : definitions) {
        configs.add(IndicatorMapper.toConfigDto(timeframe, definition));
      }
    }
    return configs;
  }

  public List<IndicatorSeriesDto> getIndicatorSeries(
      Ticker ticker, Timeframe timeframe, int page, int size) {
    return getIndicatorSeries(ticker, timeframe, LocalDate.now(), page, size);
  }

  public List<IndicatorSeriesDto> getIndicatorSeries(
      Ticker ticker, Timeframe timeframe, LocalDate endDate, int page, int size) {
    log.debug(
        "Fetching {} indicators for ticker: {} up to {} (page={}, size={})",
        timeframe,
        ticker.getTickerSymbol(),
        endDate,
        page,
        size);

    PageRequest pageRequest = PageRequest.of(page, size);
    List<LocalDate> pageDates;
    if (timeframe == Timeframe.DAILY) {
      pageDates = dailyPriceRepository.findRecentPriceDatesUpTo(ticker, endDate, pageRequest);
    } else if (timeframe == Timeframe.WEEKLY) {
      pageDates = weeklyPriceRepository.findRecentPriceDatesUpTo(ticker, endDate, pageRequest);
    } else {
      log.error("Unsupported timeframe for fetching recent price dates: {}", timeframe);
      throw new IllegalArgumentException("Unsupported timeframe: " + timeframe);
    }

    if (pageDates.isEmpty()) {
      return List.of();
    }

    // pageDates is ordered DESC, so the last element is the oldest and the first element is the
    // newest
    LocalDate start = pageDates.getLast();
    LocalDate end = pageDates.getFirst();

    List<IndicatorDefinition> definitions = indicatorConfig.getDefinitions();
    if (definitions.isEmpty()) {
      return List.of();
    }
    List<Long> indicatorIds =
        definitions.stream().map(IndicatorDefinition::getIndicatorId).toList();

    List<? extends Indicator> rows;
    if (timeframe == Timeframe.DAILY) {
      rows = dailyIndicatorRepository.findSeriesBetween(ticker, indicatorIds, start, end);
    } else if (timeframe == Timeframe.WEEKLY) {
      rows = weeklyIndicatorRepository.findSeriesBetween(ticker, indicatorIds, start, end);
    } else {
      log.error("Unsupported timeframe for fetching indicator series: {}", timeframe);
      throw new IllegalArgumentException("Unsupported timeframe: " + timeframe);
    }
    return IndicatorMapper.toSeries(rows);
  }
}
