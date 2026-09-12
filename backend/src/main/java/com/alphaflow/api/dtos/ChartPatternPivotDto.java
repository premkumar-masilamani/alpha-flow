package com.alphaflow.api.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Builder;

@Builder
public record ChartPatternPivotDto(
    @JsonProperty("date") LocalDate date,
    @JsonProperty("price") BigDecimal price,
    @JsonProperty("type") String type,
    @JsonProperty("role") String role) {}
