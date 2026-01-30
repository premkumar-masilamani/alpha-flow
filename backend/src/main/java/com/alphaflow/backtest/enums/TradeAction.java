package com.alphaflow.backtest.enums;

import java.util.Collections;
import java.util.Map;

public record TradeAction(TradeSignal tradeSignal, PositionType positionType, Map<String, Object> signalData) {
    public TradeAction(TradeSignal tradeSignal, PositionType positionType) {
        this(tradeSignal, positionType, Collections.emptyMap());
    }
}
