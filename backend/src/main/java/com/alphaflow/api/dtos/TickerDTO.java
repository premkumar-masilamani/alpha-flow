package com.alphaflow.api.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TickerDTO(
        @JsonProperty("ticker_id") Long tickerId,
        @JsonProperty("symbol") String tickerSymbol,
        @JsonProperty("name") String tickerName,
        @JsonProperty("date") String tickerDate,
        @JsonProperty("is_active") boolean isActive
) {
}
