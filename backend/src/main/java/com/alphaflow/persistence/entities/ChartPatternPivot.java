package com.alphaflow.persistence.entities;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode
@ToString
public class ChartPatternPivot implements Serializable {

  private static final long serialVersionUID = 1L;

  private LocalDate date;
  private BigDecimal price;
  private String type;
  private String role;
}
