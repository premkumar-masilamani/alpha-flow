package com.alphaflow.persistence.repositories;

import com.alphaflow.engine.calculators.dtos.TickerSyncState;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.entities.WeeklyChartPattern;
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
public interface WeeklyChartPatternRepository extends JpaRepository<WeeklyChartPattern, Long> {

  @Query(
      """
      SELECT new com.alphaflow.engine.calculators.dtos.TickerSyncState(
          tk,
          (SELECT MAX(wp.priceDate) FROM WeeklyPrice wp WHERE wp.ticker = tk),
          (SELECT MAX(wcp.endDate) FROM WeeklyChartPattern wcp WHERE wcp.ticker = tk)
      )
      FROM Ticker tk
      WHERE tk.isActive = true
      """)
  List<TickerSyncState> findSyncStatesForActiveTickers();

  Page<WeeklyChartPattern> findByTickerAndStatusInOrderByEndDateDesc(
      Ticker ticker, Collection<ChartPatternStatus> statuses, Pageable pageable);

  Optional<WeeklyChartPattern> findByTickerAndPatternTypeAndStartDate(
      Ticker ticker, ChartPatternType patternType, LocalDate startDate);
}
