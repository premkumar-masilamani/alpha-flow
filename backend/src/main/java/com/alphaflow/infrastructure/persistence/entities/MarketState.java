package com.alphaflow.infrastructure.persistence.entities;

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

    private LocalDate marketStateDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticker_id")
    private Ticker ticker;

    private String metric;

    private String maType;

    private Integer period;

    private BigDecimal value;

}
