package com.alphaflow.engine.calculators.indicators;


import com.alphaflow.persistence.enums.IndicatorType;

import com.alphaflow.persistence.enums.PriceSource;


import java.util.List;


/**

 * A single technical-indicator family (one bean per family, discovered via {@link IndicatorRegistry}).

 * <p>

 * Implementations are pure functions of (bars, prior state, params, source) — no I/O, no Spring

 * dependencies — so they are trivially unit-testable and the engine stays in control of bar loading

 * and persistence.

 *

 * <h4>Two modes, one method</h4>

 * <ul>

 *   <li><b>Backfill</b> — {@code priorStateJson == null}: {@code bars} is the full price history;

 *       the indicator self-seeds (standard seeding) and emits values from its first defined bar.</li>

 *   <li><b>Resume</b> — {@code priorStateJson != null}: recursive indicators treat {@code bars} as

 *       the continuation after the checkpoint and seed from the persisted state; windowed indicators

 *       ignore state and require the caller to include enough lookback bars in {@code bars}.</li>

 * </ul>

 * The contract guarantees resume reproduces a full backfill bit-for-bit over the overlapping dates.

 */

public interface Indicator {


  /**

   * The indicator family this bean implements; used as the registry key.

   */

  IndicatorType type();


  /**

   * Whether this indicator carries recursive running state across bars.

   * <p>

   * Recursive indicators (EMA, RSI, MACD) return {@code true} and resume from persisted internals.

   * Windowed indicators (SMA, Stochastic) return {@code false}: they hold no state and are resumed

   * by recomputing from the price bars the caller loads. The engine uses this to decide whether a

   * checkpoint with empty internals is resumable (windowed) or means "still warming up" (recursive).

   */

  default boolean requiresState() {

    return true;

  }


  /**

   * @param bars           chronologically ascending bars to compute over

   * @param priorStateJson persisted running state (see class doc), or {@code null} on backfill

   * @param params         parsed indicator parameters (periods)

   * @param source         which bar field single-series indicators read; ignored by multi-field ones

   */

  IndicatorResult compute(List<PriceBar> bars, String priorStateJson, IndicatorParams params, PriceSource source);

}

