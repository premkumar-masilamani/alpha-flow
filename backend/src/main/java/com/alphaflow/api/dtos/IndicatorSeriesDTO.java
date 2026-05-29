package com.alphaflow.api.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;

/**
 * A single indicator's time series for a ticker on one timeframe, ready for charting.
 */
@Builder
public record IndicatorSeriesDTO(
        @JsonProperty("type") String type,
        @JsonProperty("source") String source,
        @JsonProperty("params") String params,
        @JsonProperty("label") String label,
        @JsonProperty("points") List<IndicatorPointDTO> points
) {
}
