package com.alphaflow.persistence.entities;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import lombok.*;

@Entity
@Table(name = "weekly_indicators")
@Getter
@Setter
@NoArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class WeeklyIndicator extends Indicator {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "weekly_indicator_id")
  private Long weeklyIndicatorId;

  @Builder
  public WeeklyIndicator(
      Long weeklyIndicatorId,
      Ticker ticker,
      IndicatorDefinition indicatorDefinition,
      LocalDate priceDate,
      Map<String, BigDecimal> values) {
    super(ticker, indicatorDefinition, priceDate, values);
    this.weeklyIndicatorId = weeklyIndicatorId;
  }
}
