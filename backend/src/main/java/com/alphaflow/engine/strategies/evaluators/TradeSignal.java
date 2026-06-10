package com.alphaflow.engine.strategies.evaluators;

import com.alphaflow.persistence.enums.TradeAction;

/**
 * Represents a calculated evaluation signal containing the recommended trade action and its reason.
 */
public record TradeSignal(TradeAction tradeAction, String reason) {}
