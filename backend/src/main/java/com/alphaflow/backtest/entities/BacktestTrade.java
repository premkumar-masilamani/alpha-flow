package com.alphaflow.backtest.entities;

import com.alphaflow.backtest.enums.PositionType;
import com.alphaflow.infrastructure.entities.Ticker;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "backtest_trades")
@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BacktestTrade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "backtest_trade_id")
    private Long backtestTradeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticker_id", nullable = false)
    private Ticker ticker;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "backtest_strategy_id")
    private BacktestStrategy strategy;

    @Enumerated(EnumType.STRING)
    @Column(name = "side", nullable = false, length = 50)
    private PositionType side;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Column(name = "entry_price", nullable = false, precision = 28, scale = 8)
    private BigDecimal entryPrice;

    @Column(name = "exit_date")
    private LocalDate exitDate;

    @Column(name = "exit_price", precision = 28, scale = 8)
    private BigDecimal exitPrice;

    @Column(name = "quantity", nullable = false, precision = 28, scale = 8)
    private BigDecimal quantity;

    @Column(name = "pnl", precision = 28, scale = 8)
    private BigDecimal pnl;

    @Column(name = "pnl_pct", precision = 28, scale = 8)
    private BigDecimal pnlPct;

    @Column(name = "holding_bars")
    private Integer holdingBars;
}
