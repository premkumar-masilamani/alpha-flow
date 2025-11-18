package com.prem.ta.entities;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "trade_data", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"ticker_id", "trade_date"})
})
@Data
public class TradeData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long tradeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticker_id", nullable = false)
    private Ticker ticker;

    @Column(name = "trade_date", nullable = false)
    private LocalDate tradeDate;

    @Column(name = "price_open", precision = 18, scale = 8)
    private BigDecimal priceOpen;

    @Column(name = "price_high", precision = 18, scale = 8)
    private BigDecimal priceHigh;

    @Column(name = "price_low", precision = 18, scale = 8)
    private BigDecimal priceLow;

    @Column(name = "price_close", precision = 18, scale = 8)
    private BigDecimal priceClose;

    @Column(name = "volume", precision = 32, scale = 8)
    private BigDecimal volume;

    @Column(name = "vwap", precision = 18, scale = 8)
    private BigDecimal vwap;

    @Column(name = "buyer_volume_ratio")
    private Double buyerVolumeRatio;

    @Column(name = "buyer_capital_ratio")
    private Double buyerCapitalRatio;
}