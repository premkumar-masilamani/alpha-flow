package com.alphaflow.persistence.repositories;

import com.alphaflow.persistence.entities.DailyChartPattern;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.enums.ChartPatternStatus;
import com.alphaflow.persistence.enums.ChartPatternType;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface DailyChartPatternRepository extends JpaRepository<DailyChartPattern, Long> {

  @Query(
      """
      SELECT CASE WHEN COUNT(tk) > 0 THEN true ELSE false END
      FROM Ticker tk
      WHERE tk.isActive = true
        AND EXISTS (SELECT 1 FROM DailyPrice dp WHERE dp.ticker = tk)
        AND (
          NOT EXISTS (SELECT 1 FROM DailyChartPattern dcp WHERE dcp.ticker = tk)
          OR (SELECT MAX(dcp.endDate) FROM DailyChartPattern dcp WHERE dcp.ticker = tk) <
             (SELECT MAX(dp.priceDate) FROM DailyPrice dp WHERE dp.ticker = tk)
        )
      """)
  boolean hasPendingDailyChartPatterns();

  Page<DailyChartPattern> findByTickerAndStatusInOrderByEndDateDesc(
      Ticker ticker, Collection<ChartPatternStatus> statuses, Pageable pageable);

  Optional<DailyChartPattern> findByTickerAndPatternTypeAndStartDate(
      Ticker ticker, ChartPatternType patternType, LocalDate startDate);
}
