package com.alphaflow.backtest.entities;

import com.alphaflow.backtest.enums.PositionType;
import com.alphaflow.infrastructure.entities.Ticker;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "backtest_equities")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BacktestEquity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "backtest_equity_id")
    private Long backtestEquityId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticker_id", nullable = false)
    private Ticker ticker;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "backtest_strategy_id")
    private BacktestStrategy strategy;

    @Column(name = "equity_date", nullable = false)
    private LocalDate equityDate;

    @Column(name = "equity", nullable = false, precision = 28, scale = 8)
    private BigDecimal equity;

    @Enumerated(EnumType.STRING)
    @Column(name = "position", nullable = false, length = 50)
    private PositionType position;

    @Column(name = "price_close", nullable = false, precision = 28, scale = 8)
    private BigDecimal priceClose;
}
