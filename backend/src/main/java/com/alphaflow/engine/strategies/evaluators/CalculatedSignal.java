package com.alphaflow.engine.strategies.evaluators;

import com.alphaflow.persistence.enums.TradeAction;

public record CalculatedSignal(TradeAction signal, String value) {}
