package com.alphaflow.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "indicators")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "ticker")
public class Indicator {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "indicator_id")
    private Long indicatorId;

    @Column(name = "indicator_date", nullable = false)
    private LocalDate indicatorDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticker_id", nullable = false)
    private Ticker ticker;

    @Column(name = "metric", nullable = false)
    private String metric;

    @Column(name = "ma_type", nullable = false)
    private String maType;

    @Column(name = "period", nullable = false)
    private Integer period;

    @Column(name = "value", nullable = false, precision = 38, scale = 2)
    private BigDecimal value;

}
