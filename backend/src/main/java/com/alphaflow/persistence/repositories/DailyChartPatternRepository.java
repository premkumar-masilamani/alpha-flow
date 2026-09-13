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
import org.springframework.stereotype.Repository;

@Repository
public interface DailyChartPatternRepository extends JpaRepository<DailyChartPattern, Long> {

  Page<DailyChartPattern> findByTickerOrderByEndDateDesc(Ticker ticker, Pageable pageable);

  Page<DailyChartPattern> findByTickerAndStatusOrderByEndDateDesc(
      Ticker ticker, ChartPatternStatus status, Pageable pageable);

  Page<DailyChartPattern> findByTickerAndStatusInOrderByEndDateDesc(
      Ticker ticker, Collection<ChartPatternStatus> statuses, Pageable pageable);

  Optional<DailyChartPattern> findByTickerAndPatternTypeAndStartDate(
      Ticker ticker, ChartPatternType patternType, LocalDate startDate);

  List<DailyChartPattern> findByTicker(Ticker ticker);

  List<DailyChartPattern> findByTickerAndStatus(Ticker ticker, ChartPatternStatus status);

  void deleteByTicker(Ticker ticker);
}
