package com.alphaflow.persistence.repositories;

import com.alphaflow.persistence.entities.Indicator;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.enums.Timeframe;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

/** Unified, timeframe-aware repository interface for daily and weekly technical indicators. */
public interface IndicatorRepository {

  /**
   * Deletes technical indicator records for a ticker and indicator IDs.
   *
   * @param ticker the ticker
   * @param indicatorIds the indicator definition IDs
   * @param timeframe the timeframe (DAILY or WEEKLY)
   */
  void deleteByTickerAndIndicatorIds(
      Ticker ticker, Collection<Long> indicatorIds, Timeframe timeframe);

  /**
   * Finds technical indicators series from a start date.
   *
   * @param symbol the ticker symbol
   * @param indicatorIds the indicator IDs
   * @param from the start date (inclusive)
   * @param timeframe the timeframe (DAILY or WEEKLY)
   * @return list of indicators
   */
  List<? extends Indicator> findSeries(
      String symbol, Collection<Long> indicatorIds, LocalDate from, Timeframe timeframe);

  /**
   * Finds technical indicators series between two dates.
   *
   * @param symbol the ticker symbol
   * @param indicatorIds the indicator IDs
   * @param from the start date (inclusive)
   * @param to the end date (inclusive)
   * @param timeframe the timeframe (DAILY or WEEKLY)
   * @return list of indicators
   */
  List<? extends Indicator> findSeriesBetween(
      String symbol,
      Collection<Long> indicatorIds,
      LocalDate from,
      LocalDate to,
      Timeframe timeframe);

  /**
   * Saves a collection of indicators.
   *
   * @param entities the indicator records to save
   * @param timeframe the timeframe (DAILY or WEEKLY)
   */
  void saveAll(List<? extends Indicator> entities, Timeframe timeframe);
}
