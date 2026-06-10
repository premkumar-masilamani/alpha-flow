package com.alphaflow.engine.strategies.evaluators;

import com.alphaflow.api.dtos.IndicatorSeriesDTO;
import com.alphaflow.api.dtos.OhlcvDTO;
import java.util.List;

public record ASTAEvaluationContext(
    List<OhlcvDTO> dailyCandles,
    List<IndicatorSeriesDTO> dailyIndicators,
    List<IndicatorSeriesDTO> weeklyIndicators) {}
