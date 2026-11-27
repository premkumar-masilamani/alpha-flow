package com.alphaflow.engine.structure.enums;

/** Dow Theory directional market structure classification derived from swing highs and lows. */
public enum DowTrend {
  /** Sequence of Higher Highs (HH) and Higher Lows (HL). */
  UPTREND,
  /** Sequence of Lower Highs (LH) and Lower Lows (LL). */
  DOWNTREND,
  /** Mixed or insufficient swing sequence without a confirmed directional bias. */
  SIDEWAYS
}
