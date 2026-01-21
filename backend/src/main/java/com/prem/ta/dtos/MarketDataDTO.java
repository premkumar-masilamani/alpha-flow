package com.prem.ta.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MarketDataDTO(

        @JsonProperty("date") LocalDate marketDataDate,

        @JsonProperty("open") BigDecimal priceOpen,

        @JsonProperty("high") BigDecimal priceHigh,

        @JsonProperty("low") BigDecimal priceLow,

        @JsonProperty("close") BigDecimal priceClose,

        @JsonProperty("vol") BigDecimal volume,

        @JsonProperty("vwap") BigDecimal vwap,

        @JsonProperty("poc") BigDecimal volumeProfilePOC,

        @JsonProperty("vah") BigDecimal volumeProfileVAH,

        @JsonProperty("val") BigDecimal volumeProfileVAL,

        @JsonProperty("vpd") BigDecimal vwapPocDeviationPct,

        @JsonProperty("bvs") BigDecimal buyerVolumeShare,

        @JsonProperty("bcs") BigDecimal buyerCapitalShare) {
}
