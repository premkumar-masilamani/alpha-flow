package com.alphaflow.engine.indicators.dtos;

import com.alphaflow.persistence.entities.DailyPrice;
import com.alphaflow.persistence.entities.WeeklyPrice;
import com.alphaflow.persistence.enums.PriceSource;
import java.math.BigDecimal;
import java.time.LocalDate;

public record PriceBar(
    LocalDate date,
    BigDecimal open,
    BigDecimal high,
    BigDecimal low,
    BigDecimal close,
    BigDecimal volume) {

  public static PriceBar from(DailyPrice dailyPrice) {
    return new PriceBar(
        dailyPrice.getPriceDate(),
        dailyPrice.getPriceOpen(),
        dailyPrice.getPriceHigh(),
        dailyPrice.getPriceLow(),
        dailyPrice.getPriceClose(),
        dailyPrice.getVolume());
  }

  public static PriceBar from(WeeklyPrice weeklyPrice) {
    return new PriceBar(
        weeklyPrice.getPriceDate(),
        weeklyPrice.getPriceOpen(),
        weeklyPrice.getPriceHigh(),
        weeklyPrice.getPriceLow(),
        weeklyPrice.getPriceClose(),
        weeklyPrice.getVolume());
  }

  public BigDecimal valueFor(PriceSource source) {
    return switch (source) {
      case CLOSE -> close;
      case VOLUME -> volume;
    };
  }
}
