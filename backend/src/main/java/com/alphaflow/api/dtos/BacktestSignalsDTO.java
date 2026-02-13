package com.alphaflow.api.dtos;

import com.alphaflow.backtest.enums.TradeSignal;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.time.LocalDate;

@Builder
public record BacktestSignalsDTO(
        @JsonProperty("strategy") String strategyName,
        @JsonProperty("date") LocalDate signalDate,
        @JsonProperty("action") TradeSignal action
) {
}
