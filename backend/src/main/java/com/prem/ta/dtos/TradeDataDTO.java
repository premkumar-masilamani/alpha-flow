package com.prem.ta.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TradeDataDTO(

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
        BigDecimal volumeWeightedAveragePrice,

        @JsonProperty("poc")
        BigDecimal volumeProfilePointOfControl,

        @JsonProperty("vah")
        BigDecimal volumeProfileValueAreaHigh,

        @JsonProperty("val")
        BigDecimal volumeProfileValueAreaLow,

        @JsonProperty("bvs")
        Double buyerVolumeShare,

        @JsonProperty("bcs")
        Double buyerCapitalShare
) {
}
