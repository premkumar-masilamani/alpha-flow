package com.alphaflow.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "renko_data")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "ticker")
public class RenkoData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long renkoDataId;

    @Column(name = "renko_date", nullable = false)
    private LocalDate renkoDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticker_id", nullable = false)
    private Ticker ticker;

    @Column(name = "brick_low", nullable = false, precision = 38, scale = 2)
    private BigDecimal brickLow;

    @Column(name = "brick_high", nullable = false, precision = 38, scale = 2)
    private BigDecimal brickHigh;

    @Column(nullable = false)
    private String direction;

    @Column(nullable = false)
    private Integer trend;

    @Column(nullable = false)
    private Integer zone;

}
