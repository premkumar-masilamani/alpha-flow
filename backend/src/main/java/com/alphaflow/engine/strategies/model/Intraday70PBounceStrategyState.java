package com.alphaflow.engine.strategies.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Intraday70PBounceStrategyState {

  private LocalDate sessionDate;
  private LocalDate previousTradingDate;
  private BigDecimal previousDayHigh;
  private BigDecimal previousDayLow;

  private boolean bearBreachNotified;
  private BigDecimal bearLowestPrice;
  private boolean bearReversalNotified;
  private boolean bullBreachNotified;
  private BigDecimal bullHighestPrice;
  private boolean bullReversalNotified;

  public Intraday70PBounceStrategyState(LocalDate sessionDate) {
    this.sessionDate = sessionDate;
  }

  public Intraday70PBounceStrategyState(
      LocalDate sessionDate,
      LocalDate previousTradingDate,
      BigDecimal previousDayHigh,
      BigDecimal previousDayLow) {
    this.sessionDate = sessionDate;
    this.previousTradingDate = previousTradingDate;
    this.previousDayHigh = previousDayHigh;
    this.previousDayLow = previousDayLow;
  }

  public void resetForNewSession(LocalDate newSessionDate) {
    this.sessionDate = newSessionDate;
    this.previousTradingDate = null;
    this.previousDayHigh = null;
    this.previousDayLow = null;
    this.bearBreachNotified = false;
    this.bearLowestPrice = null;
    this.bearReversalNotified = false;
    this.bullBreachNotified = false;
    this.bullHighestPrice = null;
    this.bullReversalNotified = false;
  }
}
