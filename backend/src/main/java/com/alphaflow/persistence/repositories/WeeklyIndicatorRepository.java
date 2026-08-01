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

@Repository
public interface WeeklyIndicatorRepository extends JpaRepository<WeeklyIndicator, Long> {

  Optional<WeeklyIndicator> findFirstByTickerAndIndicatorDefinitionOrderByPriceDateDesc(
      Ticker ticker, IndicatorDefinition indicatorDefinition);

  @Query(
      """
            SELECT iv FROM WeeklyIndicator iv
            WHERE iv.ticker = :ticker
              AND iv.indicatorDefinition.indicatorId IN :indicatorIds
              AND iv.priceDate >= :from
              AND iv.priceDate <= :to
            ORDER BY iv.priceDate ASC
            """)
  List<WeeklyIndicator> findSeriesBetween(
      Ticker ticker, Collection<Long> indicatorIds, LocalDate from, LocalDate to);

  @Query(
      """
            SELECT iv FROM WeeklyIndicator iv
            WHERE iv.ticker = :ticker
              AND iv.indicatorDefinition.indicatorId IN :indicatorIds
              AND iv.priceDate >= :startDate
            ORDER BY iv.priceDate ASC
            """)
  List<WeeklyIndicator> findSeriesFrom(
      Ticker ticker, Collection<Long> indicatorIds, LocalDate startDate);
}
