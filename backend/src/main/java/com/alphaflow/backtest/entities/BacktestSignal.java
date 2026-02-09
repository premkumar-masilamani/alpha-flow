package com.alphaflow.backtest.entities;

import com.alphaflow.backtest.enums.TradeSignal;
import com.alphaflow.infrastructure.entities.Ticker;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "backtest_signal")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BacktestSignal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long backtestSignalId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticker_id")
    private Ticker ticker;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "backtest_strategy_id")
    private BacktestStrategy strategy;

    private LocalDate signalDate;

    private LocalDate executeDate;

    @Enumerated(EnumType.STRING)
    private TradeSignal action;

    @Column(columnDefinition = "TEXT")
    private String signalData;
}
