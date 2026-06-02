package com.alphaflow.api.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

/**
 * One configured indicator combo, returned by the discovery endpoint so the UI can build its
 * controls from configuration rather than a hardcoded list.
 */
@Builder
public record IndicatorConfigDTO(
    @JsonProperty("timeframe") String timeframe,
    @JsonProperty("type") String type,
    @JsonProperty("source") String source,
    @JsonProperty("params") String params,
    @JsonProperty("label") String label,
    @JsonProperty("upperBound") Integer upperBound,
    @JsonProperty("lowerBound") Integer lowerBound
) {}
