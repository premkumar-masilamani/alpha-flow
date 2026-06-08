package com.alphaflow.persistence.repositories;

import com.alphaflow.persistence.entities.IndicatorDefinition;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.entities.WeeklyIndicator;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/** JPA repository for managing WeeklyIndicator entities. */
@Repository
public interface WeeklyIndicatorRepository extends JpaRepository<WeeklyIndicator, Long> {

  /**
   * Finds the last stored weekly indicator entity for a given ticker and definition.
   *
   * @param ticker the ticker
   * @param indicatorDefinition the indicator definition
   * @return optional containing the weekly indicator entity if found
   */
  Optional<WeeklyIndicator> findFirstByTickerAndIndicatorDefinitionOrderByPriceDateDesc(
      Ticker ticker, IndicatorDefinition indicatorDefinition);

  /**
   * Finds weekly indicators series between two dates.
   *
   * @param symbol the ticker symbol
   * @param indicatorIds the indicator IDs
   * @param from the start date (inclusive)
   * @param to the end date (inclusive)
   * @return list of weekly indicators
   */
  @Query(
      """
            SELECT iv FROM WeeklyIndicator iv
            JOIN iv.ticker tk
            WHERE LOWER(tk.tickerSymbol) = LOWER(:symbol)
              AND iv.indicatorDefinition.indicatorId IN :indicatorIds
              AND iv.priceDate >= :from
              AND iv.priceDate <= :to
            ORDER BY iv.priceDate ASC
            """)
  List<WeeklyIndicator> findSeriesBetween(
      String symbol, Collection<Long> indicatorIds, LocalDate from, LocalDate to);
}
