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
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "weekly_indicators_seq_gen")
  @SequenceGenerator(
      name = "weekly_indicators_seq_gen",
      sequenceName = "weekly_indicators_weekly_indicator_id_seq",
      allocationSize = 50)
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
