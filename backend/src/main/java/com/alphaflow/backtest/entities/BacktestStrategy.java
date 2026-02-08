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
    private Long backtestStrategyId;

    @Column(unique = true, nullable = false)
    private String name;

    @Column(nullable = false)
    private String strategyType;

    private String priceSource;

    @OneToMany(mappedBy = "backtestStrategy", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    private List<BacktestIndicator> indicators = new ArrayList<>();
}
