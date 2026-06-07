package com.alphaflow.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/** Concrete JPA entity mapped to the weekly_indicators database table. */
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

  /**
   * Constructs a WeeklyIndicator.
   *
   * @param weeklyIndicatorId the primary key ID
   * @param ticker the ticker
   * @param indicatorDefinition the definition
   * @param priceDate the price date
   * @param values the output values map
   */
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
