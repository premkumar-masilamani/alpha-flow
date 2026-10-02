package com.alphaflow.api.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.math.BigDecimal;
import java.time.temporal.TemporalAccessor;
import lombok.Builder;

@Builder
public record OhlcvDto(
    @JsonProperty("date")
        @JsonSerialize(using = TemporalAccessorSerializer.class)
        @JsonDeserialize(using = TemporalAccessorDeserializer.class)
        TemporalAccessor priceDate,
    @JsonProperty("open") BigDecimal priceOpen,
    @JsonProperty("high") BigDecimal priceHigh,
    @JsonProperty("low") BigDecimal priceLow,
    @JsonProperty("close") BigDecimal priceClose,
    @JsonProperty("vol") BigDecimal volume) {}
