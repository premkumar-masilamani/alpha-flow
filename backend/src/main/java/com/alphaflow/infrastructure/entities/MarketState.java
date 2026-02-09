package com.alphaflow.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "market_state")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "ticker")
public class MarketState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long marketStateId;

    @Column(name = "market_state_date", nullable = false)
    private LocalDate marketStateDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticker_id", nullable = false)
    private Ticker ticker;

    @Column(nullable = false)
    private String metric;

    @Column(name = "ma_type", nullable = false)
    private String maType;

    @Column(nullable = false)
    private Integer period;

    @Column(nullable = false, precision = 38, scale = 2)
    private BigDecimal value;

}
