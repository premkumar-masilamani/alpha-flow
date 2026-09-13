package com.alphaflow.api.dtos;

import com.alphaflow.persistence.enums.ChartPatternStatus;
import com.alphaflow.persistence.enums.ChartPatternType;
import com.alphaflow.persistence.enums.PatternSentiment;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.Builder;

@Builder
public record ChartPatternDto(
    @JsonProperty("id") Long id,
    @JsonProperty("patternType") ChartPatternType patternType,
    @JsonProperty("shortName") String shortName,
    @JsonProperty("displayName") String displayName,
    @JsonProperty("sentiment") PatternSentiment sentiment,
    @JsonProperty("status") ChartPatternStatus status,
    @JsonProperty("startDate") LocalDate startDate,
    @JsonProperty("endDate") LocalDate endDate,
    @JsonProperty("breakoutDate") LocalDate breakoutDate,
    @JsonProperty("necklineSlope") BigDecimal necklineSlope,
    @JsonProperty("necklinePrice") BigDecimal necklinePrice,
    @JsonProperty("targetPrice") BigDecimal targetPrice,
    @JsonProperty("stopLossPrice") BigDecimal stopLossPrice,
    @JsonProperty("invalidationPrice") BigDecimal invalidationPrice,
    @JsonProperty("pivotPoints") List<ChartPatternPivotDto> pivotPoints) {}
