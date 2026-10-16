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
public class TickerStrategyState {

  private LocalDate sessionDate;
  private boolean bearBreachNotified;
  private BigDecimal bearLowestPrice;
  private boolean bearReversalNotified;
  private boolean bullBreachNotified;
  private BigDecimal bullHighestPrice;
  private boolean bullReversalNotified;

  public TickerStrategyState(LocalDate sessionDate) {
    this.sessionDate = sessionDate;
  }

  public void resetForNewSession(LocalDate newSessionDate) {
    this.sessionDate = newSessionDate;
    this.bearBreachNotified = false;
    this.bearLowestPrice = null;
    this.bearReversalNotified = false;
    this.bullBreachNotified = false;
    this.bullHighestPrice = null;
    this.bullReversalNotified = false;
  }
}
