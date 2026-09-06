package com.alphaflow.api.dtos;

import com.alphaflow.persistence.enums.IndicatorOutputKey;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import lombok.Builder;

@Builder
public record IndicatorPointDto(
    @JsonProperty("date") LocalDate date, @JsonProperty("values") Map<String, BigDecimal> values) {

  public BigDecimal getValue(IndicatorOutputKey key) {
    return values != null ? values.get(key.getValue()) : null;
  }
}
