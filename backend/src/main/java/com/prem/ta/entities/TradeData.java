package com.prem.ta.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "trade_data")
@IdClass(TradeDataId.class)
@Getter
@Setter
public class TradeData {

    @Id
    @Column(name = "trade_time", nullable = false)
    private OffsetDateTime tradeTime;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticker_id", nullable = false)
    private Ticker ticker;

    @Column(name = "price_open")
    private Double priceOpen;

    @Column(name = "price_high")
    private Double priceHigh;

    @Column(name = "price_low")
    private Double priceLow;

    @Column(name = "price_close")
    private Double priceClose;

    @Column(name = "volume")
    private Double volume;

    @Column(name = "vwap")
    private Double vwap;

    @Column(name = "buyer_volume_ratio")
    private Float buyerVolumeRatio;

    @Column(name = "buyer_capital_ratio")
    private Float buyerCapitalRatio;
}
