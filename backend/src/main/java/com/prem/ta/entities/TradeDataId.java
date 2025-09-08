package com.prem.ta.entities;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Objects;

public class TradeDataId implements Serializable {

    private Long tickerId;
    private Short intervalId;
    private OffsetDateTime tradeTime;

    public TradeDataId() {
    }

    public TradeDataId(Long tickerId, Short intervalId, OffsetDateTime tradeTime) {
        this.tickerId = tickerId;
        this.intervalId = intervalId;
        this.tradeTime = tradeTime;
    }

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TradeDataId that = (TradeDataId) o;
        return Objects.equals(tickerId, that.tickerId) &&
                Objects.equals(intervalId, that.intervalId) &&
                Objects.equals(tradeTime, that.tradeTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tickerId, intervalId, tradeTime);
    }
}
