package com.alphaflow.engine.strategies.evaluators;

import com.alphaflow.persistence.entities.DailyIndicator;
import com.alphaflow.persistence.entities.DailyPrice;
import com.alphaflow.persistence.entities.WeeklyIndicator;
import java.util.List;

public record ASTAEvaluationContext(
    List<DailyPrice> dailyCandles,
    List<DailyIndicator> dailyIndicators,
    List<WeeklyIndicator> weeklyIndicators) {}
