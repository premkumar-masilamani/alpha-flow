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

@Repository
public interface DailyIndicatorRepository extends JpaRepository<DailyIndicator, Long> {

  Optional<DailyIndicator> findFirstByTickerAndIndicatorDefinitionOrderByPriceDateDesc(
      Ticker ticker, IndicatorDefinition indicatorDefinition);

  @Query(
      """
            SELECT iv FROM DailyIndicator iv
            WHERE iv.ticker = :ticker
              AND iv.indicatorDefinition.indicatorId IN :indicatorIds
              AND iv.priceDate >= :from
              AND iv.priceDate <= :to
            ORDER BY iv.priceDate ASC
            """)
  List<DailyIndicator> findSeriesBetween(
      Ticker ticker, Collection<Long> indicatorIds, LocalDate from, LocalDate to);

  @Query(
      """
            SELECT iv FROM DailyIndicator iv
            WHERE iv.ticker = :ticker
              AND iv.indicatorDefinition.indicatorId IN :indicatorIds
              AND iv.priceDate >= :startDate
            ORDER BY iv.priceDate ASC
            """)
  List<DailyIndicator> findSeriesFrom(
      Ticker ticker, Collection<Long> indicatorIds, LocalDate startDate);
}
