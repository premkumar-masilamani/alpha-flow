package com.alphaflow.backtest.enums;

public record TradeSignal(TradeAction action, PositionType targetPosition) {
}