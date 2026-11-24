package com.alphaflow.engine.structure;

import com.alphaflow.engine.indicators.dtos.PriceBar;
import com.alphaflow.engine.structure.dtos.SwingPoint;
import com.alphaflow.engine.structure.enums.SwingType;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Reusable Dow Theory market structure detector using a zero-lookback, Close-confirmed rolling
 * Wilder ATR state machine.
 *
 * <p>Pivot prices are tracked using bar wick extremes ({@code high} and {@code low}), while wave
 * reversals require the bar {@code close} to retrace by at least {@code swingAtrMultiplier *
 * ATR[i]} from the tracked extreme. Separating wick anchoring from Close confirmation prevents
 * single-bar outside-candle ordering ambiguity and filters intraday rejection wicks, producing a
 * strictly alternating sequence of confirmed Swing Highs and Swing Lows.
 */
@Component
@Slf4j
public class DowAtrSwingDetector {

  /**
   * Detects confirmed Dow Theory Swing Highs and Swing Lows across a chronological price series
   * using a precomputed Wilder ATR series.
   *
   * @param bars chronologically ordered OHLCV price bars
   * @param atrSeries precomputed Wilder ATR series aligned 1-to-1 with {@code bars}
   * @param atrPeriod Wilder ATR smoothing period used to compute {@code atrSeries}
   * @param swingAtrMultiplier multiple of rolling ATR required in Close retracement to confirm a
   *     wave reversal
   * @return strictly alternating list of confirmed swing pivots, or empty list if inputs are
   *     invalid or insufficient
   */
  public List<SwingPoint> detectSwings(
      List<PriceBar> bars,
      List<BigDecimal> atrSeries,
      int atrPeriod,
      BigDecimal swingAtrMultiplier) {
    if (bars != null
        && atrSeries != null
        && atrPeriod > 0
        && swingAtrMultiplier != null
        && swingAtrMultiplier.signum() > 0
        && bars.size() > atrPeriod
        && atrSeries.size() == bars.size()) {
      List<SwingPoint> swings = runWaveStateMachine(bars, atrSeries, atrPeriod, swingAtrMultiplier);
      log.debug(
          "Detected {} confirmed Dow-ATR swings across {} bars (atrPeriod={}, multiplier={}).",
          swings.size(),
          bars.size(),
          atrPeriod,
          swingAtrMultiplier);
      return swings;
    } else {
      log.debug(
          "Skipping Dow-ATR swing detection due to invalid parameters or insufficient bars "
              + "(atrPeriod={}, multiplier={}).",
          atrPeriod,
          swingAtrMultiplier);
      return List.of();
    }
  }

  /**
   * Executes the single-pass Dow-ATR wave state machine from the first post-seed bar onward.
   *
   * <p>The state machine begins in an undetermined wave ({@code activeWave == null}) tracking both
   * expanding high and low extremes until a Close retracement confirms the initial structural
   * pivot. Once initialized, it alternates strictly between seeking a {@link SwingType#HIGH} peak
   * and seeking a {@link SwingType#LOW} trough, locking each pivot only after price closes beyond
   * that bar's volatility-adjusted reversal threshold.
   */
  private List<SwingPoint> runWaveStateMachine(
      List<PriceBar> bars,
      List<BigDecimal> atrSeries,
      int atrPeriod,
      BigDecimal swingAtrMultiplier) {
    List<SwingPoint> swings = new ArrayList<>();

    // Anchor initial extremes at the first bar with a defined Wilder ATR value (atrPeriod - 1).
    int seedIndex = atrPeriod - 1;
    SwingType activeWave = null;
    int extremeHighIndex = seedIndex;
    BigDecimal extremeHigh = bars.get(seedIndex).high();
    int extremeLowIndex = seedIndex;
    BigDecimal extremeLow = bars.get(seedIndex).low();

    for (int i = atrPeriod; i < bars.size(); i++) {
      PriceBar currentBar = bars.get(i);
      // Scale the reversal threshold to bar i's contemporaneous ATR for scale/regime invariance.
      BigDecimal reversalThreshold = atrSeries.get(i).multiply(swingAtrMultiplier);

      if (reversalThreshold.signum() > 0) {
        if (activeWave == null) {
          // Undetermined initial trend: track both extremes until Close confirms the first turn.
          if (currentBar.high().compareTo(extremeHigh) > 0) {
            extremeHigh = currentBar.high();
            extremeHighIndex = i;
          }
          if (currentBar.low().compareTo(extremeLow) < 0) {
            extremeLow = currentBar.low();
            extremeLowIndex = i;
          }
          if (extremeHigh.subtract(currentBar.close()).compareTo(reversalThreshold) >= 0) {
            // Downward Close retracement confirmed the tracked peak; transition to downswing leg.
            swings.add(
                createSwingPoint(bars, atrSeries, extremeHighIndex, extremeHigh, SwingType.HIGH));
            activeWave = SwingType.LOW;
            extremeLow = currentBar.low();
            extremeLowIndex = i;
          } else if (currentBar.close().subtract(extremeLow).compareTo(reversalThreshold) >= 0) {
            // Upward Close rally confirmed the tracked trough; transition to upswing leg.
            swings.add(
                createSwingPoint(bars, atrSeries, extremeLowIndex, extremeLow, SwingType.LOW));
            activeWave = SwingType.HIGH;
            extremeHigh = currentBar.high();
            extremeHighIndex = i;
          }
        } else if (activeWave == SwingType.HIGH) {
          // Active upswing: extend peak on higher highs until Close drops by reversalThreshold.
          if (currentBar.high().compareTo(extremeHigh) > 0) {
            extremeHigh = currentBar.high();
            extremeHighIndex = i;
          }
          if (extremeHigh.subtract(currentBar.close()).compareTo(reversalThreshold) >= 0) {
            // Lock confirmed Swing High and begin tracking the subsequent Swing Low from this bar.
            swings.add(
                createSwingPoint(bars, atrSeries, extremeHighIndex, extremeHigh, SwingType.HIGH));
            activeWave = SwingType.LOW;
            extremeLow = currentBar.low();
            extremeLowIndex = i;
          }
        } else {
          // Active downswing: extend trough on lower lows until Close rallies by reversalThreshold.
          if (currentBar.low().compareTo(extremeLow) < 0) {
            extremeLow = currentBar.low();
            extremeLowIndex = i;
          }
          if (currentBar.close().subtract(extremeLow).compareTo(reversalThreshold) >= 0) {
            // Lock confirmed Swing Low and begin tracking the subsequent Swing High from this bar.
            swings.add(
                createSwingPoint(bars, atrSeries, extremeLowIndex, extremeLow, SwingType.LOW));
            activeWave = SwingType.HIGH;
            extremeHigh = currentBar.high();
            extremeHighIndex = i;
          }
        }
      }
    }
    return swings;
  }

  /**
   * Builds a {@link SwingPoint} snapshot anchored at the exact pivot bar's date, wick extreme
   * price, and contemporaneous Wilder ATR.
   */
  private SwingPoint createSwingPoint(
      List<PriceBar> bars,
      List<BigDecimal> atrSeries,
      int barIndex,
      BigDecimal price,
      SwingType swingType) {
    return new SwingPoint(
        barIndex, bars.get(barIndex).date(), price, swingType, atrSeries.get(barIndex));
  }
}
