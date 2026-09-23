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
    @JsonProperty("country") Country country) {

  public TickerDto(Long tickerId, String tickerSymbol, String tickerName) {
    this(tickerId, tickerSymbol, tickerName, TickerType.STOCK, Country.US);
  }

  public TickerDto(Long tickerId, String tickerSymbol, String tickerName, TickerType tickerType) {
    this(tickerId, tickerSymbol, tickerName, tickerType, Country.US);
  }
}
