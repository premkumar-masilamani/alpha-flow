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
      SELECT tk,
          (SELECT MAX(dp.priceDate) FROM DailyPrice dp WHERE dp.ticker = tk),
          (SELECT MAX(dcp.endDate) FROM DailyChartPattern dcp WHERE dcp.ticker = tk)
      FROM Ticker tk
      WHERE tk.isActive = true
      """)
  List<Object[]> findSyncStatesForActiveTickers();

  Page<DailyChartPattern> findByTickerAndStatusInOrderByEndDateDesc(
      Ticker ticker, Collection<ChartPatternStatus> statuses, Pageable pageable);

  Optional<DailyChartPattern> findByTickerAndPatternTypeAndStartDate(
      Ticker ticker, ChartPatternType patternType, LocalDate startDate);
}
