package com.alphaflow.infrastructure.persistence.entities;

import com.alphaflow.domain.enums.PositionType;
import com.alphaflow.domain.enums.TradeAction;
import jakarta.persistence.*;
import lombok.*;

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
    private TradeAction action;

    @Enumerated(EnumType.STRING)
    private PositionType fromPosition;

    @Enumerated(EnumType.STRING)
    private PositionType toPosition;
}
