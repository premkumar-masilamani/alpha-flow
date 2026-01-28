package com.alphaflow.infrastructure.persistence.entities;

import com.alphaflow.domain.enums.TradeSide;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "backtest_trade")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BacktestTrade {

    @Id
    @Column(name = "trade_id", columnDefinition = "UUID")
    private UUID tradeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticker_id")
    private Ticker ticker;

    @Column(name = "strategy_name")
    private String strategyName;

    @Enumerated(EnumType.STRING)
    @Column(name = "side")
    private TradeSide side;

    @Column(name = "entry_date")
    private LocalDate entryDate;

    @Column(name = "entry_price", precision = 28, scale = 8)
    private BigDecimal entryPrice;

    @Column(name = "exit_date")
    private LocalDate exitDate;

    @Column(name = "exit_price", precision = 28, scale = 8)
    private BigDecimal exitPrice;

    @Column(name = "quantity", precision = 28, scale = 8)
    private BigDecimal quantity;

    @Column(name = "pnl", precision = 28, scale = 8)
    private BigDecimal pnl;

    @Column(name = "holding_bars")
    private Integer holdingBars;
}
