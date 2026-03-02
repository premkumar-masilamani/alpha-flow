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
@Table(name = "backtest_signals")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BacktestSignal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "backtest_signal_id")
    private Long backtestSignalId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticker_id", nullable = false)
    private Ticker ticker;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "backtest_strategy_id")
    private BacktestStrategy strategy;

    @Column(name = "signal_date", nullable = false)
    private LocalDate signalDate;

    @Column(name = "execute_date")
    private LocalDate executeDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 50)
    private TradeSignal action;

    @Column(name = "signal_data", columnDefinition = "TEXT")
    private String signalData;

    @Column(name = "strategy_state", columnDefinition = "TEXT")
    private String strategyState;
}
