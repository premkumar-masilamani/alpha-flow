package com.prem.ta.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "market_data")
@Data
@ToString(exclude = "ticker")
public class MarketData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long marketDataId;

    private LocalDate marketDataDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticker_id")
    private Ticker ticker;

    private BigDecimal priceOpen;

    private BigDecimal priceHigh;

    private BigDecimal priceLow;

    private BigDecimal priceClose;

    private BigDecimal volume;

    private BigDecimal vwap;

    @Column(name = "volume_profile_poc")
    private BigDecimal volumeProfilePOC;

    @Column(name = "volume_profile_vah")
    private BigDecimal volumeProfileVAH;

    @Column(name = "volume_profile_val")
    private BigDecimal volumeProfileVAL;

    private Double buyerVolumeShare;

    private Double buyerCapitalShare;
}