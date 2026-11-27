package com.alphaflow.persistence.repositories;

import com.alphaflow.persistence.entities.DailyIndicator;
import com.alphaflow.persistence.entities.IndicatorDefinition;
import com.alphaflow.persistence.entities.Ticker;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DailyIndicatorRepository extends JpaRepository<DailyIndicator, Long> {

  @Query(
      """
      SELECT CASE WHEN COUNT(tk) > 0 THEN true ELSE false END
      FROM Ticker tk, IndicatorDefinition idDef
      WHERE tk.isActive = true
        AND EXISTS (SELECT 1 FROM DailyPrice dp WHERE dp.ticker = tk)
        AND (
          NOT EXISTS (SELECT 1 FROM DailyIndicator di WHERE di.ticker = tk AND di.indicatorDefinition = idDef)
          OR (SELECT MAX(di.priceDate) FROM DailyIndicator di WHERE di.ticker = tk AND di.indicatorDefinition = idDef) <
             (SELECT MAX(dp.priceDate) FROM DailyPrice dp WHERE dp.ticker = tk)
        )
      """)
  boolean hasPendingDailyIndicators();

  @Query(
      """
      SELECT di.indicatorDefinition, MAX(di.priceDate)
      FROM DailyIndicator di
      WHERE di.ticker = :ticker
      GROUP BY di.indicatorDefinition
      """)
  List<Object[]> findLatestDatesGroupedByDefinition(@Param("ticker") Ticker ticker);

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
