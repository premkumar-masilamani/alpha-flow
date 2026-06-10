package com.alphaflow.persistence.enums;

import lombok.Getter;

/** Represents the technical analysis evaluator detail/signal and error message values. */
@Getter
public enum EvaluatorMessage {
  // EMA Evaluator Messages
  INSUFFICIENT_EMA_DATA("Insufficient EMA data"),
  MISSING_EMA_VALUES("Missing EMA values"),
  EMA_5_POS_CROSS_13_26("5 EMA Positive Crossover with 13 & 26 EMA"),
  EMA_5_NEG_CROSS_13_26("5 EMA Negative Crossover with 13 & 26 EMA"),
  EMA_STRONG_BUY("EMA 5 > 13 > 26"),
  EMA_STRONG_SELL("EMA 5 < 13 < 26"),
  MIXED_EMAS("Mixed EMAs"),

  // RSI Evaluator Messages
  INSUFFICIENT_RSI_DATA("Insufficient RSI data"),
  MISSING_RSI_VALUES("Missing RSI values"),
  UPTICK("Uptick"),
  DOWNTICK("Downtick"),
  FLAT("Flat"),

  // Stochastic Evaluator Messages
  INSUFFICIENT_STOCH_DATA("Insufficient stochastic data"),
  MISSING_STOCH_VALUES("Missing Stoch K/D values"),
  POSITIVE_CROSSOVER("Positive Crossover"),
  NEGATIVE_CROSSOVER("Negative Crossover"),
  K_ABOVE_D("K > D"),
  K_BELOW_D("K < D"),
  K_EQUAL_D("K = D"),

  // Volume Evaluator Messages
  INSUFFICIENT_PRICE_DATA("Insufficient price data"),
  INSUFFICIENT_VOLUME_SMA_DATA("Insufficient volume SMA data"),
  MISSING_VOLUME_SMA_VALUE("Missing volume SMA value"),
  GREEN_CANDLE_HEAVY_VOLUME("Green Candle with Heavy Volume"),
  RED_CANDLE_HEAVY_VOLUME("Red Candle with Heavy Volume"),
  DOJI_HEAVY_VOLUME("Doji with Heavy Volume"),
  GREEN_CANDLE_NORMAL_VOLUME("Green Candle with Normal Volume"),
  RED_CANDLE_NORMAL_VOLUME("Red Candle with Normal Volume"),
  DOJI_NORMAL_VOLUME("Doji with Normal Volume"),

  // MACD Evaluator Messages
  INSUFFICIENT_WEEKLY_MACD_DATA("Insufficient weekly MACD data"),
  MISSING_MACD_VALUES("Missing MACD/Signal values"),
  FLAT_AFTER_DOWN_RARE("Flat after down (rare)"),
  FLAT_AFTER_UP_RARE("Flat after up (rare)"),
  MACD_EQUAL_SIGNAL("MACD = Signal");

  private final String value;

  EvaluatorMessage(String value) {
    this.value = value;
  }
}
