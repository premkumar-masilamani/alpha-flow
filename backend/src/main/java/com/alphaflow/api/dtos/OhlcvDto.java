package com.alphaflow.api.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Builder;

@Builder
public record OhlcvDto(
    @JsonProperty("date") String priceDate,
    @JsonProperty("open") BigDecimal priceOpen,
    @JsonProperty("high") BigDecimal priceHigh,
    @JsonProperty("low") BigDecimal priceLow,
    @JsonProperty("close") BigDecimal priceClose,
    @JsonProperty("vol") BigDecimal volume) {

  public OhlcvDto(
      LocalDate date,
      BigDecimal priceOpen,
      BigDecimal priceHigh,
      BigDecimal priceLow,
      BigDecimal priceClose,
      BigDecimal volume) {
    this(
        (date != null) ? date.toString() : null,
        priceOpen,
        priceHigh,
        priceLow,
        priceClose,
        volume);
  }

  public static class OhlcvDtoBuilder {
    public OhlcvDtoBuilder priceDate(LocalDate date) {
      this.priceDate = (date != null) ? date.toString() : null;
      return this;
    }

    public OhlcvDtoBuilder priceDate(String date) {
      this.priceDate = date;
      return this;
    }
  }
}
