package com.alphaflow.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "renko_data")
@Data
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "ticker")
public class RenkoData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "renko_data_id")
    private Long renkoDataId;

    @Column(name = "renko_date", nullable = false)
    private LocalDate renkoDataDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticker_id", nullable = false)
    private Ticker ticker;

    @Column(name = "brick_low", nullable = false, precision = 38, scale = 2)
    private BigDecimal brickLow;

    @Column(name = "brick_high", nullable = false, precision = 38, scale = 2)
    private BigDecimal brickHigh;

    @Column(name = "direction", nullable = false)
    private String direction;

    @Column(name = "trend", nullable = false)
    private Integer trend;

    @Column(name = "zone", nullable = false)
    private Integer zone;

}
