package com.alphaflow.engine.calculators.dtos;

import com.alphaflow.engine.calculators.enums.LevelType;
import com.alphaflow.engine.indicators.utils.IndicatorMath;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;

/**
 * Represents a validated horizontal support or resistance price band for a snapshot date,
 * encapsulating its price boundaries, derived midpoint, structural role, and surviving swing touch
 * metadata.
 */
@Getter
public class Bucket {

  /** Snapshot date of the latest price bar used to evaluate this zone. */
  private final LocalDate priceDate;

  /** Lower boundary of the price band at published precision. */
  private final BigDecimal bottom;

  /** Upper boundary of the price band at published precision. */
  private final BigDecimal top;

  /** Published arithmetic midpoint of {@code bottom} and {@code top}. */
  private final BigDecimal midpoint;

  /** Active structural role of the zone relative to the latest closing price. */
  private final LevelType levelType;

  /** Number of surviving confirmed swing points clustered within this zone. */
  private final int touchCount;

  /** Date of the earliest surviving swing touch belonging to this zone. */
  private final LocalDate firstTouchDate;

  /** Date of the most recent surviving swing touch belonging to this zone. */
  private final LocalDate lastTouchDate;

  /**
   * Constructs a support/resistance zone bucket and computes its published midpoint from the
   * supplied boundaries.
   *
   * @param priceDate snapshot date of the latest evaluated bar
   * @param bottom lower zone boundary
   * @param top upper zone boundary
   * @param levelType final classified role ({@link LevelType#SUPPORT} or {@link
   *     LevelType#RESISTANCE})
   * @param touchCount count of surviving swing touches inside the zone
   * @param firstTouchDate earliest surviving swing date
   * @param lastTouchDate most recent surviving swing date
   */
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
        IndicatorMath.publish(IndicatorMath.divide(bottom.add(top), BigDecimal.valueOf(2)));
    this.levelType = levelType;
    this.touchCount = touchCount;
    this.firstTouchDate = firstTouchDate;
    this.lastTouchDate = lastTouchDate;
  }
}
