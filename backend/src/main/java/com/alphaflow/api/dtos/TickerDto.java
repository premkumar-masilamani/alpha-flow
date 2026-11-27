package com.alphaflow.api.dtos;

import com.alphaflow.persistence.enums.Country;
import com.alphaflow.persistence.enums.TickerType;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record TickerDto(
    @JsonProperty("id") Long tickerId,
    @JsonProperty("symbol") String tickerSymbol,
    @JsonProperty("name") String tickerName,
    @JsonProperty("type") TickerType tickerType,
    @JsonProperty("country") Country country,
    @JsonProperty("isStale") boolean isStale) {}
