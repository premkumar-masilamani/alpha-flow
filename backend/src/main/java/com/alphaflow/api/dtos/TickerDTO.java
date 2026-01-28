package com.alphaflow.api.dtos;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public record TickerDTO(
        @JsonProperty("id") Long tickerId,
        @JsonProperty("symbol") String tickerSymbol,
        @JsonProperty("name") String tickerName,
        @JsonIgnore String tickerDate,
        @JsonIgnore boolean isActive
) {
}
