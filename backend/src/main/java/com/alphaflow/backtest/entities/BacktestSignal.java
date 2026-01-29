package com.alphaflow.backtest.entities;

import com.alphaflow.backtest.enums.PositionType;
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

    private String strategyName;

    private LocalDate signalDate;

    private LocalDate executeDate;

    @Enumerated(EnumType.STRING)
    private TradeSignal action;

    @Enumerated(EnumType.STRING)
    private PositionType fromPosition;

    @Enumerated(EnumType.STRING)
    private PositionType toPosition;
}
