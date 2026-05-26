package com.alphaflow.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "weekly_candle_data")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "ticker")
public class WeeklyCandleData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "weekly_candle_data_id")
    private Long weeklyCandleDataId;

    @Column(name = "candle_date", nullable = false)
    private LocalDate candleDataDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticker_id", nullable = false)
    private Ticker ticker;

    @Column(name = "price_open", nullable = false, precision = 38, scale = 2)
    private BigDecimal priceOpen;

    @Column(name = "price_high", nullable = false, precision = 38, scale = 2)
    private BigDecimal priceHigh;

    @Column(name = "price_low", nullable = false, precision = 38, scale = 2)
    private BigDecimal priceLow;

    @Column(name = "price_close", nullable = false, precision = 38, scale = 2)
    private BigDecimal priceClose;

    @Column(name = "volume", nullable = false, precision = 38, scale = 2)
    private BigDecimal volume;
}
