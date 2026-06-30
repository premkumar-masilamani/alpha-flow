package com.alphaflow.persistence.entities;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SRTouchPoint {
  private LocalDate date;
  private BigDecimal price;
}
