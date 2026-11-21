package com.alphaflow.engine.calculators.dtos;

import com.alphaflow.engine.calculators.enums.LevelType;
import com.alphaflow.engine.indicators.utils.IndicatorMath;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;

@Getter
public class Bucket {

  private final LocalDate priceDate;
  private final BigDecimal bottom;
  private final BigDecimal top;
  private final BigDecimal midpoint;
  private final LevelType levelType;
  private final int touchCount;
  private final LocalDate firstTouchDate;
  private final LocalDate lastTouchDate;

  public Bucket(
      LocalDate priceDate,
      BigDecimal bottom,
      BigDecimal top,
      LevelType levelType,
      int touchCount,
      LocalDate firstTouchDate,
      LocalDate lastTouchDate) {
    this.priceDate = priceDate;
    this.bottom = bottom;
    this.top = top;
    this.midpoint =
        IndicatorMath.publish(
            IndicatorMath.divide(bottom.add(top), BigDecimal.valueOf(2)));
    this.levelType = levelType;
    this.touchCount = touchCount;
    this.firstTouchDate = firstTouchDate;
    this.lastTouchDate = lastTouchDate;
  }
}
