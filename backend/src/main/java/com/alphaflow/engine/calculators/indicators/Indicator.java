package com.alphaflow.engine.calculators.indicators;

import com.alphaflow.persistence.enums.IndicatorType;
import com.alphaflow.persistence.enums.PriceSource;
import java.util.List;

/**
 * A single technical-indicator family (one bean per family, discovered via {@link
 * IndicatorRegistry}).
 *
 * <p>Implementations are pure functions of (bars, params, source) — no I/O, no Spring dependencies
 * — so they are trivially unit-testable and the engine stays in control of bar loading and
 * persistence.
 */
public interface Indicator {
  /** The indicator family this bean implements; used as the registry key. */
  IndicatorType type();

  /**
   * @param bars chronologically ascending bars to compute over
   * @param params parsed indicator parameters (periods)
   * @param source which bar field single-series indicators read; ignored by multi-field ones
   * @return list of computed plot points
   */
  List<PlotPoint> compute(List<PriceBar> bars, IndicatorParams params, PriceSource source);
}
