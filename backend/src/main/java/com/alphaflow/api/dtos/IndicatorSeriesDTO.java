package com.alphaflow.api.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Builder;

@Builder
public record IndicatorSeriesDto(
    @JsonProperty("type") String type,
    @JsonProperty("source") String source,
    @JsonProperty("params") String params,
    @JsonProperty("label") String label,
    @JsonProperty("points") List<IndicatorPointDto> points) {}
