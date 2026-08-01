package com.alphaflow.api.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record IndicatorConfigDto(
    @JsonProperty("timeframe") String timeframe,
    @JsonProperty("type") String type,
    @JsonProperty("source") String source,
    @JsonProperty("params") String params,
    @JsonProperty("label") String label) {}
