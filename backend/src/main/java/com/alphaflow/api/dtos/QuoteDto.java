package com.alphaflow.api.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import lombok.Builder;

@Builder
public record QuoteDto(
    @JsonProperty("symbol") String symbol,
    @JsonProperty("name") String name,
    @JsonProperty("lastPrice") BigDecimal lastPrice,
    @JsonProperty("change") BigDecimal change,
    @JsonProperty("changePercent") BigDecimal changePercent,
    @JsonProperty("open") BigDecimal open,
    @JsonProperty("high") BigDecimal high,
    @JsonProperty("low") BigDecimal low,
    @JsonProperty("close") BigDecimal close,
    @JsonProperty("volume") BigDecimal volume,
    @JsonProperty("timestamp") String timestamp) {}
