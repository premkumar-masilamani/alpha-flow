package com.alphaflow.infrastructure.persistence.entities;

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

    @Column(name = "action")
    private String action; // ENTER_LONG, ENTER_SHORT, EXIT, HOLD

    @Column(name = "from_position")
    private String fromPosition;

    @Column(name = "to_position")
    private String toPosition;
}
