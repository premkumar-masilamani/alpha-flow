package com.prem.ta.entities;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Objects;

public class TradeDataId implements Serializable {

    private OffsetDateTime tradeTime;
    private Long ticker;
    private Integer interval;

    public TradeDataId() {
    }

    public TradeDataId(OffsetDateTime tradeTime, Long ticker, Integer interval) {
        this.tradeTime = tradeTime;
        this.ticker = ticker;
        this.interval = interval;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TradeDataId that)) return false;
        return Objects.equals(tradeTime, that.tradeTime) &&
                Objects.equals(ticker, that.ticker) &&
                Objects.equals(interval, that.interval);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tradeTime, ticker, interval);
    }
}
