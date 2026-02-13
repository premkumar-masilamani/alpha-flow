package com.alphaflow.backtest.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "backtest_strategies")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "indicators")
public class BacktestStrategy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "backtest_strategy_id")
    private Long backtestStrategyId;

    @Column(name = "name", unique = true, nullable = false, length = 255)
    private String name;

    @Column(name = "strategy_type", nullable = false, length = 100)
    private String strategyType;

    @Column(name = "price_source", length = 50)
    private String priceSource;

    @OneToMany(mappedBy = "backtestStrategy", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    private List<BacktestIndicator> indicators = new ArrayList<>();
}
