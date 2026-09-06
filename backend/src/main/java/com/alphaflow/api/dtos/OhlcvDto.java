package com.alphaflow.api.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Builder;

@Builder
public record OhlcvDto(
    @JsonProperty("date") LocalDate priceDate,
    @JsonProperty("open") BigDecimal priceOpen,
    @JsonProperty("high") BigDecimal priceHigh,
    @JsonProperty("low") BigDecimal priceLow,
    @JsonProperty("close") BigDecimal priceClose,
    @JsonProperty("vol") BigDecimal volume) {}
