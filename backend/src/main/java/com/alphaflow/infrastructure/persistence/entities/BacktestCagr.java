package com.alphaflow.infrastructure.persistence.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "backtest_cagr")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BacktestCagr {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long backtestCagrId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticker_id")
    private Ticker ticker;

    private String strategyName;

    private BigDecimal cagr;

    private BigDecimal initialEquity;

    private BigDecimal finalEquity;

    private LocalDate startDate;

    private LocalDate endDate;

    private BigDecimal years;

    private BigDecimal winRate;
}
