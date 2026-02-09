package com.alphaflow.backtest.entities;

import com.alphaflow.infrastructure.enums.RenkoPriceSource;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Table(name = "backtest_strategies")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BacktestStrategy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long backtestStrategyId;

    @Column(unique = true, nullable = false)
    private String name;

    @Column(name = "strategy_type", nullable = false, length = 100)
    private String strategyType;

    @Enumerated(EnumType.STRING)
    @Column(name = "price_source", length = 50)
    private RenkoPriceSource priceSource;

    @OneToMany(mappedBy = "backtestStrategy", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BacktestStrategyIndicator> indicators;
}
