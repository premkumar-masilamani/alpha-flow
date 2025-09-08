package com.prem.ta.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "trade_data")
@IdClass(TradeDataId.class)
public class TradeData {

    @Id
    private Long tickerId;

    @Id
    private Short intervalId;

    @Id
    private OffsetDateTime tradeTime;

    private Double priceOpen;
    private Double priceHigh;
    private Double priceLow;
    private Double priceClose;
    private Double volume;
    private Double vwap;
    private Double buyerCapitalRatio;
    private Double buyerParticipationRatio;
    private Double buyerVolumeRatio;
    private Double whaleImpact;

    public Long getTickerId() {
        return tickerId;
    }

    public void setTickerId(Long tickerId) {
        this.tickerId = tickerId;
    }

    public Short getIntervalId() {
        return intervalId;
    }

    public void setIntervalId(Short intervalId) {
        this.intervalId = intervalId;
    }

    public OffsetDateTime getTradeTime() {
        return tradeTime;
    }

    public void setTradeTime(OffsetDateTime tradeTime) {
        this.tradeTime = tradeTime;
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

    public Double getBuyerParticipationRatio() {
        return buyerParticipationRatio;
    }

    public void setBuyerParticipationRatio(Double buyerParticipationRatio) {
        this.buyerParticipationRatio = buyerParticipationRatio;
    }

    public Double getBuyerVolumeRatio() {
        return buyerVolumeRatio;
    }

    public void setBuyerVolumeRatio(Double buyerVolumeRatio) {
        this.buyerVolumeRatio = buyerVolumeRatio;
    }

    public Double getWhaleImpact() {
        return whaleImpact;
    }

    public void setWhaleImpact(Double whaleImpact) {
        this.whaleImpact = whaleImpact;
    }
}
