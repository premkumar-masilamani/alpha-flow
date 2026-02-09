package com.alphaflow.api.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record TickerDTO(
        @JsonProperty("id") Long tickerId,
        @JsonProperty("symbol") String tickerSymbol,
        @JsonProperty("name") String tickerName
) {
}
