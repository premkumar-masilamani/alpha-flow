package com.alphaflow.api.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SRTouchPointDTO {
  private LocalDate date;
  private BigDecimal price;
}
