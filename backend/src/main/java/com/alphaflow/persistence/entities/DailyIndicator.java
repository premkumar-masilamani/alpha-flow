package com.alphaflow.persistence.entities;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import lombok.*;

@Entity
@Table(name = "daily_indicators")
@Getter
@Setter
@NoArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class DailyIndicator extends Indicator {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "daily_indicators_seq_gen")
  @SequenceGenerator(
      name = "daily_indicators_seq_gen",
      sequenceName = "daily_indicators_daily_indicator_id_seq",
      allocationSize = 50)
  @Column(name = "daily_indicator_id")
  private Long dailyIndicatorId;

  @Builder
  public DailyIndicator(
      Long dailyIndicatorId,
      Ticker ticker,
      IndicatorDefinition indicatorDefinition,
      LocalDate priceDate,
      Map<String, BigDecimal> values) {
    super(ticker, indicatorDefinition, priceDate, values);
    this.dailyIndicatorId = dailyIndicatorId;
  }
}
