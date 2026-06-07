package com.alphaflow.engine.calculators.indicators;

import com.alphaflow.persistence.enums.PriceSource;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Timeframe-agnostic view of a single OHLCV bar fed to the indicator engine.
 *
 * <p>Indicators read whichever field they need: single-series indicators (EMA/SMA/RSI/MACD) use
 *
 * <p>{@link #valueFor(PriceSource)} to select their configured source; multi-field indicators
 *
 * <p>(Stochastic) read {@link #high()}/{@link #low()}/{@link #close()} directly. The same bar shape
 *
 * <p>is produced from both {@code daily_prices} and {@code weekly_prices}, so indicators are blind
 * to
 *
 * <p>the timeframe they run on.
 */
public record PriceBar(
    LocalDate date,
    BigDecimal open,
    BigDecimal high,
    BigDecimal low,
    BigDecimal close,
    BigDecimal volume) {
  /** Returns the field this indicator's input series should be drawn from. */
  public BigDecimal valueFor(PriceSource source) {
    return switch (source) {
      case OPEN -> open;
      case HIGH -> high;
      case LOW -> low;
      case CLOSE -> close;
      case VOLUME -> volume;
    };
  }
}
