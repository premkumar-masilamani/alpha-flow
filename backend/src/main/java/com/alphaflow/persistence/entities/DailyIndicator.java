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

/** Concrete JPA entity mapped to the daily_indicators database table. */
@Entity
@Table(name = "daily_indicators")
@Getter
@Setter
@NoArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class DailyIndicator extends Indicator {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "daily_indicator_id")
  private Long dailyIndicatorId;

  /**
   * Constructs a DailyIndicator.
   *
   * @param dailyIndicatorId the primary key ID
   * @param ticker the ticker
   * @param indicatorDefinition the definition
   * @param priceDate the price date
   * @param values the output values map
   */
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
