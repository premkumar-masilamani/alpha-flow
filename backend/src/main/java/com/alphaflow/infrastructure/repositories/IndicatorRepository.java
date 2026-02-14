package com.alphaflow.infrastructure.repositories;

import com.alphaflow.infrastructure.entities.Indicator;
import com.alphaflow.infrastructure.entities.Ticker;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface IndicatorRepository extends JpaRepository<Indicator, Long> {

    Optional<Indicator> findTopByTickerAndMetricAndMaTypeAndPeriodOrderByIndicatorDateDesc(
            Ticker ticker,
            String metric,
            String maType,
            int period
    );

    Optional<Indicator> findByTickerAndIndicatorDateAndMetricAndMaTypeAndPeriod(
            Ticker ticker,
            LocalDate indicatorDate,
            String metric,
            String maType,
            int period
    );

    List<Indicator> findByTickerAndMetricAndMaTypeAndPeriodAndIndicatorDateGreaterThanEqualOrderByIndicatorDateAsc(
            Ticker ticker,
            String metric,
            String maType,
            int period,
            LocalDate startDate
    );

    List<Indicator> findByTickerOrderByIndicatorDateAsc(Ticker ticker);
}
