package com.alphaflow.persistence.repositories;

import com.alphaflow.engine.calculators.dtos.TickerIndicatorSyncState;
import com.alphaflow.persistence.entities.IndicatorDefinition;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.entities.WeeklyIndicator;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface WeeklyIndicatorRepository extends JpaRepository<WeeklyIndicator, Long> {

  @Query(
      """
      SELECT new com.alphaflow.engine.calculators.dtos.TickerIndicatorSyncState(
          tk,
          idDef,
          (SELECT MAX(wp.priceDate) FROM WeeklyPrice wp WHERE wp.ticker = tk),
          (SELECT MAX(wi.priceDate) FROM WeeklyIndicator wi WHERE wi.ticker = tk AND wi.indicatorDefinition = idDef)
      )
      FROM Ticker tk, IndicatorDefinition idDef
      WHERE tk.isActive = true
      """)
  List<TickerIndicatorSyncState> findSyncStatesForActiveTickers();

  @Modifying(flushAutomatically = true)
  @Query(
      """
      DELETE FROM WeeklyIndicator wi
      WHERE wi.ticker = :ticker
        AND wi.indicatorDefinition = :indicatorDefinition
        AND wi.priceDate >= :priceDate
      """)
  void deleteByTickerAndIndicatorDefinitionAndPriceDateGreaterThanEqual(
      @Param("ticker") Ticker ticker,
      @Param("indicatorDefinition") IndicatorDefinition indicatorDefinition,
      @Param("priceDate") LocalDate priceDate);

  @Query(
      """
      SELECT iv FROM WeeklyIndicator iv
      WHERE iv.ticker = :ticker
        AND iv.indicatorDefinition.indicatorDefinitionId IN :indicatorIds
        AND iv.priceDate >= :from
        AND iv.priceDate <= :to
      ORDER BY iv.priceDate ASC
      """)
  List<WeeklyIndicator> findSeriesBetween(
      Ticker ticker, Collection<Long> indicatorIds, LocalDate from, LocalDate to);
}
