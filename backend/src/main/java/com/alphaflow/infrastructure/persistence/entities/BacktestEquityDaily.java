package com.alphaflow.infrastructure.persistence.entities;

import com.alphaflow.domain.enums.PositionType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "backtest_equity_daily")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BacktestEquityDaily {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticker_id")
    private Ticker ticker;

    @Column(name = "strategy_name")
    private String strategyName;

    @Column(name = "date")
    private LocalDate date;

    @Column(name = "equity", precision = 28, scale = 8)
    private BigDecimal equity;

    @Enumerated(EnumType.STRING)
    @Column(name = "position")
    private PositionType position;

    @Column(name = "price_close", precision = 28, scale = 8)
    private BigDecimal priceClose;
}
