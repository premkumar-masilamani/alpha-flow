package com.alphaflow.engine.calculators.dtos;

import com.alphaflow.persistence.entities.ChartPatternPivot;
import com.alphaflow.persistence.enums.ChartPatternStatus;
import com.alphaflow.persistence.enums.ChartPatternType;
import com.alphaflow.persistence.enums.PatternSentiment;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.Builder;

@Builder
public record ChartPatternMatch(
    ChartPatternType patternType,
    PatternSentiment sentiment,
    ChartPatternStatus status,
    LocalDate startDate,
    LocalDate endDate,
    LocalDate breakoutDate,
    BigDecimal necklineSlope,
    BigDecimal necklinePrice,
    BigDecimal targetPrice,
    BigDecimal invalidationPrice,
    List<ChartPatternPivot> pivotPoints) {}
