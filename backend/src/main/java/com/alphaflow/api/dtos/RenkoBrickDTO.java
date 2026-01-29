package com.alphaflow.api.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
public record RenkoBrickDTO(
        @JsonProperty("date") LocalDate date,
        @JsonProperty("low") BigDecimal low,
        @JsonProperty("high") BigDecimal high,
        @JsonProperty("direction") String direction,
        @JsonProperty("trend") int trend,
        @JsonProperty("zone") int zone
) {
}
