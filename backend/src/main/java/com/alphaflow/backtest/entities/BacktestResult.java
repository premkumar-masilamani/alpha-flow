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
    @JoinColumn(name = "ticker_id", nullable = false)
    private Ticker ticker;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "backtest_strategy_id")
    private BacktestStrategy backtestStrategy;

    @Column(name = "initial_equity", nullable = false, precision = 28, scale = 8)
    private BigDecimal initialEquity;

    @Column(name = "final_equity", nullable = false, precision = 28, scale = 8)
    private BigDecimal finalEquity;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(nullable = false, precision = 28, scale = 8)
    private BigDecimal years;

    @Column(nullable = false, precision = 28, scale = 8)
    private BigDecimal cagr;

    @Column(name = "win_rate", precision = 28, scale = 8)
    private BigDecimal winRate;

    @Column(name = "total_return_pct", precision = 28, scale = 8)
    private BigDecimal totalReturnPct;

    @Column(name = "max_drawdown_pct", precision = 28, scale = 8)
    private BigDecimal maxDrawdownPct;

    @Column(name = "sharpe_ratio", precision = 28, scale = 8)
    private BigDecimal sharpeRatio;

    @Column(name = "total_trades")
    private Integer totalTrades;

    @Column(name = "avg_win", precision = 28, scale = 8)
    private BigDecimal avgWin;

    @Column(name = "avg_loss", precision = 28, scale = 8)
    private BigDecimal avgLoss;

    @Column(name = "profit_factor", precision = 28, scale = 8)
    private BigDecimal profitFactor;

    @Column(precision = 28, scale = 8)
    private BigDecimal expectancy;

    @Column(name = "cagr_score")
    private Integer cagrScore;

    @Column(name = "mdd_score")
    private Integer mddScore;

    @Column(name = "sharpe_score")
    private Integer sharpeScore;

    @Column(name = "profit_factor_score")
    private Integer profitFactorScore;

    @Column(name = "expectancy_score")
    private Integer expectancyScore;

    @Column(name = "final_score", precision = 28, scale = 8)
    private BigDecimal finalScore;

    @Column(name = "passes_filters")
    private Boolean passesFilters;
}
