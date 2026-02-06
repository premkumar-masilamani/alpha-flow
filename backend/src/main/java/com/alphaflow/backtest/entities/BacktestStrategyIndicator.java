package com.alphaflow.backtest.entities;

import com.alphaflow.backtest.enums.IndicatorRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "backtest_strategy_indicators")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BacktestStrategyIndicator {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long backtestStrategyIndicatorId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "backtest_strategy_id", nullable = false)
    private BacktestStrategy backtestStrategy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IndicatorRole indicatorRole;

    @Column(nullable = false)
    private String metric;

    @Column(nullable = false)
    private String transformation;

    @Column(nullable = false)
    private Integer period;
}
