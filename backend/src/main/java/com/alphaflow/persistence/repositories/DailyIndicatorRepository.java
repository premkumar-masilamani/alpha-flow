package com.alphaflow.persistence.repositories;

import com.alphaflow.persistence.entities.DailyIndicator;
import com.alphaflow.persistence.entities.Ticker;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/** JPA repository for managing DailyIndicator entities. */
@Repository
interface DailyIndicatorRepository extends JpaRepository<DailyIndicator, Long> {

  /**
   * Deletes daily indicator values for a ticker and indicator IDs.
   *
   * @param ticker the ticker
   * @param indicatorIds the indicator definition IDs
   */
  @Modifying
  @Query(
      """
      DELETE FROM DailyIndicator v
      WHERE v.ticker = :ticker
        AND v.indicatorDefinition.indicatorId IN :indicatorIds
      """)
  void deleteByTickerAndIndicatorIds(Ticker ticker, Collection<Long> indicatorIds);

  /**
   * Finds daily indicators series from a start date.
   *
   * @param symbol the ticker symbol
   * @param indicatorIds the indicator IDs
   * @param from the start date (inclusive)
   * @return list of daily indicators
   */
  @Query(
      """
      SELECT iv FROM DailyIndicator iv
      JOIN iv.ticker tk
      WHERE LOWER(tk.tickerSymbol) = LOWER(:symbol)
        AND iv.indicatorDefinition.indicatorId IN :indicatorIds
        AND iv.priceDate >= :from
      ORDER BY iv.priceDate ASC
      """)
  List<DailyIndicator> findSeries(String symbol, Collection<Long> indicatorIds, LocalDate from);

  /**
   * Finds daily indicators series between two dates.
   *
   * @param symbol the ticker symbol
   * @param indicatorIds the indicator IDs
   * @param from the start date (inclusive)
   * @param to the end date (inclusive)
   * @return list of daily indicators
   */
  @Query(
      """
      SELECT iv FROM DailyIndicator iv
      JOIN iv.ticker tk
      WHERE LOWER(tk.tickerSymbol) = LOWER(:symbol)
        AND iv.indicatorDefinition.indicatorId IN :indicatorIds
        AND iv.priceDate >= :from
        AND iv.priceDate <= :to
      ORDER BY iv.priceDate ASC
      """)
  List<DailyIndicator> findSeriesBetween(
      String symbol, Collection<Long> indicatorIds, LocalDate from, LocalDate to);
}
