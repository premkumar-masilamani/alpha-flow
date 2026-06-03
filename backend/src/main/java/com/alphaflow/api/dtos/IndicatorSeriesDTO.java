package com.alphaflow.api.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Builder;

/** A single indicator's time series for a ticker on one timeframe, ready for charting. */
@Builder
public record IndicatorSeriesDTO(
    @JsonProperty("type") String type,
    @JsonProperty("source") String source,
    @JsonProperty("params") String params,
    @JsonProperty("label") String label,
    @JsonProperty("points") List<IndicatorPointDTO> points) {}
