package com.alphaflow.persistence.repositories;

import com.alphaflow.persistence.entities.DailyIndicator;
import com.alphaflow.persistence.entities.IndicatorDefinition;
import com.alphaflow.persistence.entities.Ticker;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/** JPA repository for managing DailyIndicator entities. */
@Repository
public interface DailyIndicatorRepository extends JpaRepository<DailyIndicator, Long> {

  /**
   * Finds the last stored daily indicator entity for a given ticker and definition.
   *
   * @param ticker the ticker
   * @param indicatorDefinition the indicator definition
   * @return optional containing the daily indicator entity if found
   */
  Optional<DailyIndicator> findFirstByTickerAndIndicatorDefinitionOrderByPriceDateDesc(
      Ticker ticker, IndicatorDefinition indicatorDefinition);

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
