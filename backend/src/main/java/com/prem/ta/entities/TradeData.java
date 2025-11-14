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
    private OffsetDateTime tradeTime;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticker_id", nullable = false)
    private Ticker ticker;

    private Double priceOpen;

    private Double priceHigh;

    private Double priceLow;

    private Double priceClose;

    private Double volume;

    private Double vwap;

    private Float buyerVolumeRatio;

    private Float buyerCapitalRatio;
}
