package com.prem.ta.dtos;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TradeDataDto(
        @JsonIgnore
        String symbol,

        @JsonProperty("date")
        LocalDate tradeDate,

        @JsonProperty("open")
        BigDecimal priceOpen,

        @JsonProperty("high")
        BigDecimal priceHigh,

        @JsonProperty("low")
        BigDecimal priceLow,

        @JsonProperty("close")
        BigDecimal priceClose,

        @JsonProperty("vol")
        BigDecimal volume,

        @JsonProperty("vwap")
        BigDecimal vwap,

        @JsonProperty("bvr")
        Double buyerVolumeRatio,

        @JsonProperty("bcr")
        Double buyerCapitalRatio
) {
}
