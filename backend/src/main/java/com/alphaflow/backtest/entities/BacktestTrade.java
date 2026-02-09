package com.alphaflow.backtest.entities;

import com.alphaflow.backtest.enums.PositionType;
import com.alphaflow.infrastructure.entities.Ticker;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "backtest_trade")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BacktestTrade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long backtestTradeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticker_id", nullable = false)
    private Ticker ticker;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "backtest_strategy_id")
    private BacktestStrategy backtestStrategy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PositionType side;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Column(name = "entry_price", nullable = false, precision = 28, scale = 8)
    private BigDecimal entryPrice;

    @Column(name = "exit_date")
    private LocalDate exitDate;

    @Column(name = "exit_price", precision = 28, scale = 8)
    private BigDecimal exitPrice;

    @Column(nullable = false, precision = 28, scale = 8)
    private BigDecimal quantity;

    @Column(precision = 28, scale = 8)
    private BigDecimal pnl;

    @Column(name = "pnl_pct", precision = 28, scale = 8)
    private BigDecimal pnlPct;

    @Column(name = "holding_bars")
    private Integer holdingBars;
}
