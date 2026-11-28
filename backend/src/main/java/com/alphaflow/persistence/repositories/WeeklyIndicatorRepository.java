package com.alphaflow.persistence.repositories;

import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.entities.WeeklyIndicator;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
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
      SELECT CASE WHEN COUNT(tk) > 0 THEN true ELSE false END
      FROM Ticker tk, IndicatorDefinition idDef
      WHERE tk.isActive = true
        AND EXISTS (SELECT 1 FROM WeeklyPrice wp WHERE wp.ticker = tk)
        AND (
          NOT EXISTS (SELECT 1 FROM WeeklyIndicator wi WHERE wi.ticker = tk AND wi.indicatorDefinition = idDef)
          OR (SELECT MAX(wi.priceDate) FROM WeeklyIndicator wi WHERE wi.ticker = tk AND wi.indicatorDefinition = idDef) <
             (SELECT MAX(wp.priceDate) FROM WeeklyPrice wp WHERE wp.ticker = tk)
          OR (:isTradingWeekday = true AND (SELECT MAX(wp.priceDate) FROM WeeklyPrice wp WHERE wp.ticker = tk) = :currentWeekMonday)
        )
      """)
  boolean hasPendingWeeklyIndicators(
      @Param("currentWeekMonday") LocalDate currentWeekMonday,
      @Param("isTradingWeekday") boolean isTradingWeekday);

  default boolean hasPendingWeeklyIndicators() {
    LocalDate today = LocalDate.now(ZoneOffset.UTC);
    DayOfWeek dow = today.getDayOfWeek();
    boolean isTradingWeekday = dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY;
    LocalDate currentWeekMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    return hasPendingWeeklyIndicators(currentWeekMonday, isTradingWeekday);
  }

  @Query(
      value =
          """
          SELECT wi.*
          FROM (
              SELECT w.*,
                     ROW_NUMBER() OVER (
                         PARTITION BY w.ticker_id, w.indicator_definition_id
                         ORDER BY w.price_date DESC
                     ) AS rn
              FROM weekly_indicators w
              JOIN tickers t ON t.ticker_id = w.ticker_id
              WHERE t.is_active = true
          ) wi
          WHERE wi.rn <= 2
          ORDER BY wi.ticker_id, wi.indicator_definition_id, wi.price_date DESC
          """,
      nativeQuery = true)
  List<WeeklyIndicator> findTop2LatestForActiveTickers();

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

  @Modifying
  @Query("DELETE FROM WeeklyIndicator wi WHERE wi.ticker = :ticker")
  void deleteByTicker(@Param("ticker") Ticker ticker);
}
