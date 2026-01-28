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
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticker_id")
    private Ticker ticker;

    @Column(name = "strategy_name")
    private String strategyName;

    @Column(name = "cagr", precision = 28, scale = 8)
    private BigDecimal cagr;

    @Column(name = "initial_equity", precision = 28, scale = 8)
    private BigDecimal initialEquity;

    @Column(name = "final_equity", precision = 28, scale = 8)
    private BigDecimal finalEquity;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "years", precision = 28, scale = 8)
    private BigDecimal years;
}
