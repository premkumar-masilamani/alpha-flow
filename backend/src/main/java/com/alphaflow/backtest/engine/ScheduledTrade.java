package com.alphaflow.backtest.engine;

import com.alphaflow.backtest.enums.TradeAction;

import java.time.LocalDate;

public record ScheduledTrade(TradeAction action, LocalDate executeDate) {
}
