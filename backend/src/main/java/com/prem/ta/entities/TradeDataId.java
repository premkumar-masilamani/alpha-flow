package com.prem.ta.entities;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Objects;

public class TradeDataId implements Serializable {

    private OffsetDateTime tradeTime;
    private Integer ticker;

    public TradeDataId() {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TradeDataId that)) return false;
        return (
                Objects.equals(tradeTime, that.tradeTime) &&
                        Objects.equals(ticker, that.ticker)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(tradeTime, ticker);
    }
}
