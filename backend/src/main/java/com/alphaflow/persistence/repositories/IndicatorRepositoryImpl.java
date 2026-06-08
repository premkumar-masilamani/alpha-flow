package com.alphaflow.persistence.repositories;

import com.alphaflow.persistence.entities.DailyIndicator;
import com.alphaflow.persistence.entities.Indicator;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.entities.WeeklyIndicator;
import com.alphaflow.persistence.enums.Timeframe;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of IndicatorRepository that routes query and database mutation requests
 * dynamically to the daily or weekly underlying JPA repositories.
 */
@Repository
@Transactional
public class IndicatorRepositoryImpl implements IndicatorRepository {

  private final DailyIndicatorRepository dailyIndicatorRepository;
  private final WeeklyIndicatorRepository weeklyIndicatorRepository;

  /**
   * Constructs an IndicatorRepositoryImpl.
   *
   * @param dailyIndicatorRepository the daily indicators repository
   * @param weeklyIndicatorRepository the weekly indicators repository
   */
  public IndicatorRepositoryImpl(
      DailyIndicatorRepository dailyIndicatorRepository,
      WeeklyIndicatorRepository weeklyIndicatorRepository) {
    this.dailyIndicatorRepository = dailyIndicatorRepository;
    this.weeklyIndicatorRepository = weeklyIndicatorRepository;
  }

  @Override
  public void deleteByTickerAndIndicatorIds(
      Ticker ticker, Collection<Long> indicatorIds, Timeframe timeframe) {
    if (timeframe == Timeframe.DAILY) {
      dailyIndicatorRepository.deleteByTickerAndIndicatorIds(ticker, indicatorIds);
    } else if (timeframe == Timeframe.WEEKLY) {
      weeklyIndicatorRepository.deleteByTickerAndIndicatorIds(ticker, indicatorIds);
    }
  }

  @Override
  @Transactional(readOnly = true)
  public List<? extends Indicator> findSeries(
      String symbol, Collection<Long> indicatorIds, LocalDate from, Timeframe timeframe) {
    if (timeframe == Timeframe.DAILY) {
      return dailyIndicatorRepository.findSeries(symbol, indicatorIds, from);
    } else if (timeframe == Timeframe.WEEKLY) {
      return weeklyIndicatorRepository.findSeries(symbol, indicatorIds, from);
    }
    return List.of();
  }

  @Override
  @Transactional(readOnly = true)
  public List<? extends Indicator> findSeriesBetween(
      String symbol,
      Collection<Long> indicatorIds,
      LocalDate from,
      LocalDate to,
      Timeframe timeframe) {
    if (timeframe == Timeframe.DAILY) {
      return dailyIndicatorRepository.findSeriesBetween(symbol, indicatorIds, from, to);
    } else if (timeframe == Timeframe.WEEKLY) {
      return weeklyIndicatorRepository.findSeriesBetween(symbol, indicatorIds, from, to);
    }
    return List.of();
  }

  @Override
  public void saveAll(List<? extends Indicator> entities, Timeframe timeframe) {
    if (entities.isEmpty()) {
      return;
    }
    if (timeframe == Timeframe.DAILY) {
      List<DailyIndicator> dailyList = new ArrayList<>(entities.size());
      for (Indicator e : entities) {
        if (e instanceof DailyIndicator) {
          dailyList.add((DailyIndicator) e);
        } else {
          throw new IllegalArgumentException(
              "Expected DailyIndicator, got: " + e.getClass().getName());
        }
      }
      dailyIndicatorRepository.saveAll(dailyList);
    } else if (timeframe == Timeframe.WEEKLY) {
      List<WeeklyIndicator> weeklyList = new ArrayList<>(entities.size());
      for (Indicator e : entities) {
        if (e instanceof WeeklyIndicator) {
          weeklyList.add((WeeklyIndicator) e);
        } else {
          throw new IllegalArgumentException(
              "Expected WeeklyIndicator, got: " + e.getClass().getName());
        }
      }
      weeklyIndicatorRepository.saveAll(weeklyList);
    }
  }
}
