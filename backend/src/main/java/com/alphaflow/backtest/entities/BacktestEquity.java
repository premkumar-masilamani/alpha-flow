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
@Table(name = "backtest_equity")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BacktestEquity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long backtestEquityId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticker_id", nullable = false)
    private Ticker ticker;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "backtest_strategy_id")
    private BacktestStrategy backtestStrategy;

    @Column(name = "equity_date", nullable = false)
    private LocalDate equityDate;

    @Column(nullable = false, precision = 28, scale = 8)
    private BigDecimal equity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PositionType position;

    @Column(name = "price_close", nullable = false, precision = 28, scale = 8)
    private BigDecimal priceClose;
}
