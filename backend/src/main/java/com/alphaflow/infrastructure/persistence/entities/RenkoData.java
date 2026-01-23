package com.alphaflow.infrastructure.persistence.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "renko_data")
@Data
@ToString(exclude = "ticker")
public class RenkoData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long renkoDataId;

    private LocalDate renkoDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticker_id")
    private Ticker ticker;

    private BigDecimal brickLow;

    private BigDecimal brickHigh;

    private String direction;

    private Integer trend;

}
