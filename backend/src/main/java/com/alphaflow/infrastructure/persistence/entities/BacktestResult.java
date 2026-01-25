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
    private Long id;

    private LocalDate date;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticker_id")
    private Ticker ticker;

    private String strategyName;

    @Column(precision = 19, scale = 4)
    private BigDecimal equity;

    private String position; // LONG, SHORT, NONE

    @Column(precision = 19, scale = 4)
    private BigDecimal price; // price_close of the day

    private String signal;
}
