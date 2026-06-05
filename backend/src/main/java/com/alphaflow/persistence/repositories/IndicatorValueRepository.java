package com.alphaflow.persistence.repositories;

import com.alphaflow.persistence.entities.IndicatorValue;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.enums.IndicatorType;
import com.alphaflow.persistence.enums.PriceSource;
import com.alphaflow.persistence.enums.Timeframe;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/** Published indicator plot values. See docs/indicators-design.md. */
@Repository
public interface IndicatorValueRepository extends JpaRepository<IndicatorValue, Long> {

  /**
   * Deletes a combo's published values from {@code from} (inclusive) onward, so the recomputed tail
   *
   * <p>can be reinserted. This is a bulk {@code DELETE} that executes immediately (not via the
   *
   * <p>persistence context), which is important: it runs before the subsequent inserts flush, so
   * the
   *
   * <p>delete-then-insert of the same natural keys cannot collide with the unique constraint.
   */
  @Modifying
  @Query(
      """

                    DELETE FROM IndicatorValue v

                    WHERE v.ticker = :ticker

                      AND v.timeframe = :timeframe

                      AND v.indicatorType = :indicatorType

                      AND v.source = :source

                      AND v.params = :params

                      AND v.priceDate >= :from

                    """)
  void deleteCombo(
      Ticker ticker,
      Timeframe timeframe,
      IndicatorType indicatorType,
      PriceSource source,
      String params,
      LocalDate from);

  /**
   * All published plot values for a ticker on a timeframe from {@code from} (inclusive) onward,
   *
   * <p>chronological. Backs the bulk-by-timeframe API; the caller groups rows into per-indicator
   * series.
   */
  @Query(
      """

                    SELECT iv FROM IndicatorValue iv

                    JOIN iv.ticker tk

                    WHERE LOWER(tk.tickerSymbol) = LOWER(:symbol)

                      AND iv.timeframe = :timeframe

                      AND iv.priceDate >= :from

                    ORDER BY iv.priceDate ASC

                    """)
  List<IndicatorValue> findSeries(String symbol, Timeframe timeframe, LocalDate from);

  @Query(
      """

                    SELECT iv FROM IndicatorValue iv

                    JOIN iv.ticker tk

                    WHERE LOWER(tk.tickerSymbol) = LOWER(:symbol)

                      AND iv.timeframe = :timeframe

                      AND iv.priceDate >= :from

                      AND iv.priceDate <= :to

                    ORDER BY iv.priceDate ASC

                    """)
  List<IndicatorValue> findSeriesBetween(
      String symbol, Timeframe timeframe, LocalDate from, LocalDate to);
}
