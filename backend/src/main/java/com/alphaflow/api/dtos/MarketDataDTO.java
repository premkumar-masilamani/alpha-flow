package com.alphaflow.api.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
public record MarketDataDTO(
        @JsonProperty("date") LocalDate marketDataDate,
        @JsonProperty("open") BigDecimal priceOpen,
        @JsonProperty("high") BigDecimal priceHigh,
        @JsonProperty("low") BigDecimal priceLow,
        @JsonProperty("close") BigDecimal priceClose,
        @JsonProperty("vol") BigDecimal volume,
        @JsonProperty("vwap") BigDecimal vwap,
        @JsonProperty("capital_poc") BigDecimal capitalPOC,
        @JsonProperty("capital_vah") BigDecimal capitalVAH,
        @JsonProperty("capital_val") BigDecimal capitalVAL,
        @JsonProperty("buyer_capital") BigDecimal buyerCapital,
        @JsonProperty("total_capital") BigDecimal totalCapital
) {
}
