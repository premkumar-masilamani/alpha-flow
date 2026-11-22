package com.alphaflow.engine.structure;

import com.alphaflow.engine.indicators.dtos.PriceBar;
import com.alphaflow.engine.indicators.utils.IndicatorMath;
import com.alphaflow.engine.structure.dtos.SwingPoint;
import com.alphaflow.engine.structure.enums.SwingType;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Reusable Dow Theory market structure detector using a zero-lookback, Close-confirmed rolling
 * Wilder ATR state machine. Produces strictly alternating Swing Highs and Swing Lows for horizontal
 * S/R, angular trendlines, and classical chart pattern recognition.
 */
@Component
@Slf4j
public class DowAtrSwingDetector {

  /**
   * Detects confirmed Dow Theory Swing Highs and Swing Lows across a chronological price series.
   *
   * @param bars chronologically ordered OHLCV price bars
   * @param atrPeriod Wilder ATR smoothing period
   * @param swingAtrMultiplier multiple of rolling ATR required in Close retracement to confirm a
   *     wave reversal
   * @return strictly alternating list of confirmed swing pivots, or empty list if insufficient data
   */
  public List<SwingPoint> detectSwings(
      List<PriceBar> bars, int atrPeriod, BigDecimal swingAtrMultiplier) {
    if (bars != null
        && atrPeriod > 0
        && swingAtrMultiplier != null
        && swingAtrMultiplier.signum() > 0
        && bars.size() > atrPeriod) {
      List<BigDecimal> atrSeries = IndicatorMath.calculateWilderAtrSeries(bars, atrPeriod);
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

  private List<SwingPoint> runWaveStateMachine(
      List<PriceBar> bars,
      List<BigDecimal> atrSeries,
      int atrPeriod,
      BigDecimal swingAtrMultiplier) {
    List<SwingPoint> swings = new ArrayList<>();
    int seedIndex = atrPeriod - 1;
    SwingType activeWave = null;
    int extremeHighIndex = seedIndex;
    BigDecimal extremeHigh = bars.get(seedIndex).high();
    int extremeLowIndex = seedIndex;
    BigDecimal extremeLow = bars.get(seedIndex).low();

    for (int i = atrPeriod; i < bars.size(); i++) {
      PriceBar currentBar = bars.get(i);
      BigDecimal reversalThreshold = atrSeries.get(i).multiply(swingAtrMultiplier);

      if (reversalThreshold.signum() > 0) {
        if (activeWave == null) {
          if (currentBar.high().compareTo(extremeHigh) > 0) {
            extremeHigh = currentBar.high();
            extremeHighIndex = i;
          }
          if (currentBar.low().compareTo(extremeLow) < 0) {
            extremeLow = currentBar.low();
            extremeLowIndex = i;
          }
          if (extremeHigh.subtract(currentBar.close()).compareTo(reversalThreshold) >= 0) {
            swings.add(
                createSwingPoint(bars, atrSeries, extremeHighIndex, extremeHigh, SwingType.HIGH));
            activeWave = SwingType.LOW;
            extremeLow = currentBar.low();
            extremeLowIndex = i;
          } else if (currentBar.close().subtract(extremeLow).compareTo(reversalThreshold) >= 0) {
            swings.add(
                createSwingPoint(bars, atrSeries, extremeLowIndex, extremeLow, SwingType.LOW));
            activeWave = SwingType.HIGH;
            extremeHigh = currentBar.high();
            extremeHighIndex = i;
          }
        } else if (activeWave == SwingType.HIGH) {
          if (currentBar.high().compareTo(extremeHigh) > 0) {
            extremeHigh = currentBar.high();
            extremeHighIndex = i;
          }
          if (extremeHigh.subtract(currentBar.close()).compareTo(reversalThreshold) >= 0) {
            swings.add(
                createSwingPoint(bars, atrSeries, extremeHighIndex, extremeHigh, SwingType.HIGH));
            activeWave = SwingType.LOW;
            extremeLow = currentBar.low();
            extremeLowIndex = i;
          }
        } else {
          if (currentBar.low().compareTo(extremeLow) < 0) {
            extremeLow = currentBar.low();
            extremeLowIndex = i;
          }
          if (currentBar.close().subtract(extremeLow).compareTo(reversalThreshold) >= 0) {
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
