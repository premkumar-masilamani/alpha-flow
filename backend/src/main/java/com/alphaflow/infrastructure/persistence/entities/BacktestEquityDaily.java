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
    private Long backtestEquityDailyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticker_id")
    private Ticker ticker;

    private String strategyName;

    private LocalDate equityDate;

    private BigDecimal equity;

    @Enumerated(EnumType.STRING)
    private PositionType position;

    private BigDecimal priceClose;
}
