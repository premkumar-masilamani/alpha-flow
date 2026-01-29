package com.alphaflow.backtest.entities;

import com.alphaflow.infrastructure.entities.Ticker;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "backtest_results")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BacktestResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long backtestResultId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticker_id")
    private Ticker ticker;

    private String strategyName;

    private BigDecimal initialEquity;

    private BigDecimal finalEquity;

    private LocalDate startDate;

    private LocalDate endDate;

    private BigDecimal years;

    private BigDecimal cagr;

    private BigDecimal winRate;
}
