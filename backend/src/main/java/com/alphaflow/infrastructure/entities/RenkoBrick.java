package com.alphaflow.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "renko_bricks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "ticker")
public class RenkoBrick {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long renkoBrickId;

    private LocalDate renkoBrickDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticker_id")
    private Ticker ticker;

    private BigDecimal brickLow;

    private BigDecimal brickHigh;

    private String direction;

    private Integer trend;

    private Integer zone;

}
