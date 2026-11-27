package com.alphaflow.api.services;

import com.alphaflow.api.dtos.ChartPatternDto;
import com.alphaflow.api.dtos.ChartPatternPivotDto;
import com.alphaflow.common.enums.Timeframe;
import com.alphaflow.persistence.entities.ChartPatternPivot;
import com.alphaflow.persistence.entities.DailyChartPattern;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.entities.WeeklyChartPattern;
import com.alphaflow.persistence.enums.ChartPatternStatus;
import com.alphaflow.persistence.repositories.DailyChartPatternRepository;
import com.alphaflow.persistence.repositories.WeeklyChartPatternRepository;
import java.time.LocalDate;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@Slf4j
public class ChartPatternService {

  private final DailyChartPatternRepository dailyChartPatternRepository;
  private final WeeklyChartPatternRepository weeklyChartPatternRepository;

  public ChartPatternService(
      DailyChartPatternRepository dailyChartPatternRepository,
      WeeklyChartPatternRepository weeklyChartPatternRepository) {
    this.dailyChartPatternRepository = dailyChartPatternRepository;
    this.weeklyChartPatternRepository = weeklyChartPatternRepository;
  }

  public List<ChartPatternDto> getPatterns(
      Ticker ticker,
      Timeframe timeframe,
      LocalDate date,
      List<ChartPatternStatus> statuses,
      int page,
      int size) {
    PageRequest pageRequest = PageRequest.of(page, size);

    List<ChartPatternStatus> effectiveStatuses =
        (statuses != null && !statuses.isEmpty())
            ? statuses
            : List.of(ChartPatternStatus.IN_PROGRESS, ChartPatternStatus.COMPLETED);

    log.debug(
        "Fetching {} chart patterns for ticker {} (date={}, statuses={}, page={}, size={})",
        timeframe,
        ticker.getTickerSymbol(),
        date,
        effectiveStatuses,
        page,
        size);

    if (timeframe == Timeframe.DAILY) {
      List<ChartPatternDto> patterns =
          dailyChartPatternRepository
              .findByTickerAndPriceDateAndStatusInOrderByEndDateDesc(
                  ticker, date, effectiveStatuses, pageRequest)
              .getContent()
              .stream()
              .map(this::toDto)
              .toList();
      log.debug(
          "Retrieved {} DAILY chart patterns for ticker {} on {}",
          patterns.size(),
          ticker.getTickerSymbol(),
          date);
      return patterns;
    } else if (timeframe == Timeframe.WEEKLY) {
      List<ChartPatternDto> patterns =
          weeklyChartPatternRepository
              .findByTickerAndPriceDateAndStatusInOrderByEndDateDesc(
                  ticker, date, effectiveStatuses, pageRequest)
              .getContent()
              .stream()
              .map(this::toDto)
              .toList();
      log.debug(
          "Retrieved {} WEEKLY chart patterns for ticker {} on {}",
          patterns.size(),
          ticker.getTickerSymbol(),
          date);
      return patterns;
    } else {
      log.error("Unsupported timeframe for fetching chart patterns: {}", timeframe);
      throw new IllegalArgumentException("Unsupported timeframe: " + timeframe);
    }
  }

  private ChartPatternDto toDto(DailyChartPattern dailyPattern) {
    List<ChartPatternPivotDto> pivotDtos =
        dailyPattern.getPivotPoints() != null
            ? dailyPattern.getPivotPoints().stream().map(this::toPivotDto).toList()
            : List.of();

    return ChartPatternDto.builder()
        .id(dailyPattern.getDailyChartPatternId())
        .priceDate(dailyPattern.getPriceDate())
        .patternType(dailyPattern.getPatternType())
        .shortName(dailyPattern.getPatternType().getShortName())
        .displayName(dailyPattern.getPatternType().getDisplayName())
        .sentiment(dailyPattern.getSentiment())
        .status(dailyPattern.getStatus())
        .startDate(dailyPattern.getStartDate())
        .endDate(dailyPattern.getEndDate())
        .breakoutDate(dailyPattern.getBreakoutDate())
        .necklineSlope(dailyPattern.getNecklineSlope())
        .necklinePrice(dailyPattern.getNecklinePrice())
        .targetPrice(dailyPattern.getTargetPrice())
        .stopLossPrice(dailyPattern.getStopLossPrice())
        .invalidationPrice(dailyPattern.getInvalidationPrice())
        .pivotPoints(pivotDtos)
        .build();
  }

  private ChartPatternDto toDto(WeeklyChartPattern weeklyPattern) {
    List<ChartPatternPivotDto> pivotDtos =
        weeklyPattern.getPivotPoints() != null
            ? weeklyPattern.getPivotPoints().stream().map(this::toPivotDto).toList()
            : List.of();

    return ChartPatternDto.builder()
        .id(weeklyPattern.getWeeklyChartPatternId())
        .priceDate(weeklyPattern.getPriceDate())
        .patternType(weeklyPattern.getPatternType())
        .shortName(weeklyPattern.getPatternType().getShortName())
        .displayName(weeklyPattern.getPatternType().getDisplayName())
        .sentiment(weeklyPattern.getSentiment())
        .status(weeklyPattern.getStatus())
        .startDate(weeklyPattern.getStartDate())
        .endDate(weeklyPattern.getEndDate())
        .breakoutDate(weeklyPattern.getBreakoutDate())
        .necklineSlope(weeklyPattern.getNecklineSlope())
        .necklinePrice(weeklyPattern.getNecklinePrice())
        .targetPrice(weeklyPattern.getTargetPrice())
        .stopLossPrice(weeklyPattern.getStopLossPrice())
        .invalidationPrice(weeklyPattern.getInvalidationPrice())
        .pivotPoints(pivotDtos)
        .build();
  }

  private ChartPatternPivotDto toPivotDto(ChartPatternPivot pivot) {
    return ChartPatternPivotDto.builder()
        .date(pivot.getDate())
        .price(pivot.getPrice())
        .type(pivot.getType())
        .role(pivot.getRole())
        .build();
  }
}
