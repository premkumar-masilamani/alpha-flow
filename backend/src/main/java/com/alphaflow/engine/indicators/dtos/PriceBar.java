package com.alphaflow.engine.indicators.dtos;

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

  public BigDecimal valueFor(PriceSource source) {
    return switch (source) {
      case CLOSE -> close;
      case VOLUME -> volume;
    };
  }
}
