package com.alphaflow.infrastructure.persistence.entities;

import com.alphaflow.domain.enums.PositionType;
import com.alphaflow.domain.enums.TradeAction;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "backtest_signal_intent")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BacktestSignalIntent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticker_id")
    private Ticker ticker;

    @Column(name = "strategy_name")
    private String strategyName;

    @Column(name = "signal_date")
    private LocalDate signalDate;

    @Column(name = "execute_date")
    private LocalDate executeDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "action")
    private TradeAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_position")
    private PositionType fromPosition;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_position")
    private PositionType toPosition;
}
