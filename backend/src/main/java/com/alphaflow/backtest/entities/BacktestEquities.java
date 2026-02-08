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
@Table(name = "backtest_equities")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BacktestEquities {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long backtestEquitiesId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticker_id")
    private Ticker ticker;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "backtest_strategy_id")
    private BacktestStrategy strategy;

    private LocalDate equityDate;

    private BigDecimal equity;

    @Enumerated(EnumType.STRING)
    private PositionType position;

    private BigDecimal priceClose;
}
