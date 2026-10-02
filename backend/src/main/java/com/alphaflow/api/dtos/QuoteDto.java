package com.alphaflow.api.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import lombok.Builder;

@Builder
public record QuoteDto(
    @JsonProperty("currentPrice") BigDecimal currentPrice,
    @JsonProperty("timestamp") String timestamp) {}
