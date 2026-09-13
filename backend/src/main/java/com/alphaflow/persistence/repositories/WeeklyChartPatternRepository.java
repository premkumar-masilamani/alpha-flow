package com.alphaflow.persistence.repositories;

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
import org.springframework.stereotype.Repository;

@Repository
public interface WeeklyChartPatternRepository extends JpaRepository<WeeklyChartPattern, Long> {

  Page<WeeklyChartPattern> findByTickerOrderByEndDateDesc(Ticker ticker, Pageable pageable);

  Page<WeeklyChartPattern> findByTickerAndStatusOrderByEndDateDesc(
      Ticker ticker, ChartPatternStatus status, Pageable pageable);

  Page<WeeklyChartPattern> findByTickerAndStatusInOrderByEndDateDesc(
      Ticker ticker, Collection<ChartPatternStatus> statuses, Pageable pageable);

  Optional<WeeklyChartPattern> findByTickerAndPatternTypeAndStartDate(
      Ticker ticker, ChartPatternType patternType, LocalDate startDate);

  List<WeeklyChartPattern> findByTicker(Ticker ticker);

  List<WeeklyChartPattern> findByTickerAndStatus(Ticker ticker, ChartPatternStatus status);

  void deleteByTicker(Ticker ticker);
}
