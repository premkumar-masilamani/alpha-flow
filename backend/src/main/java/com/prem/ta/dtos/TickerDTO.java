package com.prem.ta.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TickerDTO(
        @JsonProperty("ticker_id")
        Long tickerId,

        @JsonProperty("symbol")
        String symbol,

        @JsonProperty("name")
        String name,

        @JsonProperty("start_date")
        String startDate
) {
}
