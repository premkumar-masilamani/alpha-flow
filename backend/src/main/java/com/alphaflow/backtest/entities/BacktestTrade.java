package com.alphaflow.backtest.entities;

import com.alphaflow.backtest.enums.TradeSide;
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
    @JoinColumn(name = "ticker_id")
    private Ticker ticker;

    private String strategyName;

    @Enumerated(EnumType.STRING)
    private TradeSide side;

    private LocalDate entryDate;

    private BigDecimal entryPrice;

    private LocalDate exitDate;

    private BigDecimal exitPrice;

    private BigDecimal quantity;

    private BigDecimal pnl;

    private BigDecimal pnlPct;

    private Integer holdingBars;
}
