package com.alphaflow.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "renko")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "ticker")
public class Renko {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long renkoId;

    private LocalDate renkoDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticker_id")
    private Ticker ticker;

    private BigDecimal brickLow;

    private BigDecimal brickHigh;

    private String direction;

    private Integer trend;

    private Integer zone;

}
