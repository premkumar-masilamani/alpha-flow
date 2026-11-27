package com.alphaflow.engine.calculators.dtos;

import com.alphaflow.persistence.enums.ChartPatternStatus;
import java.time.LocalDate;

/**
 * Single-pass evaluation result capturing a chart pattern's lifecycle status and its breakout date
 * (when a breakout occurred prior to any invalidation).
 */
public record ChartPatternEvaluation(ChartPatternStatus status, LocalDate breakoutDate) {}
