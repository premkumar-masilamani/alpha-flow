package com.alphaflow.backtest.entities;

import com.alphaflow.backtest.enums.IndicatorRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "backtest_indicators")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BacktestIndicator {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "backtest_indicator_id")
    private Long backtestIndicatorId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "backtest_strategy_id", nullable = false)
    private BacktestStrategy backtestStrategy;

    @Enumerated(EnumType.STRING)
    @Column(name = "indicator_role", nullable = false, length = 100)
    private IndicatorRole indicatorRole;

    @Column(name = "metric", nullable = false, length = 100)
    private String metric;

    @Column(name = "transformation", nullable = false, length = 20)
    private String transformation;

    @Column(name = "period", nullable = false)
    private Integer period;
}
