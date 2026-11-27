package com.alphaflow.persistence.repositories;

import com.alphaflow.engine.calculators.dtos.TickerIndicatorSyncState;
import com.alphaflow.persistence.entities.DailyIndicator;
import com.alphaflow.persistence.entities.Ticker;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface DailyIndicatorRepository extends JpaRepository<DailyIndicator, Long> {

  @Query(
      """
      SELECT new com.alphaflow.engine.calculators.dtos.TickerIndicatorSyncState(
          tk,
          idDef,
          (SELECT MAX(dp.priceDate) FROM DailyPrice dp WHERE dp.ticker = tk),
          (SELECT MAX(di.priceDate) FROM DailyIndicator di WHERE di.ticker = tk AND di.indicatorDefinition = idDef)
      )
      FROM Ticker tk, IndicatorDefinition idDef
      WHERE tk.isActive = true
      """)
  List<TickerIndicatorSyncState> findSyncStatesForActiveTickers();

  @Query(
      """
      SELECT iv FROM DailyIndicator iv
      WHERE iv.ticker = :ticker
        AND iv.indicatorDefinition.indicatorDefinitionId IN :indicatorIds
        AND iv.priceDate >= :from
        AND iv.priceDate <= :to
      ORDER BY iv.priceDate ASC
      """)
  List<DailyIndicator> findSeriesBetween(
      Ticker ticker, Collection<Long> indicatorIds, LocalDate from, LocalDate to);
}
