package com.alphaflow.engine.calculators.dtos;

import com.alphaflow.engine.calculators.enums.LevelType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Bucket {

  private final LocalDate priceDate;
  private final BigDecimal bottom;
  private final BigDecimal top;
  private final BigDecimal midpoint;
  private LevelType levelType;
  private int touchCount;
  private LocalDate firstTouchDate;
  private LocalDate lastTouchDate;
  private int consecutiveBreachCount;

  public Bucket(LocalDate priceDate, BigDecimal bottom, BigDecimal top, LevelType levelType) {
    this.priceDate = priceDate;
    this.bottom = bottom;
    this.top = top;
    this.midpoint = bottom.add(top).divide(BigDecimal.valueOf(2), 18, RoundingMode.HALF_UP);
    this.levelType = levelType;
    this.touchCount = 0;
    this.firstTouchDate = null;
    this.lastTouchDate = null;
    this.consecutiveBreachCount = 0;
  }

  public void incrementTouchCount() {
    this.touchCount++;
  }

  public void incrementTouchCount(LocalDate barDate) {
    this.touchCount++;
    if (this.firstTouchDate == null) {
      this.firstTouchDate = barDate;
    }
    this.lastTouchDate = barDate;
  }

  public void incrementConsecutiveBreachCount() {
    this.consecutiveBreachCount++;
  }

  public void resetConsecutiveBreachCount() {
    this.consecutiveBreachCount = 0;
  }

  public void resetTouchCount() {
    this.touchCount = 0;
    this.firstTouchDate = null;
    this.lastTouchDate = null;
    this.consecutiveBreachCount = 0;
  }
}
