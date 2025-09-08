package com.prem.ta.entities;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "trade_data")
@IdClass(TradeDataId.class)
public class TradeData {

    @Id
    @Column(name = "trade_time", nullable = false)
    private OffsetDateTime tradeTime;

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticker_id", nullable = false, updatable = false,
            foreignKey = @ForeignKey(name = "fk_trade_data_ticker"))
    private Ticker ticker;

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "interval_id", nullable = false, updatable = false,
            foreignKey = @ForeignKey(name = "fk_trade_data_interval"))
    private Interval interval;

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

    @Column(name = "buyer_capital_ratio")
    private Double buyerCapitalRatio;

    @Column(name = "buyer_volume_ratio")
    private Double buyerVolumeRatio;

    @Column(name = "trades_per_sec")
    private Double tradesPerSec;

    @Column(name = "micro_volatility")
    private Double microVolatility;

    @Column(name = "avg_inter_trade_ms")
    private Double avgInterTradeMs;

    @Column(name = "vpin")
    private Double vpin;

    public OffsetDateTime getTradeTime() {
        return tradeTime;
    }

    public void setTradeTime(OffsetDateTime tradeTime) {
        this.tradeTime = tradeTime;
    }

    public Ticker getTicker() {
        return ticker;
    }

    public void setTicker(Ticker ticker) {
        this.ticker = ticker;
    }

    public Interval getInterval() {
        return interval;
    }

    public void setInterval(Interval interval) {
        this.interval = interval;
    }

    public Double getPriceOpen() {
        return priceOpen;
    }

    public void setPriceOpen(Double priceOpen) {
        this.priceOpen = priceOpen;
    }

    public Double getPriceHigh() {
        return priceHigh;
    }

    public void setPriceHigh(Double priceHigh) {
        this.priceHigh = priceHigh;
    }

    public Double getPriceLow() {
        return priceLow;
    }

    public void setPriceLow(Double priceLow) {
        this.priceLow = priceLow;
    }

    public Double getPriceClose() {
        return priceClose;
    }

    public void setPriceClose(Double priceClose) {
        this.priceClose = priceClose;
    }

    public Double getVolume() {
        return volume;
    }

    public void setVolume(Double volume) {
        this.volume = volume;
    }

    public Double getVwap() {
        return vwap;
    }

    public void setVwap(Double vwap) {
        this.vwap = vwap;
    }

    public Double getBuyerCapitalRatio() {
        return buyerCapitalRatio;
    }

    public void setBuyerCapitalRatio(Double buyerCapitalRatio) {
        this.buyerCapitalRatio = buyerCapitalRatio;
    }

    public Double getBuyerVolumeRatio() {
        return buyerVolumeRatio;
    }

    public void setBuyerVolumeRatio(Double buyerVolumeRatio) {
        this.buyerVolumeRatio = buyerVolumeRatio;
    }

    public Double getTradesPerSec() {
        return tradesPerSec;
    }

    public void setTradesPerSec(Double tradesPerSec) {
        this.tradesPerSec = tradesPerSec;
    }

    public Double getMicroVolatility() {
        return microVolatility;
    }

    public void setMicroVolatility(Double microVolatility) {
        this.microVolatility = microVolatility;
    }

    public Double getAvgInterTradeMs() {
        return avgInterTradeMs;
    }

    public void setAvgInterTradeMs(Double avgInterTradeMs) {
        this.avgInterTradeMs = avgInterTradeMs;
    }

    public Double getVpin() {
        return vpin;
    }

    public void setVpin(Double vpin) {
        this.vpin = vpin;
    }
}
