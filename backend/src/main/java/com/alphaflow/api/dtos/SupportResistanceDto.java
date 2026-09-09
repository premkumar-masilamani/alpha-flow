package com.alphaflow.api.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SupportResistanceDto {
  private LocalDate priceDate;
  private LocalDate firstTouchDate;
  private LocalDate lastTouchDate;
  private BigDecimal zoneBottom;
  private BigDecimal zoneTop;
  private BigDecimal zoneMidpoint;
  private String levelType;
  private Integer touchCount;
}
