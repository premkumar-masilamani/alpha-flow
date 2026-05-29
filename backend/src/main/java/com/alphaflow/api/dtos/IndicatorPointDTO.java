package com.alphaflow.api.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * One indicator reading for one bar. {@code values} is keyed by output name so multi-plot indicators
 * carry all their plots together (e.g. MACD -> {@code {"macd":..,"signal":..,"histogram":..}}).
 */
@Builder
public record IndicatorPointDTO(
        @JsonProperty("date") LocalDate date,
        @JsonProperty("values") Map<String, BigDecimal> values
) {
}
