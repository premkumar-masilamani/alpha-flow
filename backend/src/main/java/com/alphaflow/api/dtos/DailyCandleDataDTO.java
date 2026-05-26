package com.alphaflow.api.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
public record DailyCandleDataDTO(
        @JsonProperty("date") LocalDate candleDataDate,
        @JsonProperty("open") BigDecimal priceOpen,
        @JsonProperty("high") BigDecimal priceHigh,
        @JsonProperty("low") BigDecimal priceLow,
        @JsonProperty("close") BigDecimal priceClose,
        @JsonProperty("vol") BigDecimal volume
) {
}
