package com.alphaflow.infrastructure.persistence.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "backtest_results")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "ticker")
public class BacktestResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "date")
    private LocalDate date;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticker_id")
    private Ticker ticker;

    @Column(name = "strategy_name")
    private String strategyName;

    @Column(name = "equity", precision = 19, scale = 4)
    private BigDecimal equity;

    @Column(name = "position")
    private String position; // LONG, SHORT, NONE

    @Column(name = "price", precision = 19, scale = 4)
    private BigDecimal price; // price_close of the day

    @Column(name = "signal")
    private String signal;
}
