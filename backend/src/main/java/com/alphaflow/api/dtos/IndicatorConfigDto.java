package com.alphaflow.api.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
public record IndicatorConfigDto(
    @JsonProperty("timeframe") String timeframe,
    @JsonProperty("type") String type,
    @JsonProperty("source") String source,
    @JsonProperty("params") String params,
    @JsonProperty("label") String label) {}
